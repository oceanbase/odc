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

package com.oceanbase.odc.service.task.listener;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Component
@Slf4j
public class DefaultJobTerminateListener extends AbstractEventListener<JobTerminateEvent> {

    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Override
    public void onEvent(JobTerminateEvent event) {
        JobEntity jobEntity = taskFrameworkService.find(event.getJi().getId());
        // Trigger the data-delete job if necessary after the data-archive task is completed.
        if (jobEntity.getJobType().equals(JobType.DATA_ARCHIVE.name()) && event.getStatus() == JobStatus.DONE) {
            ScheduleService scheduleService = SpringContextUtil.getBean(ScheduleService.class);
            scheduleTaskService.findByJobId(jobEntity.getId()).ifPresent(o -> {
                DataArchiveParameters dataArchiveParameters = JsonUtils.fromJson(o.getParametersJson(),
                        DataArchiveParameters.class);
                if (dataArchiveParameters.isDeleteAfterMigration()) {
                    scheduleService.dataArchiveDelete(Long.parseLong(o.getJobName()), o.getId());
                }
            });
        }

        Optional<ScheduleTaskEntity> scheduleTask = scheduleTaskService.findByJobId(jobEntity.getId());
        if (scheduleTask.isPresent() && !scheduleTask.get().getStatus().isTerminated()) {
            int row = scheduleTaskRepository.updateStatusById(scheduleTask.get().getId(),
                    event.getStatus().convertTaskStatus());
            if (row >= 1) {
                log.info("Update scheduleTask successfully, scheduleTaskId={}, status={}.", jobEntity.getId(),
                        event.getStatus().convertTaskStatus());
            }
        } else {
            Optional<TaskEntity> taskEntity = taskRepository.findByJobId(jobEntity.getId());
            if (taskEntity.isPresent() && !taskEntity.get().getStatus().isTerminated()) {
                int row = taskRepository.updateStatusById(taskEntity.get().getId(),
                        event.getStatus().convertTaskStatus());
                if (row >= 1) {
                    log.info("Update taskTask successfully, taskId={}, status={}.", jobEntity.getId(),
                            event.getStatus().convertTaskStatus());
                }
            }
        }
    }
}
