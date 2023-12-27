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

import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.executor.task.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
public class TaskResultProgressUploadListener extends TaskResultUploadListener {

    private final TaskService taskService;
    private final Long jobId;
    private final Long taskTaskId;

    public TaskResultProgressUploadListener(TaskService taskService, @NonNull Long jobId, @NonNull Long taskTaskId) {
        this.taskService = taskService;
        this.jobId = jobId;
        this.taskTaskId = taskTaskId;
    }

    @Override
    public void onEvent(TaskResultUploadEvent event) {
        TaskResult taskResult = event.getTaskResult();
        JobIdentity identity = taskResult.getJobIdentity();
        if (identity.getId() != jobId) {
            return;
        }
        TaskEntity taskEntity = taskService.detail(taskTaskId);
        taskEntity.setProgressPercentage(taskResult.getProgress() * 100);
        taskEntity.setStatus(taskResult.getStatus().convertTaskStatus());
        taskEntity.setResultJson(taskResult.getResultJson());
        taskService.update(taskEntity);
    }
}
