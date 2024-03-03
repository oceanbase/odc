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

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@Slf4j
public class FlowTaskCallBackApprovalUtils {


    public static void approval(long flowInstanceId, long taskId, TaskStatus taskStatus) {
        approval(flowInstanceId, taskId, taskStatus, null);
    }

    public static void approval(long flowInstanceId, long taskId, TaskStatus taskStatus,
            Map<String, Object> approvalVariables) {

        if (!taskStatus.isTerminated()) {
            log.warn("Task is not terminated, callback failed, taskId={}.", taskId);
            return;
        }
        try {
            String taskName = null;
            String processInstanceId = null;
            String processDefinitionId = null;

            Optional<FlowTaskInstance> flowTaskInstance = flowableAdaptor.getTaskInstanceByActivityId(
                execution.getCurrentActivityId(), FlowTaskUtil.getFlowInstanceId(execution));
            Verify.verify(flowTaskInstance.isPresent(), "flowTaskInstance is null.");

            SpringContextUtil.getBean(FlowInstanceService.class).listByIds(flowInstanceId)
                ;


            SpringContextUtil.getBean(NodeInstanceEntityRepository.class)
            .findByInstanceIdAndInstanceTypeAndFlowableElementType();



            TaskService taskService = SpringContextUtil.getBean(TaskService.class);

            List<Task> task =  taskService.createTaskQuery().taskName(taskName).processInstanceId(processInstanceId)
                .processDefinitionId(processDefinitionId).list();
            Verify.verify(CollectionUtils.isNotEmpty(task), "Callback task is null");
            Verify.verify(task.size() == 1, "Expect callback size is 1, but taskSize="+task.size());

            Map<String, Object> variables = new HashMap<>();
            variables.putIfAbsent(APPROVAL_VARIABLE_NAME, taskStatus == TaskStatus.DONE);
            if (approvalVariables != null && !approvalVariables.isEmpty()) {
                variables.putAll(approvalVariables);
            }
            taskService.complete(task.get(0).getId(), variables);

        } catch (Exception e) {
            log.warn("Failed to reject flow instance, flowInstanceId={}", flowInstanceId);
        }
    }
}
