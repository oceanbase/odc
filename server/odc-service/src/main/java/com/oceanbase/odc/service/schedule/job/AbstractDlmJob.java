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
import java.util.Set;
import java.util.stream.Collectors;

import org.quartz.JobExecutionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.dlm.DLMJobFactory;
import com.oceanbase.odc.service.dlm.DlmLimiterService;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DlmTask;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.dlm.utils.DataArchiveConditionUtil;
import com.oceanbase.odc.service.dlm.utils.DlmJobIdUtil;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.flowtask.ScheduleTaskContextHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.executor.task.DataArchiveTask;
import com.oceanbase.odc.service.task.schedule.DefaultJobDefinition;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;
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

    public JobScheduler jobScheduler = null;

    public final TaskFrameworkEnabledProperties taskFrameworkProperties;

    public final TaskFrameworkService taskFrameworkService;
    public Thread jobThread;
    private Job job;


    public AbstractDlmJob() {
        scheduleTaskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
        jobFactory = SpringContextUtil.getBean(DLMJobFactory.class);
        databaseService = SpringContextUtil.getBean(DatabaseService.class);
        scheduleService = SpringContextUtil.getBean(ScheduleService.class);
        limiterService = SpringContextUtil.getBean(DlmLimiterService.class);
        taskFrameworkProperties = SpringContextUtil.getBean(TaskFrameworkEnabledProperties.class);
        taskFrameworkService = SpringContextUtil.getBean(TaskFrameworkService.class);
        if (taskFrameworkProperties.isEnabled()) {
            jobScheduler = SpringContextUtil.getBean(JobScheduler.class);
        }
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
                job = jobFactory.createJob(taskUnit);
                log.info("Create dlm job succeed,taskId={},task parameters={}", taskId, taskUnit);
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
            } catch (JobException e) {
                // used to stop several sub-threads.
                if (job.getJobMeta().isToStop()) {
                    log.info("Data archive task is Interrupted,taskId={}", taskId);
                    taskUnit.setStatus(TaskStatus.CANCELED);
                } else {
                    log.error("Data archive task is failed,taskId={},errorMessage={}", taskId, e);
                    taskUnit.setStatus(TaskStatus.FAILED);
                }
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
            RateLimitConfiguration limiterConfig =
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
            taskUnit.setSyncDBObjectTypes(parameters.getSyncTableStructure());
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
        // Init dataSourceInfo
        taskUnit.setSourceDs(sourceConfig);
        taskUnit.setTargetDs(targetConfig);
    }

    public Long publishJob(DLMJobParameters parameters) {
        Map<String, String> jobData = new HashMap<>();
        jobData.put(JobParametersKeyConstants.META_TASK_PARAMETER_JSON,
                JsonUtils.toJson(parameters));
        SingleJobProperties singleJobProperties = new SingleJobProperties();
        singleJobProperties.setEnableRetryAfterHeartTimeout(true);
        singleJobProperties.setMaxRetryTimesAfterHeartTimeout(2);
        DefaultJobDefinition jobDefinition = DefaultJobDefinition.builder().jobClass(DataArchiveTask.class)
                .jobType("DLM")
                .jobParameters(jobData).jobProperties(singleJobProperties)
                .build();
        return jobScheduler.scheduleJobNow(jobDefinition);
    }

    public DLMJobParameters getDLMJobParameters(Long jobId) {
        return JsonUtils.fromJson(JsonUtils.fromJson(
                taskFrameworkService.find(jobId).getJobParametersJson(),
                new TypeReference<Map<String, String>>() {}).get(JobParametersKeyConstants.META_TASK_PARAMETER_JSON),
                DLMJobParameters.class);
    }


    @Override
    public void execute(JobExecutionContext context) {
        if (context.getResult() == null) {
            log.warn("Concurrent execute is not allowed,job will be existed.jobKey={}",
                    context.getJobDetail().getKey());
            return;
        }
        ScheduleTaskEntity scheduleTask = (ScheduleTaskEntity) context.getResult();
        ScheduleTaskContextHolder.trace(scheduleTask.getJobName(), scheduleTask.getJobGroup(), scheduleTask.getId());
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
        ScheduleTaskContextHolder.clear();
    }

    @Override
    public void interrupt() {
        if (jobThread == null) {
            throw new IllegalStateException("Task is not executing.");
        }
        if (job != null) {
            job.stop();
            log.info("Job will be interrupted,jobId={}", job.getJobMeta().getJobId());
        }
        jobThread.interrupt();
    }
}
