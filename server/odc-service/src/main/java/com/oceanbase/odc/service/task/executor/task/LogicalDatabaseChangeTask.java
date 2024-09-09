/*
 * Copyright (c) 2023 OceanBase.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.MapUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseUtils;
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
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RelationFactorRewriter;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.SqlRewriter;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.accessor.DatasourceColumnAccessor;
import com.oceanbase.odc.service.schedule.model.PublishLogicalDatabaseChangeReq;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.loaddump.utils.CollectionUtils;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 14:33
 * @Description: []
 */
@Slf4j
public class LogicalDatabaseChangeTask extends BaseTask<Map<String, ExecutionResult<SqlExecutionResultWrapper>>> {
    private SqlRewriter sqlRewriter;
    private ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> executionGroupContext;
    private PublishLogicalDatabaseChangeReq taskParameters;
    private List<ExecutionGroup<SqlExecuteReq, SqlExecutionResultWrapper>> executionGroups;
    private List<ConnectionSession> connectionSessions;
    private ExecutorEngine executorEngine;

    @Override
    protected void doInit(JobContext context) throws Exception {
        Map<String, String> jobParameters = context.getJobParameters();
        taskParameters = JsonUtils.fromJson(jobParameters.get(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY),
                PublishLogicalDatabaseChangeReq.class);
        sqlRewriter = new RelationFactorRewriter();
        executionGroups = new ArrayList<>();
        connectionSessions = new ArrayList<>();
    }

    @Override
    protected boolean doStart(JobContext context) throws Exception {
        try {
            DialectType dialectType = taskParameters.getLogicalDatabaseResp().getDialectType();
            List<String> sqls =
                    SqlUtils.split(dialectType, taskParameters.getSqlContent(),
                            StringUtils.isEmpty(taskParameters.getDelimiter()) ? ";" : taskParameters.getDelimiter());
            Set<DataNode> allDataNodes = taskParameters.getLogicalDatabaseResp().getPhysicalDatabases().stream()
                    .map(database -> new DataNode(database.getDataSource(), database.getId(), database.getName()))
                    .collect(Collectors.toSet());
            Map<Long, DataNode> databaseId2DataNodes = allDataNodes.stream()
                    .collect(Collectors.toMap(dataNode -> dataNode.getDatabaseId(), dataNode -> dataNode,
                            (value1, value2) -> value1));

            long order = 0;
            for (String sql : sqls) {
                Statement statement = SqlParser.parseMysqlStatement(sql);
                Set<DataNode> dataNodesToExecute;
                if (statement instanceof CreateTable) {
                    dataNodesToExecute =
                            LogicalDatabaseUtils.getDataNodesFromCreateTable(sql, dialectType, allDataNodes);
                } else {
                    dataNodesToExecute = LogicalDatabaseUtils.getDataNodesFromNotCreateTable(sql, dialectType,
                            taskParameters.getLogicalDatabaseResp());
                }
                RewriteResult rewriteResult = sqlRewriter.rewrite(
                        new RewriteContext(statement, dialectType, dataNodesToExecute));
                Map<Long, List<String>> databaseId2RewrittenSqls = rewriteResult.getSqls().entrySet().stream()
                        .collect(Collectors.groupingBy(
                                entry -> entry.getKey().getDatabaseId(),
                                Collectors.mapping(entry -> entry.getValue(), Collectors.toList())));
                if (MapUtils.isEmpty(databaseId2RewrittenSqls)) {
                    log.warn("cannot recognize the sql, sql = {}", sql);
                    continue;
                }
                List<ExecutionUnit<SqlExecuteReq, SqlExecutionResultWrapper>> executionUnits = new ArrayList<>();
                for (Entry<Long, DataNode> entry : databaseId2DataNodes.entrySet()) {
                    Long databaseId = entry.getKey();
                    DataNode dataNode = entry.getValue();
                    String rewrittenSqls =
                            databaseId2RewrittenSqls.get(databaseId).stream().collect(Collectors.joining(";"));
                    SqlExecuteReq req = new SqlExecuteReq();
                    req.setSql(rewrittenSqls);
                    req.setDialectType(dialectType);
                    req.setConnectionConfig(dataNode.getDataSourceConfig());
                    ConnectionSession connectionSession = generateSession(dataNode.getDataSourceConfig());
                    connectionSessions.add(connectionSession);
                    ExecutionCallback<SqlExecuteReq, SqlExecutionResultWrapper> callback =
                            new SqlExecutionCallback(connectionSession,
                                    new SqlExecuteReq(rewrittenSqls, order, taskParameters.getTimeoutMillis(),
                                            dialectType,
                                            dataNode.getDataSourceConfig(),
                                            taskParameters.getLogicalDatabaseResp().getId(),
                                            dataNode.getDatabaseId(), taskParameters.getScheduleTaskId()));
                    executionUnits.add(new ExecutionUnit<>(StringUtils.uuid(), order, callback, req));
                }
                order++;
                executionGroups.add(dialectType == DialectType.MYSQL ? new MySQLExecutionGroup(executionUnits)
                        : new OBExecutionGroup(executionUnits));
            }
            log.info("start logical database change task, generatedSql = {}", executionGroups.stream().map(
                    group -> group.getExecutionUnits().stream().map(unit -> unit.getInput().getSql()).collect(
                            Collectors.joining(taskParameters.getDelimiter())))
                    .collect(Collectors.joining(taskParameters.getDelimiter())));
            executorEngine = new ExecutorEngine<SqlExecuteReq, SqlExecuteResult>(100);
            this.executionGroupContext = executorEngine.execute(executionGroups);
        } catch (Exception ex) {
            log.warn("start logical database change task failed, ", ex);
            return false;
        }
        while (!Thread.currentThread().isInterrupted()) {
            if (this.executionGroupContext.isCompleted()) {
                log.info("logical database change task is completed");
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
        this.executionGroupContext.terminateAll();
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
    public Map<String, ExecutionResult<SqlExecutionResultWrapper>> getTaskResult() {
        return this.executionGroupContext.getResults();
    }

    @Override
    protected void afterModifiedJobParameters() throws Exception {
        Map<String, String> currentJobParameters = getJobParameters();
        if (currentJobParameters == null || currentJobParameters.isEmpty()) {
            return;
        }
        if (currentJobParameters.containsKey(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_SKIP_UNIT)) {
            String executionUnitId =
                    currentJobParameters.get(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_SKIP_UNIT);
            if (StringUtils.isNotEmpty(executionUnitId)) {
                this.executionGroupContext.skip(executionUnitId);
            }
        }
        if (currentJobParameters.containsKey(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_TERMINATE_UNIT)) {
            String executionUnitId =
                    currentJobParameters.get(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_SKIP_UNIT);
            if (StringUtils.isNotEmpty(executionUnitId)) {
                this.executionGroupContext.terminate(executionUnitId);
            }
        }
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
