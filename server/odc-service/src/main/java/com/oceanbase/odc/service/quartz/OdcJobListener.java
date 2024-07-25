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
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.schedule.LatestTaskMappingEntity;
import com.oceanbase.odc.metadb.schedule.LatestTaskMappingRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.flowtask.ScheduleTaskContextHolder;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
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
    private LatestTaskMappingRepository latestTaskMappingRepository;

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
        if (Objects.equals(context.getJobDetail().getKey().getGroup(),
                ScheduleType.ONLINE_SCHEMA_CHANGE_COMPLETE.name())) {
            return;
        }
        // Init user.
        Long scheduleId = ScheduleTaskUtils.getScheduleId(context);
        ScheduleEntity scheduleEntity =
                scheduleRepository.findById(scheduleId)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SCHEDULE, "id", scheduleId));
        ScheduleTaskContextHolder.trace(scheduleEntity.getId(), scheduleEntity.getType().name(), null);
        log.info("Job to be executed.OrganizationId={},ProjectId={},DatabaseId={},JobType={}",
                scheduleEntity.getOrganizationId(), scheduleEntity.getProjectId(), scheduleEntity.getDatabaseId(),
                scheduleEntity.getType());

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
            if (key.getGroup().equals(ScheduleTaskType.DATA_ARCHIVE_DELETE.name())
                    || key.getGroup().equals(ScheduleTaskType.DATA_ARCHIVE_ROLLBACK.name())) {
                entity.setParametersJson(JsonUtils.toJson(context.getJobDetail().getJobDataMap()));
            } else {
                entity.setParametersJson(scheduleEntity.getJobParametersJson());
            }
            entity.setStatus(TaskStatus.PREPARING);
            entity.setFireTime(context.getFireTime());
            entity = taskRepository.save(entity);
            updateLatestTaskId(scheduleId, entity.getId());
        } else {
            log.info("Load an existing task,taskId={}", targetTaskId);
            entity = taskRepository.findById(targetTaskId).orElseThrow(() -> new NotFoundException(
                    ResourceType.ODC_SCHEDULE_TASK, "id", targetTaskId));
            int affectRows =
                    taskRepository.updateStatusById(entity.getId(), TaskStatus.PREPARING,
                            TaskStatus.getRetryAllowedStatus());
            if (affectRows < 1) {
                throw new UnexpectedException("Concurrent is not allowed.");
            }
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
    }

    private void updateLatestTaskId(Long scheduleId, Long scheduleTaskId) {
        Optional<LatestTaskMappingEntity> optional = latestTaskMappingRepository.findByScheduleId(scheduleId);
        LatestTaskMappingEntity entity;
        if (optional.isPresent()) {
            entity = optional.get();
            // double check
            if (entity.getLatestScheduleTaskId() != null) {
                Optional<ScheduleTaskEntity> taskOptional = taskRepository.findById(entity.getLatestScheduleTaskId());
                log.info("Found latest task,scheduleId={},taskId={},status={}", scheduleId, scheduleTaskId,
                        taskOptional.isPresent() ? taskOptional.get().getStatus() : null);
                if (taskOptional.isPresent() && !taskOptional.get().getStatus().isTerminated()) {
                    throw new UnexpectedException("Concurrent is not allowed.");
                }
            }
        } else {
            entity = new LatestTaskMappingEntity();
            entity.setScheduleId(scheduleId);
        }
        log.info("Update latest task from {} to {}", entity.getLatestScheduleTaskId(), scheduleTaskId);
        entity.setLatestScheduleTaskId(scheduleTaskId);
        latestTaskMappingRepository.save(entity);
    }
}
