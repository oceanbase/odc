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
 * Event listener, used to listen to the execution event of the service task
 *
 * @author yh263208
 * @date 2022-02-14 14:43
 * @since ODC_release_3.3.0
 */
@Slf4j
public class ServiceTaskStartedListener extends AbstractEventListener<ServiceTaskStartedEvent> {

    private final FlowTaskInstance target;

    public ServiceTaskStartedListener(@NonNull FlowTaskInstance target) {
        this.target = target;
    }

    @Override
    public void onEvent(ServiceTaskStartedEvent event) {
        BaseRuntimeFlowableDelegate<?> runtimeServiceTask = event.getServiceTaskImpl();
        if (runtimeServiceTask == null) {
            log.warn("BaseRuntimeServiceTask is null, unknown error");
            return;
        }
        this.target.bindServiceTask(runtimeServiceTask);
    }
}
