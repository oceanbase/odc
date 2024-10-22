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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Component
@Slf4j
public class DefaultJobProcessUpdateListener extends AbstractEventListener<DefaultJobProcessUpdateEvent> {

    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private TaskFrameworkService stdTaskFrameworkService;

    @Override
    public void onEvent(DefaultJobProcessUpdateEvent event) {
        TaskResult taskResult = event.getTaskResult();
        JobIdentity identity = taskResult.getJobIdentity();
        JobEntity jobEntity = stdTaskFrameworkService.find(identity.getId());
        scheduleTaskService.findByJobId(jobEntity.getId())
                .ifPresent(taskEntity -> updateScheduleTaskStatus(taskEntity.getId(),
                        taskResult.getStatus()));
    }

    private void updateScheduleTaskStatus(Long id, TaskStatus status) {
        scheduleTaskService.updateStatusById(id, status);
        log.debug("Update scheduleTask status to {} successfully, scheduleTaskId={}", status, id);
    }
}
