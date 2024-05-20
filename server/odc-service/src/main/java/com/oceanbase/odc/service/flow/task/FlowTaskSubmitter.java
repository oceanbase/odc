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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.flow.util.FlowConstants;
import com.oceanbase.odc.core.flow.util.FlowUtil;
import com.oceanbase.odc.core.flow.util.FlowableBoundaryEvent;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.flow.BeanInjectedClassDelegate;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
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

    @Qualifier("flowTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private FlowTaskCallBackApprovalService flowTaskCallBackApprovalService;

    @Override
    public void execute(DelegateExecution execution) {
        // DelegateExecution will be changed when current thread return,
        // so use execution facade class to save execution properties
        DelegateExecution executionFacade = new ExecutionEntityFacade(execution);
        String ids = execution.getProcessDefinitionId();
        String aId = execution.getCurrentActivityId();
        List<FlowableBoundaryEvent<ErrorEventDefinition>> defs =
                FlowUtil.getBoundaryEventDefinitions(ids, aId, ErrorEventDefinition.class);
        threadPoolTaskExecutor.submit(() -> {
            FlowTaskInstance flowTaskInstance = getFlowTaskInstance(executionFacade);
            try {
                getDelegateInstance(flowTaskInstance).execute(executionFacade);
                flowTaskCallBackApprovalService.approval(flowTaskInstance.getFlowInstanceId(), flowTaskInstance.getId(),
                        FlowNodeStatus.COMPLETED, FlowTaskResultContextHolder.getContext());
            } catch (Exception e) {
                Exception rootCause = (Exception) e.getCause();
                log.warn("Delegate task instance execute occur error.", rootCause);
                handleException(execution, flowTaskInstance, rootCause, defs);
            } finally {
                flowTaskInstance.dealloc();
                FlowTaskResultContextHolder.cleanContext();
            }

        });
    }

    private void handleException(DelegateExecution execution, FlowTaskInstance flowTaskInstance, Exception e,
            List<FlowableBoundaryEvent<ErrorEventDefinition>> defs) {
        String ids = execution.getProcessDefinitionId();
        String aId = execution.getCurrentActivityId();
        if (defs.isEmpty()) {
            log.warn("No error boundary is defined to handle events, processInstanceId={}, activityId={}",
                    ids, aId);
        } else if (e instanceof BaseFlowException) {
            BaseFlowException flowException = (BaseFlowException) e;
            String targetErrorCode = flowException.getErrorCode().code();
            for (FlowableBoundaryEvent eventDefinition : defs) {
                ErrorEventDefinition eed = (ErrorEventDefinition) eventDefinition.getEventDefinition();
                String acceptErrorCode = eed.getErrorCode();
                if (Objects.equals(acceptErrorCode, targetErrorCode)) {
                    flowTaskCallBackApprovalService.approval(flowTaskInstance.getFlowInstanceId(),
                            flowTaskInstance.getId(), FlowNodeStatus.FAILED, FlowTaskResultContextHolder.getContext());
                    if (CollectionUtils.isNotEmpty(eventDefinition.getFlowableListeners())) {
                        callListener(execution, eventDefinition.getFlowableListeners());
                    }
                    return;
                }
            }
            log.warn("Exception has no error boundary event, pId={}, activityId={}, acceptErrorCodes={}",
                    ids, aId,
                    defs.stream().map(a -> a.getEventDefinition().getErrorCode()).collect(Collectors.toList()));
        } else {
            log.warn("Exception has to be an instance of FlowException, pId={}, activityId={}, acceptErrorCodes={}",
                    ids, aId,
                    defs.stream().map(a -> a.getEventDefinition().getErrorCode()).collect(Collectors.toList()));
        }
        flowTaskCallBackApprovalService.approval(flowTaskInstance.getFlowInstanceId(),
                flowTaskInstance.getId(), FlowNodeStatus.COMPLETED,
                FlowTaskResultContextHolder.getContext());
    }

    private void callListener(DelegateExecution execution, List<FlowableListener> flowableListeners) {
        flowableListeners.forEach(fl -> {
            try {
                Class<?> clazz = Class.forName(fl.getImplementation(), false,
                        Thread.currentThread().getContextClassLoader());
                if (ExecutionListener.class.isAssignableFrom(clazz) &&
                        Objects.equals(fl.getEvent(), FlowConstants.EXECUTION_END_EVENT_NAME)) {
                    ExecutionListener listener =
                            (ExecutionListener) BeanInjectedClassDelegate.instantiateDelegate(clazz);
                    execution.setEventName(fl.getEvent());
                    listener.notify(execution);
                }
            } catch (Exception ex) {
                log.warn("Call execution listener occur error: ", ex);
            }
        });
    }

    private BaseRuntimeFlowableDelegate<?> getDelegateInstance(FlowTaskInstance flowTaskInstance) throws Exception {
        OdcRuntimeDelegateMapper mapper = new OdcRuntimeDelegateMapper();
        Class<? extends BaseRuntimeFlowableDelegate<?>> delegateClass =
                mapper.map(flowTaskInstance.getTaskType());
        return BeanInjectedClassDelegate.instantiateDelegate(delegateClass);
    }

    private FlowTaskInstance getFlowTaskInstance(DelegateExecution execution) {
        Optional<FlowTaskInstance> flowTaskInstance = flowableAdaptor.getTaskInstanceByActivityId(
                execution.getCurrentActivityId(), FlowTaskUtil.getFlowInstanceId(execution));
        PreConditions.validExists(ResourceType.ODC_FLOW_TASK_INSTANCE, "activityId",
                execution.getCurrentActivityId(), flowTaskInstance::isPresent);
        return flowTaskInstance.get();
    }

}
