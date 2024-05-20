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

import static com.oceanbase.odc.service.flow.instance.FlowApprovalInstance.APPROVAL_VARIABLE_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class FlowTaskCallBackApprovalService {

    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private TaskService flowableTaskService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    public void approval(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus FlowNodeStatus,
            Map<String, Object> approvalVariables) {
        try {
            doApproval(flowInstanceId, flowTaskInstanceId, FlowNodeStatus, approvalVariables);
        } catch (Throwable e) {
            log.warn(
                    "approval task callback node  failed, flowInstanceId={}, flowTaskInstanceId={}, FlowNodeStatus={}, ex={}",
                    flowInstanceId, flowTaskInstanceId, FlowNodeStatus.name(), e);
        }
    }

    private void doApproval(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus FlowNodeStatus,
            Map<String, Object> approvalVariables) {
        completeTask(flowInstanceId, flowTaskInstanceId, FlowNodeStatus, approvalVariables);
    }

    private void completeTask(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus FlowNodeStatus,
            Map<String, Object> approvalVariables) {
        FlowInstanceEntity flowInstance = getFlowInstance(flowInstanceId);
        FlowableElement flowableElement = getFlowableElementOfUserTask(flowTaskInstanceId);

        Await.await().timeout(60).timeUnit(TimeUnit.SECONDS)
                .until(getFlowableTask(flowInstance, flowableElement.getName())::isPresent).build().start();
        Task task = getFlowableTask(flowInstance, flowableElement.getName()).get();
        doCompleteTask(flowInstanceId, flowTaskInstanceId, FlowNodeStatus, approvalVariables, task.getId());
    }

    private void doCompleteTask(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus FlowNodeStatus,
            Map<String, Object> approvalVariables, String taskId) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.putIfAbsent(APPROVAL_VARIABLE_NAME, FlowNodeStatus == FlowNodeStatus.COMPLETED);
            if (approvalVariables != null && !approvalVariables.isEmpty()) {
                variables.putAll(approvalVariables);
            }
            flowableTaskService.complete(taskId, variables);
            log.info("complete task succeed, flowInstanceId={}, flowTaskInstanceId={}, FlowNodeStatus={}.",
                    flowInstanceId, flowTaskInstanceId, FlowNodeStatus);
        } catch (Exception e) {
            log.warn("complete task failed, flowInstanceId={}, flowTaskInstanceId={}, FlowNodeStatus={}, ex={}.",
                    flowInstanceId, flowTaskInstanceId, FlowNodeStatus, e);
        }
    }


    private Optional<Task> getFlowableTask(FlowInstanceEntity flowInstance, String taskName) {

        List<Task> tasks = flowableTaskService.createTaskQuery().taskName(taskName)
                .processInstanceId(flowInstance.getProcessInstanceId())
                .processDefinitionId(flowInstance.getProcessDefinitionId())
                .list().stream().filter(a -> taskName != null && taskName.contains(RuntimeTaskConstants.CALLBACK_TASK))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tasks)) {
            log.info("Task not found, processInstanceId={}, processDefinitionId={}, taskName={}.",
                    flowInstance.getParentInstanceId(), flowInstance.getProcessDefinitionId(), taskName);
            return Optional.empty();
        }
        Verify.verify(tasks.size() == 1,
                "Expect callback flowable task size is 1, but size is " + tasks.size());
        return Optional.of(tasks.get(0));
    }

    private FlowableElement getFlowableElementOfUserTask(long flowTaskInstanceId) {
        List<FlowableElement> flowableElements =
                this.flowableAdaptor.getFlowableElementByType(flowTaskInstanceId, FlowNodeType.SERVICE_TASK,
                        FlowableElementType.USER_TASK).stream()
                        .filter(a -> a.getName() != null && a.getName()
                                .contains(RuntimeTaskConstants.CALLBACK_TASK))
                        .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(flowableElements)) {
            throw new IllegalStateException("No flowable element is found by id " + flowTaskInstanceId);
        }
        if (flowableElements.size() >= 2) {
            log.warn("Duplicate records are found, id={}, nodeType={}, coreType={} ", flowTaskInstanceId,
                    FlowNodeType.SERVICE_TASK, FlowableElementType.USER_TASK);
            throw new IllegalStateException("Duplicate records are found");
        }
        return flowableElements.get(0);
    }

    private FlowInstanceEntity getFlowInstance(long flowInstanceId) {
        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstanceId);
        PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "Id", flowInstanceId, optional::isPresent);
        return optional.get();
    }
}
