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
package com.oceanbase.odc.service.flow.listener;

import java.util.Optional;

import org.flowable.task.service.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.flow.BaseTaskListener;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.event.UserTaskCreatedEvent;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;

import lombok.extern.slf4j.Slf4j;

/**
 * This {@link BaseTaskBindUserTaskListener} is used to monitor the creation and execution of
 * {@code Flowable} approval {@link org.flowable.task.api.Task}, and associate the approval task
 * with the {@link FlowApprovalInstance} when the task is
 * assigned({@link BaseTaskBindUserTaskListener#onTaskAssigned(DelegateTask)}).
 *
 * @author yh263208
 * @date 2022-02-16 17:57
 * @since ODC_release_3.3.0
 * @see BaseTaskListener
 */
@Slf4j
public class BaseTaskBindUserTaskListener extends BaseTaskListener {

    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;

    @Override
    protected void onTaskCreated(DelegateTask delegateTask) {
        try {
            Long flowInstanceId = getFlowInstanceId(delegateTask);
            FlowApprovalInstance approvalInstance = getApprovalInstance(flowInstanceId, delegateTask);
            Long approvalInstanceId = approvalInstance.getId();
            String userTaskId = delegateTask.getId();
            userTaskInstanceRepository.updateUserTaskIdById(approvalInstanceId, userTaskId);
            approvalInstance.setUserTaskId(userTaskId);
            eventPublisher.publishEvent(new UserTaskCreatedEvent(delegateTask, approvalInstance));
            log.info("Approval task starts execution, taskId={}, taskName={}, approvalInstanceId={}",
                    delegateTask.getId(), delegateTask.getName(), approvalInstanceId);
        } catch (Exception e) {
            log.warn("Failed to bind the approval task id to the approval instance node, taskId={}, taskName={}",
                    delegateTask.getId(), delegateTask.getName(), e);
            throw new IllegalStateException("Failed to update approval instance", e);
        }
    }

    @Override
    protected void onTaskCompleted(DelegateTask delegateTask) {}

    @Override
    protected void onTaskDeleted(DelegateTask delegateTask) {}

    @Override
    protected void onTaskAssigned(DelegateTask delegateTask) {}

    private Long getFlowInstanceId(DelegateTask delegateTask) {
        String pdId = delegateTask.getProcessDefinitionId();
        Optional<Long> optional = flowableAdaptor.getFlowInstanceIdByProcessDefinitionId(pdId);
        if (!optional.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_FLOW_INSTANCE, "processDefinitionId", pdId);
        }
        return optional.get();
    }

    private FlowApprovalInstance getApprovalInstance(Long flowInstanceId, DelegateTask delegateTask) {
        String name = delegateTask.getName();
        Optional<FlowApprovalInstance> optional = flowableAdaptor.getApprovalInstanceByName(name, flowInstanceId);
        if (!optional.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_FLOW_APPROVAL_INSTANCE, "name", name);
        }
        FlowApprovalInstance approvalInstance = optional.get();
        approvalInstance.dealloc();
        return approvalInstance;
    }

}
