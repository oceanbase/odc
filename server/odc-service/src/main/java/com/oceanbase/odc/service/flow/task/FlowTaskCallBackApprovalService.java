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
import com.oceanbase.odc.core.shared.Verify;
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

    public void approval(String processInstanceId, long flowTaskInstanceId, Map<String, Object> variables) {
        try {
            completeTask(processInstanceId, flowTaskInstanceId, FlowNodeStatus.COMPLETED, variables);
        } catch (Exception e) {
            log.warn("Failed to approval callback node, processInstanceId={}, flowTaskInstanceId={}.",
                    processInstanceId, flowTaskInstanceId, e);
        }
    }

    public void reject(String processInstanceId, long flowTaskInstanceId, Map<String, Object> variables) {
        try {
            completeTask(processInstanceId, flowTaskInstanceId, FlowNodeStatus.FAILED, variables);
        } catch (Exception e) {
            log.warn("Failed to reject callback node, processInstanceId={}, flowTaskInstanceId={}.",
                    processInstanceId, flowTaskInstanceId, e);
        }
    }

    private void completeTask(String processInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus,
            Map<String, Object> variables) {
        FlowableElement flowableElement = getFlowableElementOfUserTask(flowTaskInstanceId);

        Await.await().timeout(60).timeUnit(TimeUnit.SECONDS).period(1).periodTimeUnit(TimeUnit.SECONDS)
                .until(getFlowableTask(processInstanceId, flowableElement.getName())::isPresent).build().start();
        Task task = getFlowableTask(processInstanceId, flowableElement.getName()).get();
        doCompleteTask(processInstanceId, flowTaskInstanceId, flowNodeStatus, variables, task.getId());
    }

    private void doCompleteTask(String processInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus,
            Map<String, Object> variables, String taskId) {
        try {
            Map<String, Object> approveVariables = new HashMap<>();
            approveVariables.putIfAbsent(APPROVAL_VARIABLE_NAME, flowNodeStatus == FlowNodeStatus.COMPLETED);
            if (variables != null && !variables.isEmpty()) {
                approveVariables.putAll(variables);
            }
            flowableTaskService.complete(taskId, approveVariables);
            log.info("complete task succeed, processInstanceId={}, flowTaskInstanceId={}, flowNodeStatus={}.",
                    processInstanceId, flowTaskInstanceId, flowNodeStatus);
        } catch (Exception e) {
            log.warn("complete task failed, processInstanceId={}, flowTaskInstanceId={}, flowNodeStatus={}.",
                    processInstanceId, flowTaskInstanceId, flowNodeStatus, e);
        }
    }


    private Optional<Task> getFlowableTask(String processInstanceId, String taskName) {
        List<Task> tasks = flowableTaskService.createTaskQuery().taskName(taskName)
                .processInstanceId(processInstanceId)
                .list().stream().filter(a -> taskName != null && taskName.contains(RuntimeTaskConstants.CALLBACK_TASK))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(tasks)) {
            log.info("Task not found, processInstanceId={}, taskName={}.", processInstanceId, taskName);
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

}
