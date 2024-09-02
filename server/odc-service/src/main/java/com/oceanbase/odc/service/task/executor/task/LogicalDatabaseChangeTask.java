/*
 * Copyright (c) 2024 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oceanbase.odc.service.task.executor.task;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.sql.execute.model.SqlTuple;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.core.sql.split.SqlStatementIterator;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutorEngine;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionCallback;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionGroupContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.model.ExecutionUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.MySQLExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.OBExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecuteReq;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionCallback;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.parser.LogicalTableExpressionParseUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RelationFactorRewriter;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.SqlRewriter;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.flow.task.model.SizeAwareInputStream;
import com.oceanbase.odc.service.objectstorage.util.ObjectStorageUtils;
import com.oceanbase.odc.service.schedule.model.LogicalDatabaseChangeParameters;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor;
import com.oceanbase.odc.service.session.util.DBSchemaExtractor.DBSchemaIdentity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.dbbrowser.parser.constant.SqlType;
import com.oceanbase.tools.loaddump.utils.CollectionUtils;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 14:33
 * @Description: []
 */
@Slf4j
public class LogicalDatabaseChangeTask extends BaseTask<Map<String, ExecutionResult<SqlExecuteResult>>> {
    private SqlRewriter sqlRewriter;
    private ExecutionGroupContext<SqlExecuteReq, SqlExecuteResult> executionGroupContext;
    private LogicalDatabaseChangeParameters taskParameters;
    private List<ExecutionGroup<SqlExecuteReq, SqlExecuteResult>> executionGroups;
    private List<ConnectionSession> connectionSessions = new ArrayList<>();
    private ExecutorEngine executorEngine;

    @Override
    protected void doInit(JobContext context) throws Exception {
        Map<String, String> jobParameters = context.getJobParameters();
        taskParameters = JsonUtils.fromJson(jobParameters.get(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY),
                LogicalDatabaseChangeParameters.class);
        sqlRewriter = new RelationFactorRewriter();
    }

    @Override
    protected boolean doStart(JobContext context) throws Exception {
        // 分句
        List<String> sqls =
                SqlUtils.split(taskParameters.getConnectType().getDialectType(), taskParameters.getSqlContent(),
                        StringUtils.isEmpty(taskParameters.getDelimiter()) ? ";" : taskParameters.getDelimiter());
        List<RewriteResult> rewrittenResults = new ArrayList<>();
        for (String sql : sqls) {
            Statement statement = SqlParser.parseMysqlStatement(sql);
            Map<DBSchemaIdentity, Set<SqlType>> identity2SqlTypes = DBSchemaExtractor.listDBSchemasWithSqlTypes(
                    Arrays.asList(SqlTuple.newTuple(sql)), taskParameters.getConnectType().getDialectType(), null);
            DBSchemaIdentity identity = identity2SqlTypes.keySet().iterator().next();
            String logicalTableExpression = "";
            if (StringUtils.isNotEmpty(identity.getSchema())) {
                logicalTableExpression = identity.getSchema() + ".";
            }
            logicalTableExpression += identity.getTable();
            Set<DataNode> dataNodes = LogicalTableExpressionParseUtils.resolve(logicalTableExpression).stream().collect(
                    Collectors.toSet());
            rewrittenResults.add(sqlRewriter.rewrite(
                    new RewriteContext(statement, taskParameters.getConnectType().getDialectType(), dataNodes)));
        }
        for (RewriteResult rewrittenResult : rewrittenResults) {
            Set<DataNode> allDataNodes = taskParameters.getAllDataNodes();
            List<ExecutionUnit<SqlExecuteReq, SqlExecuteResult>> executionUnits = new ArrayList<>();
            Map<DataNode, String> dataNode2Sql = rewrittenResult.getSqls();
            for (DataNode dataNode : allDataNodes) {
                String sql = dataNode2Sql.getOrDefault(dataNode, "");
                SqlExecuteReq req = new SqlExecuteReq();
                req.setSql(sql);
                req.setDialectType(taskParameters.getConnectType().getDialectType());
                req.setConnectionConfig(dataNode.getDataSourceConfig());
                ConnectionSession connectionSession = generateSession(dataNode.getDataSourceConfig());
                connectionSessions.add(connectionSession);
                ExecutionCallback<SqlExecuteReq, SqlExecuteResult> callback =
                        new SqlExecutionCallback(connectionSession, sql, taskParameters.getTimeoutMillis());
                executionUnits.add(new ExecutionUnit<>(StringUtils.uuid(), callback, req));
            }
            executionGroups.add(taskParameters.getConnectType().getDialectType() == DialectType.MYSQL
                    ? new MySQLExecutionGroup(executionUnits)
                    : new OBExecutionGroup(executionUnits));
        }
        executorEngine = new ExecutorEngine<SqlExecuteReq, SqlExecuteResult>(100);
        this.executionGroupContext = executorEngine.execute(executionGroups);
        while (!Thread.currentThread().isInterrupted()) {
            if (this.executionGroupContext.isCompleted()) {
                return true;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
        throw new InterruptedException("logical database change task has been interrupted");
    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    protected void doClose() throws Exception {
        if (CollectionUtils.isNotEmpty(this.connectionSessions)) {
            this.connectionSessions.stream().forEach(this::tryExpireConnectionSession);
        }
        if (this.executorEngine != null) {
            this.executorEngine.close();
        }
    }

    @Override
    public double getProgress() {
        return this.executionGroupContext.getCompletedGroupCount() / this.executionGroups.size();
    }

    @Override
    public Map<String, ExecutionResult<SqlExecuteResult>> getTaskResult() {
        return this.executionGroupContext.getResults();
    }

    private ConnectionSession generateSession(@NonNull ConnectionConfig connectionConfig) {
        DefaultConnectSessionFactory sessionFactory = new DefaultConnectSessionFactory(connectionConfig);
        sessionFactory.setSessionTimeoutMillis(this.taskParameters.getTimeoutMillis());
        ConnectionSession connectionSession = sessionFactory.generateSession();
        SqlCommentProcessor processor = new SqlCommentProcessor(connectionConfig.getDialectType(), true,
                true);
        ConnectionSessionUtil.setSqlCommentProcessor(connectionSession, processor);
        ConnectionSessionUtil.setColumnAccessor(connectionSession, new DatasourceColumnAccessor(connectionSession));
        return connectionSession;
    }

    private void tryExpireConnectionSession(ConnectionSession connectionSession) {
        if (connectionSession != null && !connectionSession.isExpired()) {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }
    }
}
