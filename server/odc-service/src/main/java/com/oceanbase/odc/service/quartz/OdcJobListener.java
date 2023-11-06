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

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.SystemUtils;
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
import com.oceanbase.odc.service.notification.constant.EventLabelKeys;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
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
    private NotificationProperties notificationProperties;
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
        taskRepository.updateExecutor(entity.getId(), JsonUtils.toJson(new ExecutorInfo(hostProperties)));
        context.setResult(entity);
        log.info("Task is prepared,taskId={}", entity.getId());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (jobException != null && notificationProperties.isEnabled()) {
            try {
                JobDataMap dataMap = context.getMergedJobDataMap();
                EventLabels labels = new EventLabels();
                labels.put(EventLabelKeys.IDENTIFIER_KEY_TASK_TYPE, context.getJobInstance().getClass().getName());
                labels.put(EventLabelKeys.IDENTIFIER_KEY_ACTION, "failed");
                labels.put("taskInfo", JsonUtils.toJson(context.getMergedJobDataMap()));
                labels.put("errorMessage", jobException.getMessage());
                labels.put("region", SystemUtils.getEnvOrProperty("OB_ARN_PARTITION"));
                broker.enqueueEvent(Event.builder()
                        .status(EventStatus.CREATED)
                        .creatorId(dataMap.getLongFromString("creatorId"))
                        .organizationId(dataMap.getLongFromString("organizationId"))
                        .triggerTime(new Date(System.currentTimeMillis()))
                        .labels(labels)
                        .build());
            } catch (Exception e) {
                log.warn("Enqueue event failed, jobException:{}", jobException, e);
            }
        }
        JobKey key = context.getJobDetail().getKey();
        List<? extends Trigger> jobTriggers;
        try {
            jobTriggers = context.getScheduler().getTriggersOfJob(key);
        } catch (SchedulerException e) {
            log.warn("Get job triggers failed and don't update order status.JobKey={}", key);
            return;
        }
        for (Trigger trigger : jobTriggers) {
            if (trigger.getFinalFireTime() == null || trigger.getFinalFireTime().compareTo(context.getFireTime()) > 0) {
                return;
            }
        }
        ScheduleStatus status = jobException == null ? ScheduleStatus.COMPLETED : ScheduleStatus.EXECUTION_FAILED;
        log.info("The job is completed,jobKey={},status={}", key, status);
        scheduleRepository.updateStatusById(ScheduleTaskUtils.getScheduleId(context), status);
    }
}
