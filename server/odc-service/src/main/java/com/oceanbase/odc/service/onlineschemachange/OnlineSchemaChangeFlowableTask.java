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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
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
import com.oceanbase.odc.service.quartz.QuartzJobServiceProxy;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.task.TaskService;

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
    @Qualifier("quartzJobServiceProxy")
    private QuartzJobServiceProxy quartzJobService;
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
    private TaskStatus taskStatus;

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
                        // assign when create task
                        param.setUseODCMigrateTool(onlineSchemaChangeProperties.isUseOdcMigrateTool());
                        param.setOdcCommandURl(onlineSchemaChangeProperties.getOdcMigrateUrl());
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
        log.warn("Online schema change task failed, taskId={}", taskId);
        super.onFailure(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
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
     * refresh task's state and try complete task
     * 
     * @param flowInstanceID instance id of flow_instance
     * @param taskID task id of task_task
     * @param schedulerID scheduler id of scheduler_scheduler
     */
    public void tryCompleteTask(long flowInstanceID, long taskID, long schedulerID) {
        this.flowInstanceId = flowInstanceID;
        this.taskId = taskID;
        this.scheduleId = schedulerID;
        onProgressUpdate(taskID, taskService);
        switch (taskStatus) {
            case ABNORMAL:
                processAbnormal(taskID, taskService);
                break;
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
            // get swap table flag and oms step changed hint
            ScheduleTasksUpdateHint currentHint = getScheduleTasksUpdateHint(scheduleTasks);
            ScheduleTasksUpdateHint prevHint =
                    null != previousTaskResult ? getScheduleTasksUpdateHint(previousTaskResult.getTasks())
                            : new ScheduleTasksUpdateHint(0);

            if (currentProgressPercentage > prevProgressPercentage || dbStatus != currentStatus
                    || currentHint.hasDiff(prevHint)) {
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
     * get schedule task update hint including manual swap table counts and steps
     */
    protected ScheduleTasksUpdateHint getScheduleTasksUpdateHint(Iterable<ScheduleTaskEntity> tasks) {
        if (null == tasks) {
            return new ScheduleTasksUpdateHint(0);
        }
        int ret = 0;
        Map<Long, String> taskAndStep = new HashMap<>();
        Map<Long, String> taskAndStatus = new HashMap<>();
        for (ScheduleTaskEntity task : tasks) {
            // check current
            OnlineSchemaChangeScheduleTaskResult result = JsonUtils.fromJson(task.getResultJson(),
                    OnlineSchemaChangeScheduleTaskResult.class);
            OnlineSchemaChangeScheduleTaskParameters onlineSchemaChangeScheduleTaskParameters =
                    JsonUtils.fromJson(task.getParametersJson(), OnlineSchemaChangeScheduleTaskParameters.class);
            if (null != result) {
                ret += (result.isManualSwapTableEnabled() ? 1 : 0);
                taskAndStep.put(task.getId(), result.getCurrentStep());
            }
            if (null != onlineSchemaChangeScheduleTaskParameters) {
                taskAndStep.compute(task.getId(), (k, v) -> {
                    return v + onlineSchemaChangeScheduleTaskParameters.getState();
                });
            }
            taskAndStatus.put(task.getId(), task.getStatus().name());
        }
        return new ScheduleTasksUpdateHint(ret, taskAndStep, taskAndStatus);
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
        int abnormalTask = 0;
        boolean canceled = false;

        for (ScheduleTaskEntity task : tasks) {
            TaskStatus taskStatus = task.getStatus();
            if (taskStatus == TaskStatus.DONE) {
                successfulTask++;
            } else if (taskStatus == TaskStatus.FAILED) {
                failedTask++;
            } else if (taskStatus == TaskStatus.CANCELED) {
                canceled = true;
            } else if (taskStatus == TaskStatus.ABNORMAL) {
                abnormalTask++;
            }
        }

        if (canceled) {
            return TaskStatus.CANCELED;
        } else if (abnormalTask != 0) {
            return TaskStatus.ABNORMAL;
        } else if ((successfulTask + failedTask) == tasks.getSize()) {
            return failedTask == 0 ? TaskStatus.DONE : TaskStatus.FAILED;
        } else if (failedTask > 0) {
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
            case ABNORMAL:
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

    public void processAbnormal(Long taskId, TaskService taskService) {
        log.info("Online schema change task abnormal, taskId={}", taskId);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_ABNORMAL);
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

    /**
     * hint to determinate if schedule task has changed
     */
    protected static final class ScheduleTasksUpdateHint {
        private final int enableManualSwapTableFlagCounts;
        // oms steps map
        private final Map<Long, String> taskStepsMap = new HashMap<>();
        // task status map
        private final Map<Long, String> taskStatusMap = new HashMap<>();

        protected ScheduleTasksUpdateHint(int enableManualSwapTableFlagCounts, Map<Long, String> taskStepsMap,
                Map<Long, String> taskStatusMap) {
            this.enableManualSwapTableFlagCounts = enableManualSwapTableFlagCounts;
            this.taskStepsMap.putAll(taskStepsMap);
            this.taskStatusMap.putAll(taskStatusMap);
        }

        protected ScheduleTasksUpdateHint(int enableManualSwapTableFlagCounts) {
            this.enableManualSwapTableFlagCounts = enableManualSwapTableFlagCounts;
        }

        // if two hint has diff
        public boolean hasDiff(@NotNull ScheduleTasksUpdateHint other) {
            if (other.enableManualSwapTableFlagCounts != this.enableManualSwapTableFlagCounts) {
                return true;
            }
            // steps change
            if (mapHasDiff(taskStepsMap, other.taskStepsMap)) {
                return true;
            }
            // status change
            return mapHasDiff(taskStatusMap, other.taskStatusMap);
        }

        protected boolean mapHasDiff(Map<Long, String> src, Map<Long, String> dst) {
            if (src.size() != dst.size()) {
                return true;
            }
            for (Long scheduleTaskId : src.keySet()) {
                if (!StringUtils.equalsIgnoreCase(src.get(scheduleTaskId),
                        dst.get(scheduleTaskId))) {
                    return true;
                }
            }
            return false;
        }

        @VisibleForTesting
        public int getEnableManualSwapTableFlagCounts() {
            return enableManualSwapTableFlagCounts;
        }

        @VisibleForTesting
        public Map<Long, String> getTaskStepsMap() {
            return taskStepsMap;
        }

        @VisibleForTesting
        public Map<Long, String> getTaskStatusMap() {
            return taskStatusMap;
        }
    }
}
