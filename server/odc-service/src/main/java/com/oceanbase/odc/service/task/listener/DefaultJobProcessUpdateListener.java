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
import org.springframework.web.util.UriComponents;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.schedule.ScheduleTaskService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.StdTaskFrameworkService;

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
    private ScheduleTaskRepository taskRepository;
    @Autowired
    private ScheduleTaskService scheduleTaskService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private StdTaskFrameworkService stdTaskFrameworkService;

    @Override
    public void onEvent(DefaultJobProcessUpdateEvent event) {
        TaskResult taskResult = event.getTaskResult();
        JobIdentity identity = taskResult.getJobIdentity();
        JobEntity jobEntity = stdTaskFrameworkService.find(identity.getId());

        Optional<ScheduleTaskEntity> scheduleTaskEntityOptional = scheduleTaskService.findByJobId(jobEntity.getId());
        if (scheduleTaskEntityOptional.isPresent()) {
            updateScheduleTask(taskResult, scheduleTaskEntityOptional.get());
            return;
        }
        Optional<TaskEntity> taskEntityOptional = taskService.findByJobId(jobEntity.getId());
        taskEntityOptional.ifPresent(taskEntity -> updateTask(taskResult, taskEntity));
    }

    private void updateScheduleTask(TaskResult taskResult, ScheduleTaskEntity taskEntity) {
        taskEntity.setProgressPercentage(taskResult.getProgress());
        taskEntity.setStatus(taskResult.getStatus().convertTaskStatus());
        taskEntity.setResultJson(taskResult.getResultJson());
        if (taskResult.getExecutorEndpoint() != null) {
            UriComponents uc = UrlUtils.getUriComponents(taskResult.getExecutorEndpoint());
            ExecutorInfo executorInfo = new ExecutorInfo();
            executorInfo.setHost(uc.getHost());
            executorInfo.setPort(uc.getPort());
            taskEntity.setExecutor(JsonUtils.toJson(executorInfo));
        }
        taskRepository.update(taskEntity);
        log.debug("Update scheduleTask successfully, scheduleTaskId={}.", taskEntity.getId());
    }

    private void updateTask(TaskResult taskResult, TaskEntity taskEntity) {
        taskEntity.setProgressPercentage(taskResult.getProgress());
        taskEntity.setStatus(taskResult.getStatus().convertTaskStatus());
        taskEntity.setResultJson(taskResult.getResultJson());
        if (taskResult.getExecutorEndpoint() != null) {
            UriComponents uc = UrlUtils.getUriComponents(taskResult.getExecutorEndpoint());
            ExecutorInfo executorInfo = new ExecutorInfo();
            executorInfo.setHost(uc.getHost());
            executorInfo.setPort(uc.getPort());
            taskEntity.setExecutor(JsonUtils.toJson(executorInfo));
        }
        taskService.update(taskEntity);
        log.debug("Update taskTask successfully, taskId={}.", taskEntity.getId());

    }
}
