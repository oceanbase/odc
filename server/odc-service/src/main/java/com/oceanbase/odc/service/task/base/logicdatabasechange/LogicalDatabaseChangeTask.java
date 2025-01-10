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
package com.oceanbase.odc.service.task.base.logicdatabasechange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.MapUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.LogicalDatabaseUtils;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionGroupContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionHandler;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.ExecutionSubGroupUnit;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.execution.GroupExecutionEngine;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.MySQLExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.OBExecutionGroup;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecuteReq;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionHandler;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RelationFactorRewriter;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteContext;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.RewriteResult;
import com.oceanbase.odc.service.connection.logicaldatabase.core.rewrite.SqlRewriter;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalDatabaseResp;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DetailLogicalTableResp;
import com.oceanbase.odc.service.schedule.model.PublishLogicalDatabaseChangeReq;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.task.base.TaskBase;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.tools.dbbrowser.parser.SqlParser;
import com.oceanbase.tools.loaddump.utils.CollectionUtils;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 14:33
 * @Description: []
 */
@Slf4j
public class LogicalDatabaseChangeTask extends TaskBase<Map<String, ExecutionResult<SqlExecutionResultWrapper>>> {
    private SqlRewriter sqlRewriter;
    private ExecutionGroupContext<SqlExecuteReq, SqlExecutionResultWrapper> executionGroupContext;
    private PublishLogicalDatabaseChangeReq taskParameters;
    private List<ExecutionGroup<SqlExecuteReq, SqlExecutionResultWrapper>> executionGroups;
    private GroupExecutionEngine executorEngine;

    public LogicalDatabaseChangeTask() {}

    @Override
    protected void doInit(JobContext context) throws Exception {
        Map<String, String> jobParameters = context.getJobParameters();
        taskParameters = JsonUtils.fromJson(jobParameters.get(JobParametersKeyConstants.TASK_PARAMETER_JSON_KEY),
                PublishLogicalDatabaseChangeReq.class);
        sqlRewriter = new RelationFactorRewriter();
        executionGroups = new ArrayList<>();
    }

    @Override
    public boolean start() throws Exception {
        try {
            DialectType dialectType = taskParameters.getLogicalDatabaseResp().getDialectType();
            DetailLogicalDatabaseResp detailLogicalDatabaseResp = taskParameters.getLogicalDatabaseResp();
            Set<DataNode> physicalDatabases = taskParameters.getLogicalDatabaseResp().getPhysicalDatabases().stream()
                    .map(database -> new DataNode(database.getDataSource(), database.getId(), database.getName()))
                    .collect(Collectors.toSet());
            Map<String, DataNode> databaseName2DataNodes = physicalDatabases.stream()
                    .collect(Collectors.toMap(dataNode -> dataNode.getSchemaName(), dataNode -> dataNode,
                            (value1, value2) -> value1));
            Map<String, Set<DataNode>> logicalTableName2DataNodes =
                    detailLogicalDatabaseResp.getLogicalTables().stream()
                            .collect(Collectors.toMap(DetailLogicalTableResp::getName,
                                    resp -> resp.getAllPhysicalTables().stream().collect(Collectors.toSet())));
            Map<Long, DataNode> databaseId2DataNodes = physicalDatabases.stream()
                    .collect(Collectors.toMap(dataNode -> dataNode.getDatabaseId(), dataNode -> dataNode,
                            (value1, value2) -> value1));
            long order = 0;
            List<String> sqls =
                    SqlUtils.split(dialectType, taskParameters.getSqlContent(),
                            StringUtils.isEmpty(taskParameters.getDelimiter()) ? ";" : taskParameters.getDelimiter());
            for (String sql : sqls) {
                Statement statement = SqlParser.parseMysqlStatement(sql);
                Set<DataNode> dataNodesToExecute;
                if (statement instanceof CreateTable) {
                    dataNodesToExecute =
                            LogicalDatabaseUtils.getDataNodesFromCreateTable(sql, dialectType, databaseName2DataNodes);
                } else {
                    dataNodesToExecute = LogicalDatabaseUtils.getDataNodesFromNotCreateTable(sql, dialectType,
                            logicalTableName2DataNodes, detailLogicalDatabaseResp.getName());
                }
                if (CollectionUtils.isEmpty(dataNodesToExecute)) {
                    log.warn("There is no physical database to operate on, sql={}", sql);
                    continue;
                }
                RewriteResult rewriteResult = sqlRewriter.rewrite(
                        new RewriteContext(statement, dialectType, dataNodesToExecute));
                if (rewriteResult == null || MapUtils.isEmpty(rewriteResult.getSqls())) {
                    log.warn("Cannot rewrite the sql, sql={}", sql);
                    continue;
                }
                rewriteResult.getSqls().entrySet().stream().forEach(entry -> {
                    if (entry.getKey().getDatabaseId() == null) {
                        throw new BadRequestException(
                                "physical database not found, database name=" + entry.getKey().getSchemaName());
                    }
                });
                Map<Long, List<String>> databaseId2RewrittenSqls = rewriteResult.getSqls().entrySet().stream()
                        .collect(Collectors.groupingBy(
                                entry -> entry.getKey().getDatabaseId(),
                                Collectors.mapping(entry -> entry.getValue(), Collectors.toList())));
                if (MapUtils.isEmpty(databaseId2RewrittenSqls)) {
                    log.warn("cannot recognize the sql, sql = {}", sql);
                    continue;
                }
                List<ExecutionSubGroupUnit<SqlExecuteReq, SqlExecutionResultWrapper>> executionUnits =
                        new ArrayList<>();
                for (Entry<Long, DataNode> entry : databaseId2DataNodes.entrySet()) {
                    Long databaseId = entry.getKey();
                    DataNode dataNode = entry.getValue();
                    String rewrittenSqls =
                            databaseId2RewrittenSqls.getOrDefault(databaseId, Collections.emptyList()).stream()
                                    .collect(Collectors.joining(taskParameters.getDelimiter()));
                    SqlExecuteReq req = new SqlExecuteReq();
                    req.setSql(rewrittenSqls);
                    req.setDialectType(dialectType);
                    req.setConnectionConfig(dataNode.getDataSourceConfig());
                    ExecutionHandler<SqlExecuteReq, SqlExecutionResultWrapper> callback =
                            new SqlExecutionHandler(
                                    new SqlExecuteReq(rewrittenSqls, order, taskParameters.getTimeoutMillis(),
                                            dialectType,
                                            dataNode.getDataSourceConfig(),
                                            taskParameters.getLogicalDatabaseResp().getId(),
                                            dataNode.getDatabaseId(), taskParameters.getScheduleTaskId()));
                    executionUnits.add(new ExecutionSubGroupUnit<>(StringUtils.uuid(), order, callback, req));
                }
                order++;
                executionGroups.add(dialectType == DialectType.MYSQL ? new MySQLExecutionGroup(executionUnits)
                        : new OBExecutionGroup(executionUnits));
            }
            log.info("start logical database change task, generatedSql = {}", executionGroups.stream().map(
                    group -> group.getExecutionUnits().stream().map(unit -> unit.getInput().getSql()).collect(
                            Collectors.joining(taskParameters.getDelimiter())))
                    .collect(Collectors.joining(taskParameters.getDelimiter())));
            int maxSubGroupSize =
                    executionGroups.stream().max(Comparator.comparingInt(group -> group.getSubGroups().size()))
                            .map(group -> group.getExecutionUnits().size()).orElse(1);
            executorEngine = new GroupExecutionEngine<SqlExecuteReq, SqlExecuteResult>(
                    Math.max(SystemUtils.availableProcessors(), maxSubGroupSize));
            this.executionGroupContext = executorEngine.execute(executionGroups);
        } catch (Exception ex) {
            log.warn("start logical database change task failed, ", ex);
            context.getExceptionListener().onException(ex);
            return false;
        }
        while (!Thread.currentThread().isInterrupted()) {
            if (this.executionGroupContext.isCompleted()) {
                log.info("logical database change task is completed");
                return true;
            }
            if (CollectionUtils.isNotEmpty(this.executionGroupContext.getThrowables())) {
                log.warn("logical database change task failed, ", this.executionGroupContext.getThrowables());
                context.getExceptionListener().onException(this.executionGroupContext.getThrowables().get(0));
                return false;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    @Override
    public void stop() {
        this.executorEngine.terminateAll();
    }

    @Override
    public void close() throws Exception {
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
    public boolean modify(Map<String, String> jobParameters) {
        if (!super.modify(jobParameters)) {
            return false;
        }
        afterModifiedJobParameters();
        return true;
    }

    protected void afterModifiedJobParameters() {
        Map<String, String> currentJobParameters = jobContext.getJobParameters();
        if (currentJobParameters == null || currentJobParameters.isEmpty()) {
            return;
        }
        if (currentJobParameters.containsKey(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_SKIP_UNIT)) {
            String executionUnitId =
                    currentJobParameters.get(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_SKIP_UNIT);
            if (StringUtils.isNotEmpty(executionUnitId)) {
                this.executorEngine.skip(executionUnitId);
            }
        }
        if (currentJobParameters.containsKey(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_TERMINATE_UNIT)) {
            String executionUnitId =
                    currentJobParameters.get(JobParametersKeyConstants.LOGICAL_DATABASE_CHANGE_TERMINATE_UNIT);
            log.info("start to terminate");
            if (StringUtils.isNotEmpty(executionUnitId)) {
                log.info("start to terminate executionUnitId={}", executionUnitId);
                this.executorEngine.terminate(executionUnitId);
            }
        }
    }
}
