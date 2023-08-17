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
package com.oceanbase.odc.service.flow.event;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Event listener, used to listen to the creation event of the task instance
 *
 * @author yh263208
 * @date 2022-02-14 14:43
 * @since ODC_release_3.3.0
 */
@Slf4j
public class TaskInstanceCreatedListener extends AbstractEventListener<TaskInstanceCreatedEvent> {

    private final BaseRuntimeFlowableDelegate<?> serviceTask;

    public TaskInstanceCreatedListener(@NonNull BaseRuntimeFlowableDelegate<?> serviceTask) {
        this.serviceTask = serviceTask;
    }

    @Override
    public void onEvent(TaskInstanceCreatedEvent event) {
        FlowTaskInstance taskInstance = event.getTaskInstance();
        if (taskInstance == null) {
            log.warn("Task instance is null, unknown error");
            return;
        }
        this.serviceTask.bindToFlowTaskInstance(taskInstance);
    }

}
