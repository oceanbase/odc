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

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.service.flow.instance.BaseFlowUserTaskInstance;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Listener for Event {@link UserTaskCreatedEvent}
 *
 * @author yh263208
 * @date 2022-02-17 12:20
 * @since ODC_release_3.3.0
 * @see AbstractEventListener
 */
@Slf4j
public class UserTaskCreatedListener extends AbstractEventListener<UserTaskCreatedEvent> {

    private final BaseFlowUserTaskInstance userTaskInstance;

    public UserTaskCreatedListener(@NonNull BaseFlowUserTaskInstance userTaskInstance) {
        this.userTaskInstance = userTaskInstance;
    }

    @Override
    public void onEvent(UserTaskCreatedEvent event) {
        DelegateTask delegateTask = event.getDelegateTask();
        if (delegateTask == null) {
            log.warn("DelegateTask is null, unknown error");
            return;
        }
        this.userTaskInstance.bindToUserTask(event.getApprovalInstance().getId(), delegateTask);
    }

}
