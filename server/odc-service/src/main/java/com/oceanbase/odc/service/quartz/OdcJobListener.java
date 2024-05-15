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
package com.oceanbase.odc.service.quartz;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.notification.Broker;
import com.oceanbase.odc.service.notification.NotificationProperties;
import com.oceanbase.odc.service.notification.helper.EventBuilder;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.flowtask.ScheduleTaskContextHolder;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2023/6/1 11:23
 * @Descripition: Execute before or after schedule job.
 */

@Slf4j
@Component
public class OdcJobListener implements JobListener {

    @Autowired
    private ScheduleTaskRepository taskRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private HostProperties hostProperties;
    @Autowired
    private Broker broker;
    @Autowired
    private EventBuilder eventBuilder;
    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private TaskFrameworkEnabledProperties taskFrameworkEnabledProperties;
    private static final String ODC_JOB_LISTENER = "ODC_JOB_LISTENER";

    @Override
    public String getName() {
        return ODC_JOB_LISTENER;
    }

    /**
     * This method will be executed before job execute.
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // todo skip osc task
        if (Objects.equals(context.getJobDetail().getKey().getGroup(), JobType.ONLINE_SCHEMA_CHANGE_COMPLETE.name())) {
            return;
        }
        // Init user.
        Long scheduleId = ScheduleTaskUtils.getScheduleId(context);
        ScheduleEntity scheduleEntity =
                scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE, "id", scheduleId));
        ScheduleTaskContextHolder.trace(scheduleEntity.getId(), scheduleEntity.getJobType().name(), null);
        log.info("Job to be executed.OrganizationId={},ProjectId={},DatabaseId={},JobType={}",
                scheduleEntity.getOrganizationId(), scheduleEntity.getProjectId(), scheduleEntity.getDatabaseId(),
                scheduleEntity.getJobType());
        // For tasks that do not allow concurrent execution, if they can be successfully scheduled, it
        // indicates that the existing tasks have exited. If there are still tasks in the processing state,
        // then it is necessary to correct their status.
        if (!taskFrameworkEnabledProperties.isEnabled() && context.getJobDetail().isConcurrentExectionDisallowed()
                && scheduleEntity.getJobType().isSync()) {
            List<ScheduleTaskEntity> processingTask = taskRepository.findByJobNameAndStatusIn(
                    scheduleId.toString(), TaskStatus.getProcessingStatus());
            processingTask.forEach(task -> {
                taskRepository.updateStatusById(task.getId(), TaskStatus.CANCELED);
                log.info("Task status correction successful,scheduleTaskId={}", task.getId());
            });
        }
        // Ignore this schedule if scheduler has executing job.
        if (taskFrameworkEnabledProperties.isEnabled() && scheduleEntity.getJobType().executeInTaskFramework()
                && !scheduleEntity.getAllowConcurrent()
                && !taskRepository.findByJobNameAndStatusIn(scheduleId.toString(), TaskStatus.getProcessingStatus())
                        .isEmpty()) {
            log.warn("Concurrent is not allowed for scheduler {}.", scheduleId);
            return;
        }
        UserEntity userEntity = userService.nullSafeGet(scheduleEntity.getCreatorId());
        userEntity.setOrganizationId(scheduleEntity.getOrganizationId());
        User taskCreator = new User(userEntity);
        SecurityContextUtils.setCurrentUser(taskCreator);

        // Create or load task.
        Long targetTaskId = ScheduleTaskUtils.getTargetTaskId(context);
        ScheduleTaskEntity entity;
        if (Objects.isNull(targetTaskId)) {
            entity = new ScheduleTaskEntity();
            JobKey key = context.getJobDetail().getKey();
            log.info("Create new task from job,jobKey={}", key);
            entity.setJobName(key.getName());
            entity.setJobGroup(key.getGroup());
            if (key.getGroup().equals(JobType.DATA_ARCHIVE_DELETE.name())
                    || key.getGroup().equals(JobType.DATA_ARCHIVE_ROLLBACK.name())) {
                entity.setParametersJson(JsonUtils.toJson(context.getJobDetail().getJobDataMap()));
            } else {
                entity.setParametersJson(scheduleEntity.getJobParametersJson());
            }
            entity.setStatus(TaskStatus.PREPARING);
            entity.setFireTime(context.getFireTime());
            entity = taskRepository.save(entity);
        } else {
            log.info("Load an existing task,taskId={}", targetTaskId);
            entity = taskRepository.findById(targetTaskId).orElseThrow(() -> new NotFoundException(
                    ResourceType.ODC_SCHEDULE_TASK, "id", targetTaskId));
        }
        ScheduleTaskContextHolder.trace(scheduleEntity.getId(), entity.getJobGroup(), entity.getId());
        taskRepository.updateExecutor(entity.getId(), JsonUtils.toJson(new ExecutorInfo(hostProperties)));
        context.setResult(entity);
        log.info("Task is prepared,taskId={}", entity.getId());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        ScheduleTaskContextHolder.clear();
        Optional<ScheduleEntity> scheduleEntityOptional =
                scheduleRepository.findById(ScheduleTaskUtils.getScheduleId(context));
        if (scheduleEntityOptional.isPresent()) {
            ScheduleEntity scheduleEntity = scheduleEntityOptional.get();
            if (jobException != null && notificationProperties.isEnabled()) {
                try {
                    broker.enqueueEvent(eventBuilder.ofFailedTask(scheduleEntity));
                } catch (Exception e) {
                    log.warn("Failed to enqueue event.", e);
                }
            }
        }
    }
}
