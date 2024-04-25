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
package com.oceanbase.odc.service.schedule.job;

import java.util.List;

import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactories;
import com.oceanbase.odc.core.sql.parser.AbstractSyntaxTreeFactory;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.dlm.DataSourceInfoBuilder;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataArchiveTableConfig;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.sqlparser.adapter.mysql.MySQLFromReferenceFactory;
import com.oceanbase.tools.sqlparser.obmysql.OBParser.Create_table_stmtContext;
import com.oceanbase.tools.sqlparser.statement.common.RelationFactor;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/5/9 14:46
 * @Descripition:
 */
@Slf4j
public class DataArchiveJob extends AbstractDlmJob {
    @Override
    public void executeJob(JobExecutionContext context) {
        // execute in task framework.
        if (taskFrameworkProperties.isEnabled()) {
            executeInTaskFramework(context);
            return;
        }
        jobThread = Thread.currentThread();

        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();

        List<DlmTask> taskUnits = getTaskUnits(taskEntity);

        executeTask(taskEntity.getId(), taskUnits);
        TaskStatus taskStatus = getTaskStatus(taskUnits);
        scheduleTaskRepository.updateStatusById(taskEntity.getId(), taskStatus);

        DataArchiveParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);

        if (taskStatus == TaskStatus.DONE && parameters.isDeleteAfterMigration()) {
            log.info("Start to create clear job,scheduleTaskId={}", taskEntity.getId());
            scheduleService.dataArchiveDelete(Long.parseLong(taskEntity.getJobName()), taskEntity.getId());
            log.info("Clear job is created,");
        }
    }

    @Override
    public void initTask(DlmTask taskUnit) {
        super.initTask(taskUnit);
        createTargetTable(taskUnit);
    }

    private void executeInTaskFramework(JobExecutionContext context) {
        ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
        DataArchiveParameters dataArchiveParameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);
        DLMJobParameters parameters = new DLMJobParameters();
        parameters.setJobName(taskEntity.getJobName());
        parameters.setScheduleTaskId(taskEntity.getId());
        parameters.setJobType(JobType.MIGRATE);
        parameters.setTables(dataArchiveParameters.getTables());
        for (DataArchiveTableConfig tableConfig : parameters.getTables()) {
            tableConfig.setConditionExpression(StringUtils.isNotEmpty(tableConfig.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(tableConfig.getConditionExpression(),
                            dataArchiveParameters.getVariables(),
                            context.getFireTime())
                    : "");
        }
        parameters.setDeleteAfterMigration(dataArchiveParameters.isDeleteAfterMigration());
        parameters.setMigrationInsertAction(dataArchiveParameters.getMigrationInsertAction());
        parameters.setNeedPrintSqlTrace(dataArchiveParameters.isNeedPrintSqlTrace());
        parameters
                .setRateLimit(limiterService.getByOrderIdOrElseDefaultConfig(Long.parseLong(taskEntity.getJobName())));
        parameters.setWriteThreadCount(dataArchiveParameters.getWriteThreadCount());
        parameters.setReadThreadCount(dataArchiveParameters.getReadThreadCount());
        parameters.setShardingStrategy(dataArchiveParameters.getShardingStrategy());
        parameters.setScanBatchSize(dataArchiveParameters.getScanBatchSize());
        parameters
                .setSourceDs(DataSourceInfoBuilder.build(
                        databaseService.findDataSourceForConnectById(dataArchiveParameters.getSourceDatabaseId())));
        parameters
                .setTargetDs(DataSourceInfoBuilder.build(
                        databaseService.findDataSourceForConnectById(dataArchiveParameters.getTargetDataBaseId())));
        parameters.getSourceDs().setDatabaseName(dataArchiveParameters.getSourceDatabaseName());
        parameters.getTargetDs().setDatabaseName(dataArchiveParameters.getTargetDatabaseName());
        parameters.getSourceDs().setConnectionCount(2 * (parameters.getReadThreadCount()
                + parameters.getWriteThreadCount()));
        parameters.getTargetDs().setConnectionCount(parameters.getSourceDs().getConnectionCount());

        Long jobId = publishJob(parameters);
        scheduleTaskRepository.updateJobIdById(taskEntity.getId(), jobId);
        scheduleTaskRepository.updateTaskResult(taskEntity.getId(), JsonUtils.toJson(parameters));
        log.info("Publish data-archive job to task framework succeed,scheduleTaskId={},jobIdentity={}",
                taskEntity.getId(),
                jobId);
    }


    /**
     * Create the table in the target database before migrating the data.
     */
    private void createTargetTable(DlmTask dlmTask) {


        if (dlmTask.getSourceDs().getDialectType() != dlmTask.getTargetDs().getDialectType()
                || !dlmTask.getSourceDs().getDialectType().isMysql()) {
            log.info("Automatic table creation is not supported,sourceType={},targetType={}",
                    dlmTask.getSourceDs().getDialectType(), dlmTask.getTargetDs().getDialectType());
            return;
        }
        DefaultConnectSessionFactory sourceConnectionSessionFactory =
                new DefaultConnectSessionFactory(dlmTask.getSourceDs());
        ConnectionSession srcSession = sourceConnectionSessionFactory.generateSession();
        String tableDDL;
        try {
            DBSchemaAccessor sourceDsAccessor = DBSchemaAccessors.create(srcSession);
            tableDDL = sourceDsAccessor.getTableDDL(dlmTask.getSourceDs().getDefaultSchema(), dlmTask.getTableName());
            tableDDL = buildCreateTableDDL(tableDDL, dlmTask.getTargetTableName());
        } finally {
            srcSession.expire();
        }

        DefaultConnectSessionFactory targetConnectionSessionFactory =
                new DefaultConnectSessionFactory(dlmTask.getTargetDs());
        ConnectionSession targetSession = targetConnectionSessionFactory.generateSession();
        try {
            DBSchemaAccessor targetDsAccessor = DBSchemaAccessors.create(targetSession);
            List<String> tableNames = targetDsAccessor.showTables(dlmTask.getTargetDs().getDefaultSchema());
            if (tableNames.contains(dlmTask.getTableName())) {
                log.info("Target table exist,tableName={}", dlmTask.getTableName());
                return;
            }
            log.info("Begin to create target table...");
            targetSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(tableDDL);
        } finally {
            targetSession.expire();
        }
    }

    public static String buildCreateTableDDL(String createSql, String targetTableName) {
        AbstractSyntaxTreeFactory factory = AbstractSyntaxTreeFactories.getAstFactory(DialectType.OB_MYSQL, 0);
        Create_table_stmtContext context = (Create_table_stmtContext) factory.buildAst(createSql).getRoot();
        RelationFactor factor = MySQLFromReferenceFactory.getRelationFactor(context.relation_factor());
        StringBuilder sb = new StringBuilder();
        sb.append(createSql, 0, factor.getStart());
        sb.append("`").append(targetTableName).append("`");
        sb.append(createSql, factor.getStop() + 1, createSql.length());
        return sb.toString();
    }

}
