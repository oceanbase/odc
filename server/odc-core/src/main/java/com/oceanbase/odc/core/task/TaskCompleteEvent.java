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
package com.oceanbase.odc.core.task;

import com.oceanbase.odc.common.event.AbstractEvent;

import lombok.Getter;
import lombok.NonNull;

/**
 * {@link TaskCompleteEvent}
 *
 * @author yh263208
 * @date 2022-10-09 21:40
 * @since ODC_release_3.5.0
 */
class TaskCompleteEvent extends AbstractEvent {

    @Getter
    private final Exception exception;

    /**
     * Constructs a prototypical Event.
     *
     * @param delegateTask The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public TaskCompleteEvent(BaseDelegateTask delegateTask) {
        super(delegateTask, "TaskCompleteEvent");
        this.exception = null;
    }

    public TaskCompleteEvent(BaseDelegateTask delegateTask, @NonNull Exception exception) {
        super(delegateTask, "TaskCompleteEvent");
        this.exception = exception;
    }

}

