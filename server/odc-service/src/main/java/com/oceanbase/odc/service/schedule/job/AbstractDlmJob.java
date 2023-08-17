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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.dlm.DataArchiveJobFactory;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DlmLimiterConfig;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.common.util.EncryptUtils;
import com.oceanbase.tools.migrator.job.AbstractJob;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/26 20:05
 * @Descripition:
 */
@Slf4j
public class AbstractDlmJob implements OdcJob {

    public final ScheduleTaskRepository scheduleTaskRepository;
    public final DataArchiveJobFactory dataArchiveJobFactory;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final DlmLimiterService limiterService;
    public Thread jobThread;

    private AbstractJob job;


    public AbstractDlmJob() {
        scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        dataArchiveJobFactory = SpringContextUtil.getBean(DataArchiveJobFactory.class);
        databaseService = SpringContextUtil.getBean(DatabaseService.class);
        scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        limiterService = SpringContextUtil.getBean(DlmLimiterService.class);
    }

    public void executeTask(Long taskId, List<DlmTask> taskUnits) {
        scheduleTaskRepository.updateStatusById(taskId, TaskStatus.RUNNING);
        log.info("Task is ready,taskId={}", taskId);
        scheduleTaskRepository.updateTaskResult(taskId, JsonUtils.toJson(taskUnits));
        for (DlmTask taskUnit : taskUnits) {
            if (jobThread.isInterrupted()) {
                taskUnit.setStatus(TaskStatus.CANCELED);
                log.info("Task interrupted and will exit.TaskId={}", taskId);
                continue;
            }
            if (taskUnit.getStatus() == TaskStatus.DONE) {
                log.info("The task unit had been completed,taskId={},tableName={}", taskId, taskUnit.getTableName());
                continue;
            }
            try {
                initTask(taskUnit);
                job = dataArchiveJobFactory.createJob(taskUnit);
                log.info("Create dlm job succeed,taskId={},id={}", taskId, job.getJobMeta().getJobId());
            } catch (Exception e) {
                log.warn("Create dlm job failed,taskId={},tableName={},errorMessage={}", taskId,
                        taskUnit.getTableName(), e);
                taskUnit.setStatus(TaskStatus.FAILED);
                continue;
            }
            try {
                job.run();
                taskUnit.setStatus(TaskStatus.DONE);
                log.info("DLM job succeed,taskId={},unitId={}", taskId, taskUnit.getId());
            } catch (InterruptedException e) {
                log.info("Data archive task is Interrupted,taskId={}", taskId);
                // used to stop several sub-threads.
                job.getJobMeta().closeDataAdapter();
                taskUnit.setStatus(TaskStatus.CANCELED);
                break;
            } catch (Exception e) {
                log.error("Data archive task is failed,taskId={},errorMessage={}", taskId, e);
                job.getJobMeta().closeDataAdapter();
                taskUnit.setStatus(TaskStatus.FAILED);
            } finally {
                scheduleTaskRepository.updateTaskResult(taskId, JsonUtils.toJson(taskUnits));
            }
        }
    }

    public TaskStatus getTaskStatus(List<DlmTask> taskUnits) {
        Set<TaskStatus> collect = taskUnits.stream().map(DlmTask::getStatus).collect(Collectors.toSet());
        if (collect.contains(TaskStatus.DONE) && collect.size() == 1) {
            return TaskStatus.DONE;
        }
        if (jobThread.isInterrupted()) {
            return TaskStatus.CANCELED;
        }
        if (collect.contains(TaskStatus.FAILED)) {
            return TaskStatus.FAILED;
        }
        return TaskStatus.CANCELED;
    }

    public List<DlmTask> getTaskUnits(ScheduleTaskEntity taskEntity) {
        // Resume or retry an existing task.
        if (StringUtils.isNotEmpty(taskEntity.getResultJson())) {
            try {
                return JsonUtils.fromJson(taskEntity.getResultJson(),
                        new TypeReference<List<DlmTask>>() {});
            } catch (Exception e) {
                // Fast failed and cannot retry.
                log.warn("Read task result failed and will migrate all tables.TaskId={}", taskEntity.getId());
                throw new InternalServerError(
                        String.format("Load task progress failed,errorMessage=%s", e.getMessage()));
            }
        } else {
            return splitTask(taskEntity);
        }
    }

    public List<DlmTask> splitTask(ScheduleTaskEntity taskEntity) {

        DataArchiveParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);
        List<DlmTask> taskUnits = new LinkedList<>();
        parameters.getTables().forEach(table -> {
            String condition = StringUtils.isNotEmpty(table.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(table.getConditionExpression(), parameters.getVariables(),
                            taskEntity.getFireTime())
                    : "";
            DlmTask taskUnit = new DlmTask();

            taskUnit.setId(DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                    taskEntity.getId(),
                    taskUnits.size()));
            taskUnit.setTableName(table.getTableName());
            taskUnit.setSourceDatabaseId(parameters.getSourceDatabaseId());
            taskUnit.setTargetDatabaseId(parameters.getTargetDataBaseId());
            taskUnit.setFireTime(taskEntity.getFireTime());
            DlmLimiterConfig limiterConfig =
                    limiterService.getByOrderIdOrElseDefaultConfig(Long.parseLong(taskEntity.getJobName()));
            LogicTableConfig logicTableConfig = new LogicTableConfig();
            logicTableConfig.setMigrateRule(condition);
            logicTableConfig.setCheckMode(CheckMode.MULTIPLE_GET);
            logicTableConfig.setReaderBatchSize(limiterConfig.getBatchSize());
            logicTableConfig.setWriterBatchSize(limiterConfig.getBatchSize());
            logicTableConfig.setMigrationInsertAction(parameters.getMigrationInsertAction());
            taskUnit.setLogicTableConfig(logicTableConfig);
            taskUnit.setStatus(TaskStatus.PREPARING);
            taskUnit.setJobType(JobType.MIGRATE);
            taskUnits.add(taskUnit);
        });
        return taskUnits;
    }

    public void initTask(DlmTask taskUnit) {
        Database sourceDb = databaseService.detail(taskUnit.getSourceDatabaseId());
        Database targetDb = databaseService.detail(taskUnit.getTargetDatabaseId());
        ConnectionConfig sourceConfig = databaseService.findDataSourceForConnectById(
                taskUnit.getSourceDatabaseId());
        ConnectionConfig targetConfig = databaseService.findDataSourceForConnectById(
                taskUnit.getTargetDatabaseId());
        sourceConfig.setDefaultSchema(sourceDb.getName());
        targetConfig.setDefaultSchema(targetDb.getName());
        DefaultConnectSessionFactory sourceConnectionSessionFactory = new DefaultConnectSessionFactory(sourceConfig);
        DefaultConnectSessionFactory targetConnectionSessionFactory = new DefaultConnectSessionFactory(targetConfig);
        // Init dataSourceInfo
        taskUnit.setSourceInfo(getDataSourceInfo(sourceDb, sourceConfig));
        taskUnit.setTargetInfo(getDataSourceInfo(targetDb, targetConfig));

        ConnectionSession targetSession = targetConnectionSessionFactory.generateSession();
        try {
            // Create if target table does not exist
            if (taskUnit.getJobType() == JobType.MIGRATE) {
                DBSchemaAccessor targetDsAccessor = DBSchemaAccessors.create(targetSession);
                List<String> tableNames = targetDsAccessor.showTables(targetDb.getName());
                if (tableNames.contains(taskUnit.getTableName())) {
                    log.info("Target table exist.");
                    return;
                }
                log.info("Begin to create target table...");
                // TODO maybe we can check table ddl.
                ConnectionSession srcSession = sourceConnectionSessionFactory.generateSession();
                String tableDDL;
                try {
                    DBSchemaAccessor sourceDsAccessor = DBSchemaAccessors.create(srcSession);
                    tableDDL = sourceDsAccessor.getTableDDL(sourceDb.getName(), taskUnit.getTableName());
                } finally {
                    srcSession.expire();
                }
                targetSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY).execute(tableDDL);
            }
        } finally {
            targetSession.expire();
        }
    }

    private DataSourceInfo getDataSourceInfo(Database database, ConnectionConfig connectionConfig) {
        DataSourceInfo dataSourceInfo = new DataSourceInfo();
        dataSourceInfo.setDatabaseName(database.getName());
        dataSourceInfo.setObProxy(String.format("%s:%s", connectionConfig.getHost(), connectionConfig.getPort()));
        dataSourceInfo
                .setFullUserName(OBConsoleDataSourceFactory.getUsername(connectionConfig, ConnectionAccountType.MAIN));
        if (StringUtils.isNotEmpty(connectionConfig.getPassword())) {
            dataSourceInfo.setPassword(connectionConfig.getPassword());
        }
        dataSourceInfo.setDbType("OCEANBASEV10");
        dataSourceInfo.setSysUser(connectionConfig.getSysTenantUsername());
        dataSourceInfo.setUserLocalProxy(false);
        if (StringUtils.isNotEmpty(connectionConfig.getSysTenantPassword())) {
            try {
                dataSourceInfo.setSysPassword(EncryptUtils.encode(connectionConfig.getSysTenantPassword()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        dataSourceInfo.setSysDatabaseName("oceanbase");

        return dataSourceInfo;
    }

    @Override
    public void execute(JobExecutionContext context) {

    }

    @Override
    public void interrupt() {
        if (jobThread == null) {
            throw new IllegalStateException("Task is not executing.");
        }
        job.getJobMeta().setToStop(true);
        jobThread.interrupt();
    }
}
