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
package com.oceanbase.odc.service.onlineschemachange;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.task.model.OnlineSchemaChangeTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.task.TaskService;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * OnlineSchemaChangeTask will be invoked async after flow instance is approved
 *
 * @author yaobin
 * @date 2023-05-23
 * @since 4.2.0
 */
@Slf4j
public class OnlineSchemaChangeFlowableTask extends BaseODCFlowTaskDelegate<Void> {
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private QuartzJobService quartzJobService;
    @Autowired
    private OnlineSchemaChangeTaskHandler taskHandler;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private TaskService taskService;
    private volatile long scheduleId;
    private volatile long creatorId;
    private volatile long organizationId;
    private volatile boolean continueOnError;
    private TaskStatus taskStatus;
    /**
     * if failedAsAbnormalState set true, then onFailed function will not set to failed state
     */
    @Setter
    private volatile boolean failedAsAbnormalState = false;

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
        taskService.start(taskId);
        User creator = FlowTaskUtil.getTaskCreator(execution);
        this.creatorId = creator.getId();
        this.organizationId = creator.getOrganizationId();
        long flowTaskId = taskId;
        // for public cloud
        String uid = FlowTaskUtil.getCloudMainAccountId(execution);
        OnlineSchemaChangeParameters parameter = FlowTaskUtil.getOnlineSchemaChangeParameter(execution);
        parameter.setFlowInstanceId(FlowTaskUtil.getFlowInstanceId(execution));
        parameter.setFlowTaskID(taskId);
        ConnectionConfig connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        String schema = FlowTaskUtil.getSchemaName(execution);
        continueOnError = parameter.isContinueOnError();
        OnlineSchemaChangeContextHolder.trace(this.creatorId, flowTaskId, this.organizationId);
        TaskEntity taskEntity = taskService.detail(taskId);
        Database database = databaseService.getBasicSkipPermissionCheck(taskEntity.getDatabaseId());
        ScheduleEntity schedule = createScheduleEntity(connectionConfig, parameter, schema, taskEntity.getDatabaseId(),
                database.getProject().getId());
        scheduleId = schedule.getId();
        try {
            List<ScheduleTaskEntity> tasks = parameter.generateSubTaskParameters(connectionConfig, schema).stream()
                    .map(param -> {
                        param.setUid(uid);
                        param.setRateLimitConfig(parameter.getRateLimitConfig());
                        param.setState(OscStates.YIELD_CONTEXT.getState());
                        return createScheduleTaskEntity(schedule.getId(), param);
                    }).collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(tasks)) {
                taskHandler.start(scheduleId, tasks.get(0).getId());
                log.info("Successfully start schedule task with id={}", tasks.get(0).getId());
            }
        } finally {
            OnlineSchemaChangeContextHolder.clear();
        }
        updateFlowInstanceStatus(FlowStatus.EXECUTING);
        return null;
    }

    @Override
    public void onFailure(Long taskId, TaskService taskService) {
        if (failedAsAbnormalState) {
            log.warn("Online schema change task in abnormal state, taskId={}", taskId);
            updateFlowInstanceStatus(FlowStatus.EXECUTION_ABNORMAL);
        } else {
            log.warn("Online schema change task failed, taskId={}", taskId);
            super.onFailure(taskId, taskService);
            updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
        }
    }

    @Override
    public void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Online schema change task succeed, taskId={}", taskId);
        super.onSuccessful(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    public void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Online schema change task timeout, taskId={}", taskId);
        super.onTimeout(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_EXPIRED);
    }

    /**
     * refresh task's state and try compete task
     * 
     * @param flowInstanceID instance id of flow_instance
     * @param taskID task id of task_task
     * @param schedulerID scheduler id of scheduler_scheduler
     * @param continueOnError
     */
    public void tryCompleteTask(long flowInstanceID, long taskID, long schedulerID, boolean continueOnError) {
        this.flowInstanceId = flowInstanceID;
        this.taskId = taskID;
        this.scheduleId = schedulerID;
        this.continueOnError = continueOnError;
        onProgressUpdate(taskID, taskService);
        switch (taskStatus) {
            case FAILED:
                onFailure(taskID, taskService);
                break;
            case CANCELED:
                cancel(true, taskID, taskService);
                break;
            case DONE:
                onSuccessful(taskID, taskService);
                break;
            default: // do nothing
        }
    }

    /**
     * update progress when action changed
     * 
     * @param taskId taskID for task_task table
     * @param taskService
     */
    @Override
    public void onProgressUpdate(Long taskId, TaskService taskService) {
        try {
            Page<ScheduleTaskEntity> scheduleTasks = list(Pageable.unpaged(), scheduleId);
            if (scheduleTasks.getSize() == 0) {
                log.info("List schedule task size is 0 by scheduleId {}.", scheduleId);
                return;
            }
            // compute status
            TaskStatus currentStatus = computeStatus(scheduleTasks);
            this.taskStatus = currentStatus;
            TaskEntity flowTask = taskService.detail(taskId);
            TaskStatus dbStatus = flowTask.getStatus();
            OnlineSchemaChangeTaskResult previousTaskResult =
                    JsonUtils.fromJson(flowTask.getResultJson(), OnlineSchemaChangeTaskResult.class);
            // get prev and current
            double currentProgressPercentage = getProgressPercentage(scheduleTasks);
            double prevProgressPercentage =
                    null != previousTaskResult ? getProgressPercentage(previousTaskResult.getTasks()) : 0;
            // get swap table flag changed
            int currentEnableManualSwapTableFlagCounts = getManualSwapTableEnableFlagCounts(scheduleTasks);
            int prevEnableManualSwapTableFlagCounts =
                    null != previousTaskResult ? getManualSwapTableEnableFlagCounts(previousTaskResult.getTasks()) : 0;

            if (currentProgressPercentage > prevProgressPercentage || dbStatus != currentStatus
                    || (currentEnableManualSwapTableFlagCounts != prevEnableManualSwapTableFlagCounts)) {
                flowTask.setResultJson(JsonUtils.toJson(new OnlineSchemaChangeTaskResult(scheduleTasks.getContent())));
                flowTask.setStatus(currentStatus);
                flowTask.setProgressPercentage(Math.min(currentProgressPercentage, 100));
                taskService.update(flowTask);
            }
        } catch (Throwable ex) {
            log.warn("onProgressUpdate occur exception,", ex);
        }
    }

    /**
     * get counts of tasks with swap table flag enabled
     */
    protected int getManualSwapTableEnableFlagCounts(Iterable<ScheduleTaskEntity> tasks) {
        if (null == tasks) {
            return 0;
        }
        int ret = 0;
        for (ScheduleTaskEntity task : tasks) {
            // check current
            OnlineSchemaChangeScheduleTaskResult result = JsonUtils.fromJson(task.getResultJson(),
                    OnlineSchemaChangeScheduleTaskResult.class);
            if (null != result && result.isManualSwapTableEnabled()) {
                ret++;
            }
        }
        return ret;
    }

    protected double getProgressPercentage(Iterable<ScheduleTaskEntity> tasks) {
        if (null == tasks) {
            return 0.0;
        }
        int taskCounts = 0;
        double percentageSum = 0.0;
        for (ScheduleTaskEntity task : tasks) {
            percentageSum += singleTaskPercentage(task);
            taskCounts++;
        }
        return taskCounts == 0 ? 0 : percentageSum * 100 / taskCounts;
    }

    /**
     * compute current status by saved task status
     * 
     * @param tasks
     * @return
     */
    @VisibleForTesting
    protected TaskStatus computeStatus(Page<ScheduleTaskEntity> tasks) {
        int successfulTask = 0;
        int failedTask = 0;
        boolean canceled = false;

        for (ScheduleTaskEntity task : tasks) {
            TaskStatus taskStatus = task.getStatus();
            if (taskStatus == TaskStatus.DONE) {
                successfulTask++;
            } else if (taskStatus == TaskStatus.FAILED) {
                failedTask++;
            } else if (taskStatus == TaskStatus.CANCELED) {
                canceled = true;
            }
        }

        if (canceled) {
            return TaskStatus.CANCELED;
        } else if ((successfulTask + failedTask) == tasks.getSize()) {
            return failedTask == 0 ? TaskStatus.DONE : TaskStatus.FAILED;
        } else if (failedTask > 0 && !continueOnError) {
            return TaskStatus.FAILED;
        } else {
            return TaskStatus.RUNNING;
        }
    }

    private double singleTaskPercentage(ScheduleTaskEntity scheduleTask) {
        double percentage;
        switch (scheduleTask.getStatus()) {
            case PREPARING:
                percentage = 0;
                break;
            case RUNNING:
                percentage = scheduleTask.getProgressPercentage() / 100;
                break;
            default:
                percentage = 1;
        }
        return percentage;

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        Page<ScheduleTaskEntity> scheduleTasks = list(Pageable.unpaged(), scheduleId);
        Optional<ScheduleTaskEntity> runningEntity = Optional.empty();
        for (ScheduleTaskEntity task : scheduleTasks) {
            if (TaskStatus.RUNNING == task.getStatus()) {
                runningEntity = Optional.of(task);
                break;
            }
        }
        runningEntity.ifPresent(scheduleTask -> taskHandler.terminate(scheduleId, scheduleTask.getId()));
        taskService.cancel(taskId);
        return true;
    }

    /**
     * query all scheduler task entity with scheduleId
     * 
     * @return
     */
    protected Page<ScheduleTaskEntity> list(Pageable pageable, Long scheduleId) {
        Specification<ScheduleTaskEntity> specification =
                Specification.where(ScheduleTaskSpecs.jobNameEquals(scheduleId.toString()));
        return scheduleTaskRepository.findAll(specification, pageable);
    }

    private ScheduleEntity createScheduleEntity(ConnectionConfig connectionConfig,
            OnlineSchemaChangeParameters parameter, String schema, Long databaseId, Long projectId) {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setDataSourceId(connectionConfig.id());
        scheduleEntity.setDatabaseName(schema);
        scheduleEntity.setType(ScheduleType.ONLINE_SCHEMA_CHANGE_COMPLETE);
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setAllowConcurrent(false);
        scheduleEntity.setCreatorId(creatorId);
        scheduleEntity.setOrganizationId(organizationId);
        scheduleEntity.setProjectId(projectId);
        scheduleEntity.setDatabaseId(databaseId);
        scheduleEntity.setModifierId(scheduleEntity.getCreatorId());
        TriggerConfig triggerConfig = new TriggerConfig();
        triggerConfig.setTriggerStrategy(TriggerStrategy.CRON);
        triggerConfig.setCronExpression(onlineSchemaChangeProperties.getOms().getCheckProjectProgressCronExpression());
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));
        scheduleEntity.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
        scheduleEntity.setJobParametersJson(JsonUtils.toJson(parameter));
        return scheduleService.create(scheduleEntity);
    }

    private ScheduleTaskEntity createScheduleTaskEntity(Long scheduleId,
            OnlineSchemaChangeScheduleTaskParameters param) {
        ScheduleTaskEntity scheduleTaskEntity = new ScheduleTaskEntity();
        scheduleTaskEntity.setStatus(TaskStatus.PREPARING);
        scheduleTaskEntity.setJobName(scheduleId.toString());
        scheduleTaskEntity.setJobGroup(JobType.ONLINE_SCHEMA_CHANGE_COMPLETE.name());
        scheduleTaskEntity.setProgressPercentage(0.0);
        scheduleTaskEntity.setParametersJson(JsonUtils.toJson(param));
        scheduleTaskEntity.setResultJson(JsonUtils.toJson(new OnlineSchemaChangeScheduleTaskResult(param)));
        scheduleTaskEntity.setFireTime(new Date());
        return scheduleTaskRepository.saveAndFlush(scheduleTaskEntity);
    }

    public TaskType getTaskType() {
        return TaskType.ONLINE_SCHEMA_CHANGE;
    }

    // do nothing
    protected void initMonitorExecutor() {}

    // do nothing
    protected void completeTask() {}

    @Override
    public boolean isCancelled() {
        throw new RuntimeException("not impl");
    }

    @Override
    protected boolean isSuccessful() {
        throw new RuntimeException("not impl");
    }

    @Override
    protected boolean isFailure() {
        throw new RuntimeException("not impl");
    }
}
