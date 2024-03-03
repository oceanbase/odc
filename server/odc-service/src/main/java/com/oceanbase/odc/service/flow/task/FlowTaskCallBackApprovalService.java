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

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

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

    public void approval(long flowInstanceId, long flowTaskInstanceId, TaskStatus taskStatus) {
        approval(flowInstanceId, flowTaskInstanceId, taskStatus, null);
    }

    public void approval(long flowInstanceId, long flowTaskInstanceId, TaskStatus taskStatus,
            Map<String, Object> approvalVariables) {

        if (!taskStatus.isTerminated()) {
            log.warn(
                    "Task is not terminated, callback failed, flowInstanceId={}, flowTaskInstanceId={}, taskStatus={}.",
                    flowInstanceId, flowTaskInstanceId, taskStatus);
            return;
        }
        FlowInstanceEntity flowInstance = getFlowInstance(flowInstanceId);
        FlowableElement flowableElement = getFlowableElementOfUserTask(flowTaskInstanceId);
        Task task = getFlowableTask(flowInstance, flowableElement.getName());
        Map<String, Object> variables = new HashMap<>();
        variables.putIfAbsent(APPROVAL_VARIABLE_NAME, taskStatus == TaskStatus.DONE);
        if (approvalVariables != null && !approvalVariables.isEmpty()) {
            variables.putAll(approvalVariables);
        }
        try {
            flowableTaskService.complete(task.getId(), variables);
        } catch (Exception e) {
            log.warn("Failed to approval flow instance, flowInstanceId={}, flowTaskInstanceId={},"
                    + " taskStatus={}, error={}",
                    flowInstanceId, flowTaskInstanceId, taskStatus, e.getMessage());
        }
    }

    private Task getFlowableTask(FlowInstanceEntity flowInstance, String taskName) {
        List<Task> tasks = flowableTaskService.createTaskQuery().taskName(taskName)
                .processInstanceId(flowInstance.getProcessInstanceId())
                .processDefinitionId(flowInstance.getProcessDefinitionId()).list();
        Verify.verify(CollectionUtils.isNotEmpty(tasks), "No callback flowable task is found by name " + taskName);
        Verify.verify(tasks.size() == 1,
                "Expect callback flowable task size is 1, but size is " + tasks.size());
        return tasks.get(0);
    }

    private FlowableElement getFlowableElementOfUserTask(long flowTaskInstanceId) {
        List<FlowableElement> flowableElements =
                this.flowableAdaptor.getFlowableElementByType(flowTaskInstanceId, FlowNodeType.SERVICE_TASK,
                        FlowableElementType.USER_TASK);
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
