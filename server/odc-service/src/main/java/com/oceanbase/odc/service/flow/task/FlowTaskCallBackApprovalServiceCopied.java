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
import java.util.stream.Collectors;

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
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.task.util.FlowApprovalUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class FlowTaskCallBackApprovalServiceCopied {

    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private TaskService flowableTaskService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    public void approval(long flowInstanceId, String serviceTaskActivityId, Map<String, Object> approvalVariables,
            Throwable exception) {
        FlowInstanceEntity flowInstance = getFlowInstance(flowInstanceId);
        boolean passed = FlowApprovalUtil.isApprovalPassed(flowInstance.getProcessDefinitionId(),
                serviceTaskActivityId, exception);
        doApproval(flowInstanceId, serviceTaskActivityId, passed, approvalVariables);
    }

    private void doApproval(long flowInstanceId, String serviceTaskActivityId, boolean approved,
            Map<String, Object> approvalVariables) {

        FlowInstanceEntity flowInstance = getFlowInstance(flowInstanceId);
        FlowableElement flowableElement = getFlowableElementOfCallBackTask(flowInstanceId, serviceTaskActivityId);
        Task task = getFlowableTask(flowInstance.getProcessInstanceId(), flowableElement.getName());

        Map<String, Object> variables = new HashMap<>();
        variables.putIfAbsent(APPROVAL_VARIABLE_NAME, approved);
        if (approvalVariables != null) {
            variables.putAll(approvalVariables);
        }
        flowableTaskService.complete(task.getId(), variables);
    }

    private Task getFlowableTask(String processInstanceId, String taskName) {
        List<Task> tasks = flowableTaskService.createTaskQuery().taskName(taskName)
                .processInstanceId(processInstanceId).list();
        Verify.verify(CollectionUtils.isNotEmpty(tasks), "No callback flowable task is found by name " + taskName);
        Verify.verify(tasks.size() == 1,
                "Expect callback flowable task size is 1, but size is " + tasks.size());
        return tasks.get(0);
    }

    private FlowableElement getFlowableElementOfCallBackTask(long flowInstanceId, String serviceTaskActivityId) {
        Optional<FlowTaskInstance> flowTaskInstance =
                this.flowableAdaptor.getTaskInstanceByActivityId(serviceTaskActivityId, flowInstanceId);

        long flowTaskInstanceId = flowTaskInstance.get().getId();
        List<FlowableElement> flowableElements =
                this.flowableAdaptor.getFlowableElementByType(flowTaskInstanceId, FlowNodeType.SERVICE_TASK,
                        FlowableElementType.USER_TASK).stream()
                        .filter(a -> a.getName() != null && a.getName().contains("callback")).collect(
                                Collectors.toList());
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
