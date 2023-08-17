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
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;

/**
 * Task instance instantiation event, called when a task instance is created
 *
 * @author yh263208
 * @date 2022-02-14 14:36
 * @since ODC_release_3.3.0
 */
public class TaskInstanceCreatedEvent extends AbstractEvent {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public TaskInstanceCreatedEvent(Object source) {
        super(source, EventNames.TASK_INSTANCE_CREATED);
        if (!(source instanceof FlowTaskInstance)) {
            throw new IllegalArgumentException("Event Source is illegal");
        }
    }

    public FlowTaskInstance getTaskInstance() {
        return (FlowTaskInstance) getSource();
    }

}
