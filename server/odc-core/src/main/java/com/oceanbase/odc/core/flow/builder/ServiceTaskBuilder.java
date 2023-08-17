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
package com.oceanbase.odc.core.flow.builder;

import org.flowable.bpmn.model.ServiceTask;

import com.oceanbase.odc.core.flow.BaseFlowableDelegate;

import lombok.NonNull;

/**
 * Refer to {@link ServiceTask}
 *
 * @author yh263208
 * @date 2022-01-19 17:20
 * @since ODC_release_3.3.0
 * @see BaseProcessNodeBuilder
 */
public class ServiceTaskBuilder extends BaseTaskBuilder<ServiceTask> {

    private final String implClassName;

    public ServiceTaskBuilder(@NonNull String name, @NonNull Class<? extends BaseFlowableDelegate> clazz) {
        super(name);
        this.implClassName = clazz.getName();
    }

    @Override
    public ServiceTask build() {
        ServiceTask serviceTask = new ServiceTask();
        init(serviceTask);
        return serviceTask;
    }

    @Override
    protected void init(@NonNull ServiceTask serviceTask) {
        super.init(serviceTask);
        serviceTask.setImplementation(implClassName);
        serviceTask.setImplementationType("class");
    }

}
