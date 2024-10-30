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

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.service.task.executor.TaskResult;

import lombok.Getter;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
public class DefaultJobProcessUpdateEvent extends AbstractEvent {

    @Getter
    private final TaskResult taskResult;

    /**
     * Constructs a prototypical Event.
     *
     * @param taskResult The object on which the Event initially occurred.
     */
    public DefaultJobProcessUpdateEvent(TaskResult taskResult) {
        super(taskResult, "DefaultJobProcessUpdateEvent");
        this.taskResult = taskResult;
    }
}
