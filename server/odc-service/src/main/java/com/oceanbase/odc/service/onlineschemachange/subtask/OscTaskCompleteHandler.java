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
package com.oceanbase.odc.service.onlineschemachange.subtask;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskSpecs;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeTaskHandler;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.ProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.openapi.ProjectOpenApiService;
import com.oceanbase.odc.service.onlineschemachange.oms.request.ProjectControlRequest;
import com.oceanbase.odc.service.onlineschemachange.oms.response.ProjectProgressResponse;
import com.oceanbase.odc.service.onlineschemachange.pipeline.OscValveContext;
import com.oceanbase.odc.service.quartz.QuartzJobService;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OscTaskCompleteHandler {
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private QuartzJobService quartzJobService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ProjectOpenApiService projectOpenApiService;
    @Autowired
    private OnlineSchemaChangeTaskHandler taskHandler;

    public void proceed(OscValveContext valveContext, TaskStatus status, boolean isContinue) {
        try {
            releaseOmsResource(valveContext.getTaskParameter());
            updateScheduleTask(valveContext.getScheduleTask().getId(), status);
            if (isContinue) {
                scheduleNextTask(valveContext.getSchedule().getId(), valveContext.getScheduleTask().getId());
            }
        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "Failed to proceed, schedule id {0}", valveContext.getSchedule().getId()),
                    e);
        }
    }

    private void scheduleNextTask(Long scheduleId, Long currentScheduleId) {
        Specification<ScheduleTaskEntity> specification = Specification
                .where(ScheduleTaskSpecs.jobNameEquals(scheduleId + ""));
        List<ScheduleTaskEntity> tasks = scheduleTaskRepository.findAll(specification, Sort.by("id"));
        Optional<ScheduleTaskEntity> nextTask = tasks.stream().filter(
                taskEntity -> taskEntity.getStatus() == TaskStatus.PREPARING).findFirst();

        if (!nextTask.isPresent()) {
            log.info("No preparing status schedule task for next schedule, schedule id {}",
                    scheduleId);
            return;
        }
        Long nextTaskId = nextTask.get().getId();
        try {
            taskHandler.start(scheduleId, nextTaskId);
            log.info("Successfully start next schedule task with id {}", nextTaskId);
        } catch (Exception e) {
            log.warn(
                    MessageFormat.format(
                            "Failed to schedule next, schedule id {0}, "
                                    + "current schedule task Id {1}, next schedule task {2}",
                            scheduleId, currentScheduleId, nextTaskId),
                    e);
        }

    }

    public void deleteQuartzJob(Long scheduleId, JobType jobType) {
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(scheduleId, jobType);
        try {
            quartzJobService.deleteJob(jobKey);
            log.info("Successfully delete job with jobKey {}", jobKey);
        } catch (SchedulerException e) {
            log.warn("Delete job occur error with jobKey {}", jobKey, e);
        }
    }

    public void releaseOmsResource(OnlineSchemaChangeScheduleTaskParameters taskParameters) {
        if (taskParameters.getOmsProjectId() == null) {
            return;
        }
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                ProjectControlRequest request = new ProjectControlRequest();
                request.setId(taskParameters.getOmsProjectId());
                request.setUid(taskParameters.getUid());

                ProjectProgressResponse response = projectOpenApiService.describeProjectProgress(request);
                if (response.getStatus() == ProjectStatusEnum.RUNNING) {
                    projectOpenApiService.stopProject(request);
                }
                projectOpenApiService.releaseProject(request);
                log.info("Release oms project, id {}", taskParameters.getOmsProjectId());
            } catch (Throwable ex) {
                log.warn("Failed to release oms project, id {}, occur error {}",
                        taskParameters.getOmsProjectId(), ex.getMessage());
            }
        });

    }

    public void updateScheduleTask(Long scheduleTaskId, TaskStatus status) {
        if (TaskStatus.DONE == status) {
            scheduleTaskRepository.updateStatusAndProcessPercentageById(scheduleTaskId, status, 100D);
        } else {
            scheduleTaskRepository.updateStatusById(scheduleTaskId, status);
        }
        log.info("Successfully update schedule task id {} set status {}", scheduleTaskId, status);
    }
}
