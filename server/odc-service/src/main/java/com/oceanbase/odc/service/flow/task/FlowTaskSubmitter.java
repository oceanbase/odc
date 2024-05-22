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

import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.flow.util.FlowConstants;
import com.oceanbase.odc.core.flow.util.FlowUtil;
import com.oceanbase.odc.core.flow.util.FlowableBoundaryEvent;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
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
public class FlowTaskSubmitter implements JavaDelegate {

    @Qualifier("flowTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private FlowTaskCallBackApprovalService flowTaskCallBackApprovalService;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;

    @Override
    public void execute(DelegateExecution execution) {
        // DelegateExecution will be changed when current thread return,
        // so use execution facade class to save execution properties
        CustomDelegateExecution executionFacade = new CustomDelegateExecution(execution);
        long flowInstanceId = FlowTaskUtil.getFlowInstanceId(executionFacade);
        String activityId = executionFacade.getCurrentActivityId();
        List<FlowableBoundaryEvent<ErrorEventDefinition>> defs = FlowUtil.getBoundaryEventDefinitions(
                execution.getProcessDefinitionId(), activityId, ErrorEventDefinition.class);
        threadPoolTaskExecutor.submit(() -> {
            FlowTaskInstance flowTaskInstance = null;
            try {
                flowTaskInstance = getFlowTaskInstance(flowInstanceId, activityId);
                getDelegateInstance(flowTaskInstance).execute(executionFacade);
                updateFlowTaskInstance(flowTaskInstance.getId(), FlowNodeStatus.COMPLETED);
                flowTaskCallBackApprovalService.approval(executionFacade.getProcessInstanceId(),
                        flowTaskInstance.getId(), executionFacade.getFutureVariable());
            } catch (Exception e) {
                log.warn("Delegate task instance execute occur error: ", e);
                updateFlowTaskInstance(flowTaskInstance.getId(), FlowNodeStatus.FAILED);
                Exception rootCause = (Exception) e.getCause();
                handleException(executionFacade, flowTaskInstance, rootCause, defs);
            } finally {
                if (flowTaskInstance != null) {
                    flowTaskInstance.dealloc();
                }
            }
        });
    }

    private void updateFlowTaskInstance(long flowTaskInstanceId, FlowNodeStatus flowNodeStatus) {
        try {
            int affectRows = serviceTaskRepository.updateStatusById(flowTaskInstanceId, flowNodeStatus);
            log.info("Modify node instance status successfully, instanceId={}, flowNodeStatus={}, affectRows={}",
                    flowTaskInstanceId, flowNodeStatus, affectRows);
        } catch (Exception ex) {
            log.warn("Modify node instance status occur error, instanceId={}", flowTaskInstanceId, ex);
        }
    }

    private void handleException(CustomDelegateExecution execution, FlowTaskInstance flowTaskInstance,
            Exception e, List<FlowableBoundaryEvent<ErrorEventDefinition>> defs) {
        String processDefinitionId = execution.getProcessDefinitionId();
        String activityId = execution.getCurrentActivityId();
        if (defs.isEmpty()) {
            log.warn("No error boundary is defined to handle events, processInstanceId={}, activityId={}",
                    processDefinitionId, activityId);
        } else if (e instanceof BaseFlowException) {
            BaseFlowException flowException = (BaseFlowException) e;
            String targetErrorCode = flowException.getErrorCode().code();
            for (FlowableBoundaryEvent<ErrorEventDefinition> eventDefinition : defs) {
                ErrorEventDefinition eed = eventDefinition.getEventDefinition();
                String acceptErrorCode = eed.getErrorCode();
                if (Objects.equals(acceptErrorCode, targetErrorCode)) {
                    flowTaskCallBackApprovalService.reject(execution.getProcessInstanceId(),
                            flowTaskInstance.getId(), execution.getFutureVariable());
                    if (CollectionUtils.isNotEmpty(eventDefinition.getFlowableListeners())) {
                        callListener(FlowConstants.EXECUTION_START_EVENT_NAME, execution,
                                eventDefinition.getFlowableListeners());
                        callListener(FlowConstants.EXECUTION_END_EVENT_NAME, execution,
                                eventDefinition.getFlowableListeners());
                    }
                    return;
                }
            }
            log.warn("Exception has no error boundary event, pId={}, activityId={}, acceptErrorCodes={}",
                    processDefinitionId, activityId,
                    defs.stream().map(a -> a.getEventDefinition().getErrorCode()).collect(Collectors.toList()));
        } else {
            log.warn("Exception has to be an instance of FlowException, pId={}, activityId={}, acceptErrorCodes={}",
                    processDefinitionId, activityId,
                    defs.stream().map(a -> a.getEventDefinition().getErrorCode()).collect(Collectors.toList()));
        }
        flowTaskCallBackApprovalService.approval(execution.getProcessInstanceId(),
                flowTaskInstance.getId(), execution.getFutureVariable());
    }

    private void callListener(String eventName, DelegateExecution execution, List<FlowableListener> flowableListeners) {
        flowableListeners.stream().filter(fl -> eventName.equals(fl.getEvent())).forEach(fl -> {
            try {
                Class<?> clazz = Class.forName(fl.getImplementation(), false,
                        Thread.currentThread().getContextClassLoader());
                if (ExecutionListener.class.isAssignableFrom(clazz)) {
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

    private FlowTaskInstance getFlowTaskInstance(long flowInstanceId, String activityId) {
        Optional<FlowTaskInstance> flowTaskInstance =
                flowableAdaptor.getTaskInstanceByActivityId(activityId, flowInstanceId);
        return flowTaskInstance.orElseThrow(
                () -> new NotFoundException(ResourceType.ODC_FLOW_TASK_INSTANCE, "activityId", activityId));
    }

}
