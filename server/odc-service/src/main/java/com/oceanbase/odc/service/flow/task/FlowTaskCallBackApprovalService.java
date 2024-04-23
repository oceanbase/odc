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
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

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
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private ScheduleService scheduleService;

    public void approval(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus,
            Map<String, Object> approvalVariables) {
        try {
            doApproval(flowInstanceId, flowTaskInstanceId, flowNodeStatus, approvalVariables);
        } catch (Throwable e) {
            log.warn(
                    "approval task callback node  failed, flowInstanceId={}, flowTaskInstanceId={}, flowNodeStatus={}, ex={}",
                    flowInstanceId, flowTaskInstanceId, flowNodeStatus.name(), e);
        }
    }

    private void doApproval(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus,
            Map<String, Object> approvalVariables) {

        if (!flowNodeStatus.isFinalStatus()) {
            log.warn(
                    "Task is not terminated, callback failed, flowInstanceId={}, flowTaskInstanceId={}, flowNodeStatus={}.",
                    flowInstanceId, flowTaskInstanceId, flowNodeStatus);
            return;
        }

        completeTask(flowInstanceId, flowTaskInstanceId, flowNodeStatus, approvalVariables);
        updateFlowInstance(flowInstanceId, flowTaskInstanceId, flowNodeStatus);
    }

    private void completeTask(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus,
            Map<String, Object> approvalVariables) {
        FlowInstanceEntity flowInstance = getFlowInstance(flowInstanceId);
        FlowableElement flowableElement = getFlowableElementOfUserTask(flowTaskInstanceId);

        Await.await().timeout(60).timeUnit(TimeUnit.SECONDS)
                .until(getFlowableTask(flowInstance, flowableElement.getName())::isPresent).build().start();
        Task task = getFlowableTask(flowInstance, flowableElement.getName()).get();
        doCompleteTask(flowInstanceId, flowTaskInstanceId, flowNodeStatus, approvalVariables, task.getId());
    }

    private void doCompleteTask(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus,
            Map<String, Object> approvalVariables, String taskId) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.putIfAbsent(APPROVAL_VARIABLE_NAME, flowNodeStatus == FlowNodeStatus.COMPLETED);
            if (approvalVariables != null && !approvalVariables.isEmpty()) {
                variables.putAll(approvalVariables);
            }
            flowableTaskService.complete(taskId, variables);
            log.info("complete task succeed, flowInstanceId={}, flowTaskInstanceId={}, flowNodeStatus={}.",
                    flowInstanceId, flowTaskInstanceId, flowNodeStatus);
        } catch (Exception e) {
            log.warn("complete task failed, flowInstanceId={}, flowTaskInstanceId={}, flowNodeStatus={}, ex={}.",
                    flowInstanceId, flowTaskInstanceId, flowNodeStatus, e);
        }
    }

    private void updateFlowInstance(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus) {
        int affectRows = serviceTaskRepository.updateStatusById(flowTaskInstanceId, flowNodeStatus);
        log.info("Modify node instance status successfully, instanceId={}, affectRows={}",
                flowNodeStatus, affectRows);
        if (flowNodeStatus == FlowNodeStatus.COMPLETED) {
            return;
        }
        Optional<ServiceTaskInstanceEntity> serviceTaskInstance = serviceTaskRepository.findById(flowTaskInstanceId);
        if (serviceTaskInstance.get().getTaskType() == TaskType.PRE_CHECK) {
            doUpdateFlowInstanceWhenPreCheckFailed(flowInstanceId, flowTaskInstanceId);
        } else {
            doUpdateFlowInstance(flowInstanceId, flowTaskInstanceId, flowNodeStatus);
        }
    }

    protected void doUpdateFlowInstanceWhenPreCheckFailed(long flowInstanceId, long flowTaskInstanceId) {
        flowInstanceRepository.updateStatusById(flowInstanceId, FlowStatus.PRE_CHECK_FAILED);
        int affectRows = serviceTaskRepository.updateStatusById(flowTaskInstanceId, FlowNodeStatus.FAILED);
        scheduleService.updateStatusByFlowInstanceId(flowInstanceId, ScheduleStatus.TERMINATION);
        log.info("Modify node instance status successfully, flowTaskInstanceId={}, affectRows={}",
                flowTaskInstanceId, affectRows);
    }

    private void doUpdateFlowInstance(long flowInstanceId, long flowTaskInstanceId, FlowNodeStatus flowNodeStatus) {
        flowInstanceRepository.updateStatusById(flowInstanceId, getFlowStatus(flowNodeStatus));
        int affectRows = serviceTaskRepository.updateStatusById(flowTaskInstanceId, flowNodeStatus);
        log.info("Modify node instance status successfully, flowTaskInstanceId={}, affectRows={}",
                flowTaskInstanceId, affectRows);
    }

    private FlowStatus getFlowStatus(FlowNodeStatus flowNodeStatus) {
        switch (flowNodeStatus) {
            case FAILED:
                return FlowStatus.EXECUTION_FAILED;
            case CANCELLED:
                return FlowStatus.CANCELLED;
            case EXPIRED:
                return FlowStatus.EXECUTION_EXPIRED;
            case COMPLETED:
                return FlowStatus.EXECUTION_SUCCEEDED;
            default:
                return FlowStatus.EXECUTING;
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
