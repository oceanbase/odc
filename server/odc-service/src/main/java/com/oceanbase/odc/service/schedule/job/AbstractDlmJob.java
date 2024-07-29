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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.dlm.DLMService;
import com.oceanbase.odc.service.dlm.DLMTableStructureSynchronizer;
import com.oceanbase.odc.service.dlm.DataSourceInfoMapper;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DlmTableUnit;
import com.oceanbase.odc.service.dlm.model.DlmTableUnitParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.DlmTableUnitStatistic;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.DataArchiveTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;
import com.oceanbase.tools.migrator.common.enums.JobType;
import com.oceanbase.tools.migrator.common.exception.JobException;
import com.oceanbase.tools.migrator.job.Job;
import com.oceanbase.tools.migrator.task.CheckMode;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/26 20:05
 * @Descripition:
 */
@Slf4j
public abstract class AbstractDlmJob implements OdcJob {

    public final ScheduleTaskRepository scheduleTaskRepository;
    public final DLMJobFactory jobFactory;
    public final DatabaseService databaseService;
    public final ScheduleService scheduleService;
    public final DlmLimiterService limiterService;
    public final DLMService dlmService;

    public JobScheduler jobScheduler = null;

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;

    public final TaskFrameworkService taskFrameworkService;
    private Job job;
    private boolean isInterrupted = false;
    private Thread monitor;


    public AbstractDlmJob() {
        scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        jobFactory = SpringContextUtil.getBean(DLMJobFactory.class);
        databaseService = SpringContextUtil.getBean(DatabaseService.class);
        scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        limiterService = SpringContextUtil.getBean(DlmLimiterService.class);
        taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        taskFrameworkService = SpringContextUtil.getBean(TaskFrameworkService.class);
        dlmService = SpringContextUtil.getBean(DLMService.class);
        if (taskFrameworkProperties.isEnabled()) {
            jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        }
    }

    public void executeTask(Long taskId, List<DlmTableUnit> dlmTableUnits, Long timeoutMillis) {
        initMonitor(taskId, timeoutMillis);
        scheduleTaskRepository.updateStatusById(taskId, TaskStatus.RUNNING);
        log.info("Task is ready,taskId={}", taskId);
        for (DlmTableUnit dlmTableUnit : dlmTableUnits) {
            if (isInterrupted) {
                log.info("Task interrupted and will exit.TaskId={}", taskId);
                continue;
            }
            if (dlmTableUnit.getStatus() == TaskStatus.DONE) {
                log.info("The task unit had been completed,taskId={},tableName={}", taskId,
                        dlmTableUnit.getTableName());
                continue;
            }
            if (dlmTableUnit.getType() == JobType.MIGRATE) {
                try {
                    DLMTableStructureSynchronizer.sync(dlmTableUnit.getSourceDatasourceInfo(),
                            dlmTableUnit.getTargetDatasourceInfo(), dlmTableUnit.getTableName(),
                            dlmTableUnit.getTargetTableName(),
                            dlmTableUnit.getParameters().getSyncDBObjectType());
                } catch (Exception e) {
                    log.warn("Sync table structure failed,tableName={}", dlmTableUnit.getTableName(), e);
                    dlmService.updateStatusByDlmTableUnitId(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                    continue;
                }
            }
            try {
                job = jobFactory.createJob(dlmTableUnit);
                log.info("Create dlm job succeed,taskId={},task parameters={}", taskId, dlmTableUnit);
            } catch (Exception e) {
                log.warn("Create dlm job failed,taskId={},tableName={},errorMessage={}", taskId,
                        dlmTableUnit.getTableName(), e);
                dlmService.updateStatusByDlmTableUnitId(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                continue;
            }
            try {
                dlmService.updateStatusByDlmTableUnitId(dlmTableUnit.getDlmTableUnitId(), TaskStatus.RUNNING);
                job.run();
                dlmService.updateStatusByDlmTableUnitId(dlmTableUnit.getDlmTableUnitId(), TaskStatus.DONE);
                log.info("DLM job succeed,taskId={},unitId={}", taskId, dlmTableUnit.getDlmTableUnitId());
            } catch (JobException e) {
                // used to stop several sub-threads.
                if (isInterrupted) {
                    log.info("Data archive task is Interrupted,taskId={}", taskId);
                    dlmService.updateStatusByDlmTableUnitId(dlmTableUnit.getDlmTableUnitId(), TaskStatus.CANCELED);
                } else {
                    log.error("Data archive task is failed,taskId={},errorMessage={}", taskId, e);
                    dlmService.updateStatusByDlmTableUnitId(dlmTableUnit.getDlmTableUnitId(), TaskStatus.FAILED);
                }
            }
        }
    }

    public TaskStatus getTaskStatus(Long scheduleTaskId) {
        return dlmService.getTaskStatus(scheduleTaskId);
    }

    public List<DlmTableUnit> getTaskUnits(ScheduleTaskEntity taskEntity) {
        // Resume or retry an existing task.
        List<DlmTableUnit> dlmJobs = dlmService.findByScheduleTaskId(taskEntity.getId());
        if (!dlmJobs.isEmpty()) {
            return dlmJobs;
        }
        return splitTask(taskEntity);
    }

    public List<DlmTableUnit> splitTask(ScheduleTaskEntity taskEntity) {

        DataArchiveParameters parameters = JsonUtils.fromJson(taskEntity.getParametersJson(),
                DataArchiveParameters.class);
        List<DlmTableUnit> dlmTableUnits = new LinkedList<>();
        parameters.getTables().forEach(table -> {
            String condition = StringUtils.isNotEmpty(table.getConditionExpression())
                    ? DataArchiveConditionUtil.parseCondition(table.getConditionExpression(), parameters.getVariables(),
                            taskEntity.getFireTime())
                    : "";
            DlmTableUnit dlmTableUnit = new DlmTableUnit();
            dlmTableUnit.setScheduleTaskId(taskEntity.getId());
            DlmTableUnitParameters jobParameter = new DlmTableUnitParameters();
            RateLimitConfiguration limiterConfig =
                    limiterService.getByOrderIdOrElseDefaultConfig(Long.parseLong(taskEntity.getJobName()));
            jobParameter.setMigrateRule(condition);
            jobParameter.setCheckMode(CheckMode.MULTIPLE_GET);
            jobParameter.setGeneratorBatchSize(parameters.getScanBatchSize());
            jobParameter.setShardingStrategy(parameters.getShardingStrategy());
            jobParameter.setReaderTaskCount(parameters.getReadThreadCount());
            jobParameter.setWriterTaskCount(parameters.getWriteThreadCount());
            jobParameter.setReaderBatchSize(limiterConfig.getBatchSize());
            jobParameter.setWriterBatchSize(limiterConfig.getBatchSize());
            jobParameter.setMigrationInsertAction(parameters.getMigrationInsertAction());
            jobParameter.setSyncDBObjectType(parameters.getSyncTableStructure());
            jobParameter.setMigratePartitions(table.getPartitions());
            dlmTableUnit.setParameters(jobParameter);
            dlmTableUnit.setDlmTableUnitId(
                    DlmJobIdUtil.generateHistoryJobId(taskEntity.getJobName(), taskEntity.getJobGroup(),
                            taskEntity.getId(),
                            dlmTableUnits.size()));
            dlmTableUnit.setTableName(table.getTableName());
            dlmTableUnit.setTargetTableName(table.getTargetTableName());
            dlmTableUnit.setSourceDatasourceInfo(getDataSourceInfo(parameters.getSourceDatabaseId()));
            dlmTableUnit.setTargetDatasourceInfo(getDataSourceInfo(parameters.getTargetDataBaseId()));
            dlmTableUnit.getSourceDatasourceInfo().setQueryTimeout(parameters.getQueryTimeout());
            dlmTableUnit.getTargetDatasourceInfo().setQueryTimeout(parameters.getQueryTimeout());
            dlmTableUnit.setFireTime(taskEntity.getFireTime());
            dlmTableUnit.setStatus(TaskStatus.PREPARING);
            dlmTableUnit.setType(JobType.MIGRATE);
            dlmTableUnit.setStatistic(new DlmTableUnitStatistic());
            dlmTableUnits.add(dlmTableUnit);
        });
        dlmService.createDlmTableUnits(dlmTableUnits);
        return dlmTableUnits;
    }

    public DataSourceInfo getDataSourceInfo(Long databaseId) {
        Database db = databaseService.detail(databaseId);
        ConnectionConfig config = databaseService.findDataSourceForConnectById(databaseId);
        DataSourceInfo dataSourceInfo = DataSourceInfoMapper.toDataSourceInfo(config);
        dataSourceInfo.setDatabaseName(db.getName());
        return dataSourceInfo;
    }

    public Long publishJob(DLMJobReq parameters, Long timeoutMillis) {
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON,
                JsonUtils.toJson(parameters));
        if (timeoutMillis != null) {
            jobData.put(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS, timeoutMillis.toString());
        }
        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(2);
        DefaultJobDefinition jobDefinition = DefaultJobDefinition.builder().jobClass(DataArchiveTask.class)
                .jobType("DLM")
                .jobParameters(jobData).jobProperties(singleJobProperties)
                .build();
        return jobScheduler.scheduleJobNow(jobDefinition);
    }

    public DLMJobReq getDLMJobReq(Long jobId) {
        return JsonUtils.fromJson(JsonUtils.fromJson(
                taskFrameworkService.find(jobId).getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}).get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobReq.class);
    }


    @Override
    public void execute(JobExecutionContext context) {
        if (context.getResult() == null) {
            log.warn("Concurrent execute is not allowed,job will be existed.jobKey={}",
                    context.getJobDetail().getKey());
            return;
        }
        executeJob(context);
    }

    public abstract void executeJob(JobExecutionContext context);


    @Override
    public void before(JobExecutionContext context) {
        scheduleService.refreshScheduleStatus(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void after(JobExecutionContext context) {
        scheduleService.refreshScheduleStatus(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void interrupt() {
        isInterrupted = true;
        if (job != null) {
            job.stop();
            log.info("Job will be interrupted,jobId={}", job.getJobMeta().getJobId());
        }
    }

    private void initMonitor(Long scheduleTaskId, Long timeoutMillis) {
        if (timeoutMillis == null) {
            return;
        }
        monitor = new Thread(() -> {
            long elapsedTime = 0L;
            while (elapsedTime < timeoutMillis) {
                try {
                    Thread.sleep(10000);
                    elapsedTime += 10000;
                } catch (InterruptedException e) {
                    return;
                }
            }
            log.info("Job execution timed out,initiating job suspension.");
            this.interrupt();
        });
        monitor.setName(String.format("%s-%s", "TimeoutMonitor", scheduleTaskId));
        monitor.setDaemon(true);
        monitor.start();
    }
}
