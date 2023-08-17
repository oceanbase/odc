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

import org.flowable.task.service.delegate.DelegateTask;

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;

import lombok.Getter;
import lombok.NonNull;

/**
 * This event is fired when the user task is assigned
 *
 * @author yh263208
 * @date 2022-02-17 12:16
 * @since ODC_release_3.3.0
 * @see AbstractEvent
 */
public class UserTaskCreatedEvent extends AbstractEvent {

    @Getter
    private final FlowApprovalInstance approvalInstance;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public UserTaskCreatedEvent(Object source, @NonNull FlowApprovalInstance approvalInstance) {
        super(source, EventNames.USER_TASK_CREATED);
        if (!(source instanceof DelegateTask)) {
            throw new IllegalArgumentException("Event Source is illegal");
        }
        this.approvalInstance = approvalInstance;
    }

    public DelegateTask getDelegateTask() {
        return (DelegateTask) getSource();
    }

}
