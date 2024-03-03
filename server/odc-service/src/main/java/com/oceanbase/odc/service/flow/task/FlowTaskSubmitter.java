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
package com.oceanbase.odc.service.flow.task;

import java.util.Optional;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.task.mapper.OdcRuntimeDelegateMapper;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@Slf4j
@Component
public class FlowTaskSubmitter implements JavaDelegate {

    @Autowired
    private FlowTaskBeanFactory flowableTaskBeanFactory;
    @Qualifier("flowTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private FlowableAdaptor flowableAdaptor;

    @Override
    public void execute(DelegateExecution execution) {
        // DelegateExecution will be changed when current thread return,
        // so use execution facade class to save execution properties
        DelegateExecution executionFacade = new ExecutionEntityFacade(execution);
        threadPoolTaskExecutor.submit(() -> {
            try {
                getaDelegateInstance(executionFacade).execute(executionFacade);
            } catch (Throwable e) {
                log.warn("Delegate task instance execute occur error.", e);
            }
        });
    }

    private BaseRuntimeFlowableDelegate<?> getaDelegateInstance(DelegateExecution execution) throws Exception {
        Optional<FlowTaskInstance> flowTaskInstance = flowableAdaptor.getTaskInstanceByActivityId(
                execution.getCurrentActivityId(), FlowTaskUtil.getFlowInstanceId(execution));
        PreConditions.validExists(ResourceType.ODC_FLOW_TASK_INSTANCE, "activityId",
                execution.getCurrentActivityId(), flowTaskInstance::isPresent);

        OdcRuntimeDelegateMapper mapper = new OdcRuntimeDelegateMapper();
        Class<? extends BaseRuntimeFlowableDelegate<?>> delegateClass =
                mapper.map(flowTaskInstance.get().getTaskType());
        flowTaskInstance.get().dealloc();
        return flowableTaskBeanFactory.createBeanWithDependencies(delegateClass);
    }

}
