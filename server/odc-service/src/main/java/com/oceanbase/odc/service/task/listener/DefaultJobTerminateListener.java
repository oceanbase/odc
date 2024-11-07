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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.schedule.alarm.ScheduleAlarmUtils;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.task.processor.terminate.TerminateProcessor;
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
    private List<TerminateProcessor> terminateProcessors;

    @Override
    public void onEvent(JobTerminateEvent event) {
        JobEntity jobEntity = taskFrameworkService.find(event.getJi().getId());
        scheduleTaskService.findByJobId(jobEntity.getId()).ifPresent(scheduleTask -> {
            // correct status
            TaskStatus taskStatus = TerminateProcessor.correctTaskStatus(terminateProcessors, jobEntity.getJobType(),
                    scheduleTask, event.getStatus().convertTaskStatus());
            // correct current local variable to right status
            scheduleTask.setStatus(taskStatus);
            scheduleTaskService.updateStatusById(scheduleTask.getId(), taskStatus);
            log.info("Update schedule task status to {} succeed,scheduleTaskId={}", taskStatus, scheduleTask.getId());
            // Refresh the schedule status after the task is completed.
            scheduleService.refreshScheduleStatus(Long.parseLong(scheduleTask.getJobName()));
            // Trigger the alarm if the task is failed or canceled.
            if (taskStatus == TaskStatus.FAILED) {
                ScheduleAlarmUtils.fail(scheduleTask.getId());
            }
            if (taskStatus == TaskStatus.CANCELED) {
                ScheduleAlarmUtils.timeout(scheduleTask.getId());
            }
            // invoke task related processor
            doProcessor(jobEntity, scheduleTask);
        });
    }

    private void doProcessor(JobEntity jobEntity, ScheduleTask scheduleTask) {
        for (TerminateProcessor processor : terminateProcessors) {
            if (processor.interested(jobEntity.getJobType())) {
                processor.process(scheduleTask, jobEntity);
            }
        }
    }
}
