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
package com.oceanbase.odc.service.monitor.task;

import lombok.Getter;

@Getter
public final class TaskMonitorEvent {

    private final TaskLifecycle taskLifecycle;
    private final TaskMetricsContext context;

    public TaskMonitorEvent(TaskLifecycle taskLifecycle, TaskMetricsContext context) {
        this.taskLifecycle = taskLifecycle;
        this.context = context;
    }



    // todo
    public enum TaskLifecycle {
        TASK_CREATED,
        TASK_STARTED,
        TASK_COMPLETED,
        TASK_FAILED,
        TASK_CANCELLED,

        POD_CREATED_FAILED,
        POD_CREATED_SUCCESS,
        POD_DESTROY_FAILED,
        POD_DESTROY_SUCCESS,
    }
}
