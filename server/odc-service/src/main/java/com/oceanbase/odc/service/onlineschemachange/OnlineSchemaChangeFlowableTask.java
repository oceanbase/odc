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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
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
import com.oceanbase.odc.service.onlineschemachange.subtask.OscTaskCompleteHandler;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
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
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private QuartzJobService quartzJobService;
    @Autowired
    private OnlineSchemaChangeTaskHandler taskHandler;
    @Autowired
    private OscTaskCompleteHandler completeHandler;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OnlineSchemaChangeProperties onlineSchemaChangeProperties;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private TaskService taskService;
    private volatile TaskStatus status;
    private volatile long scheduleId;
    private volatile long creatorId;
    private volatile long organizationId;
    private volatile boolean continueOnError;
    private volatile double percentage;
    private volatile Set<Long> lastManualSwapTableEnableTasks = new HashSet<>();

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception {
        taskService.start(taskId);
        User creator = FlowTaskUtil.getTaskCreator(execution);
        this.creatorId = creator.getId();
        this.organizationId = creator.getOrganizationId();
        this.status = TaskStatus.RUNNING;
        long flowTaskId = taskId;
        // for public cloud
        String uid = FlowTaskUtil.getCloudMainAccountId(execution);
        OnlineSchemaChangeParameters parameter = FlowTaskUtil.getOnlineSchemaChangeParameter(execution);
        parameter.setFlowInstanceId(FlowTaskUtil.getFlowInstanceId(execution));
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
                        return createScheduleTaskEntity(schedule.getId(), param);
                    }).collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(tasks)) {
                taskHandler.start(scheduleId, tasks.get(0).getId());
                log.info("Successfully start schedule task with id={}", tasks.get(0).getId());
            }
        } finally {
            OnlineSchemaChangeContextHolder.clear();
        }
        return null;
    }

    @Override
    protected boolean isSuccessful() {
        return status == TaskStatus.DONE;
    }

    @Override
    protected boolean isFailure() {
        return status == TaskStatus.FAILED;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Online schema change task failed, taskId={}", taskId);
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Online schema change task succeed, taskId={}", taskId);
        super.onSuccessful(taskId, taskService);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Online schema change task timeout, taskId={}", taskId);
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        try {
            Page<ScheduleTaskEntity> tasks = scheduleTaskService.listTask(Pageable.unpaged(), scheduleId);
            if (tasks.getSize() == 0) {
                log.info("List schedule task size is 0 by scheduleId {}.", scheduleId);
                return;
            }
            progressStatusUpdate(tasks);

            Optional<Double> res = tasks.stream().map(this::singleTaskPercentage).reduce(Double::sum);
            double currentPercentage = res.get() * 100 / tasks.getSize();

            TaskEntity flowTask = taskService.detail(taskId);
            TaskStatus dbStatus = flowTask.getStatus();

            Set<Long> currentManualSwapTableEnableTasks = new HashSet<>();
            for (ScheduleTaskEntity task : tasks) {
                OnlineSchemaChangeScheduleTaskResult result = JsonUtils.fromJson(task.getResultJson(),
                        OnlineSchemaChangeScheduleTaskResult.class);
                if (result.isManualSwapTableEnabled()) {
                    currentManualSwapTableEnableTasks.add(task.getId());
                }
            }

            boolean manualSwapTableTasksChanged = !CollectionUtils.isEqualCollection(currentManualSwapTableEnableTasks,
                    lastManualSwapTableEnableTasks);
            if (manualSwapTableTasksChanged) {
                lastManualSwapTableEnableTasks = currentManualSwapTableEnableTasks;
            }

            if (currentPercentage > this.percentage || dbStatus != this.status || manualSwapTableTasksChanged) {
                flowTask.setResultJson(JsonUtils.toJson(new OnlineSchemaChangeTaskResult(tasks.getContent())));
                flowTask.setStatus(this.status);
                flowTask.setProgressPercentage(Math.min(currentPercentage, 100));
                taskService.update(flowTask);
            }
            this.percentage = currentPercentage;
        } catch (Throwable ex) {
            log.warn("onProgressUpdate occur exception,", ex);
        }

    }

    private void progressStatusUpdate(Page<ScheduleTaskEntity> tasks) {
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
            this.status = TaskStatus.CANCELED;
        } else if ((successfulTask + failedTask) == tasks.getSize()) {
            this.status = failedTask == 0 ? TaskStatus.DONE : TaskStatus.FAILED;
        } else if (failedTask > 0 && !continueOnError) {
            this.status = TaskStatus.FAILED;
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
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        Optional<ScheduleTaskEntity> runningEntity = scheduleTaskService.listTask(Pageable.unpaged(), scheduleId)
                .stream().filter(task -> task.getStatus() == TaskStatus.RUNNING)
                .findFirst();
        runningEntity.ifPresent(scheduleTask -> taskHandler.terminate(scheduleId, scheduleTask.getId()));
        this.status = TaskStatus.CANCELED;
        taskService.cancel(taskId);
        return true;
    }

    @Override
    public boolean isCancelled() {
        return status == TaskStatus.CANCELED;
    }

    private ScheduleEntity createScheduleEntity(ConnectionConfig connectionConfig,
            OnlineSchemaChangeParameters parameter, String schema, Long databaseId, Long projectId) {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setConnectionId(connectionConfig.id());
        scheduleEntity.setDatabaseName(schema);
        scheduleEntity.setJobType(JobType.ONLINE_SCHEMA_CHANGE_COMPLETE);
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

}
