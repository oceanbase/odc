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

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;

import lombok.Getter;
import lombok.NonNull;

/**
 * This event is fired when a user task is executed
 *
 * @author yh263208
 * @date 2022-2-14 16:11
 * @since ODC_release_3.3.0
 * @see AbstractEvent
 */
public class ServiceTaskStartedEvent extends AbstractEvent {
    @Getter
    private final Thread currentThread;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ServiceTaskStartedEvent(Object source, @NonNull Thread currentThread) {
        super(source, EventNames.TASK_STARTED);
        if (!(source instanceof BaseRuntimeFlowableDelegate)) {
            throw new IllegalArgumentException("Event Source is illegal");
        }
        this.currentThread = currentThread;
    }

    public BaseRuntimeFlowableDelegate<?> getServiceTaskImpl() {
        return (BaseRuntimeFlowableDelegate<?>) getSource();
    }

}
