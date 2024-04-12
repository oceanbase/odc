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

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author: zijia.cj
 * @date: 2024/3/29
 */
@Slf4j
public class MultipleDatabaseChangeRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;

    private Long batchId;

    private Long batchNumber;

    @Autowired
    private FlowInstanceService flowInstanceService;

    @Autowired
    private FlowableAdaptor flowableAdaptor;

    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws InterruptedException {

        try {
            /*
             * FlowTaskInstance flowTaskInstance = flowableAdaptor.getTaskInstanceByActivityId(
             * execution.getCurrentActivityId(), getFlowInstanceId()) .orElseThrow(() -> new
             * RuntimeException("获取流程实例对象失败"));
             * log.info("multiple database task starts, taskId={}, activityId={}", taskId,
             * execution.getCurrentActivityId()); taskService.start(flowTaskInstance.getTargetTaskId(),
             * generateResult(false)); Long targetTaskId = flowTaskInstance.getTargetTaskId();
             */
            TaskEntity detail = taskService.detail(taskId);
            // this.taskId = targetTaskId;
            MultipleDatabaseChangeParameters multipleDatabaseChangeParameters = JsonUtils.fromJson(
                    detail.getParametersJson(), MultipleDatabaseChangeParameters.class);
            Integer batchId;
            Object value = multipleDatabaseChangeParameters.getBatchId();
            if (value == null) {
                batchId = 0;
            } else {
                batchId = (Integer) value;
            }
            multipleDatabaseChangeParameters.setBatchId(batchId + 1);
            detail.setParametersJson(JsonUtils.toJson(multipleDatabaseChangeParameters));
            taskService.updateParametersJson(detail);
            this.batchId = Long.valueOf(batchId);
            // this.batchId = multipleDatabaseChangeParameters.getBatchId();
            this.batchNumber = (long) multipleDatabaseChangeParameters.getOrderedDatabaseIds().size();
            List<Integer> batchDatabaseIds =
                    multipleDatabaseChangeParameters.getOrderedDatabaseIds().get(batchId);
            Set<Long> flowInstanceIds = new HashSet<>();
            for (Integer batchDatabaseId : batchDatabaseIds) {
                // 构造单库工单参数，绑定父工单
                CreateFlowInstanceReq createFlowInstanceReq = new CreateFlowInstanceReq();
                createFlowInstanceReq.setDatabaseId(Long.valueOf(batchDatabaseId));
                createFlowInstanceReq.setTaskType(TaskType.ASYNC);
                createFlowInstanceReq.setExecutionStrategy(FlowTaskExecutionStrategy.AUTO);
                createFlowInstanceReq.setParentFlowInstanceId(FlowTaskUtil.getFlowInstanceId(execution));
                createFlowInstanceReq.setParameters(multipleDatabaseChangeParameters
                        .convertIntoDatabaseChangeParameters(multipleDatabaseChangeParameters));
                List<FlowInstanceDetailResp> individualFlowInstance = flowInstanceService.createIndividualFlowInstance(
                        createFlowInstanceReq);
                flowInstanceIds.add(individualFlowInstance.get(0).getId());
            }

            long originalTime = System.currentTimeMillis();
            // 是否成功标志
            boolean flag = true;
            while (System.currentTimeMillis() - originalTime <= multipleDatabaseChangeParameters.getTimeoutMillis()) {
                // 是否完成标志，用于跳出循环
                int number = 0;
                for (Long flowInstanceId : flowInstanceIds) {
                    FlowInstanceDetailResp detailById = flowInstanceService.detail(flowInstanceId);
                    if (detailById != null) {
                        switch (detailById.getStatus()) {
                            case EXECUTION_SUCCEEDED:
                                number++;
                                break;
                            case EXECUTION_FAILED:
                            case EXECUTION_EXPIRED:
                                flag = false;
                                number++;
                                break;
                            default:
                                break;
                        }
                    }
                }
                if (number == multipleDatabaseChangeParameters.getOrderedDatabaseIds().get(
                        Math.toIntExact(this.batchId)).size()) {
                    break;
                }
                Thread.sleep(1000);
            }

            if (flag) {
                this.isFailure = false;
                this.isSuccessful = true;
            } else {
                this.isFailure = true;
                this.isSuccessful = false;
            }
            return null;
        } catch (Exception e) {
            log.warn("multiple database task failed, taskId={}", taskId, e);
            this.isFailure = true;
            this.isSuccessful = false;
            throw e;
        }
    }

    @Override
    protected boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    protected boolean isFailure() {
        return isFailure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("multiple database task failed, taskId={}", taskId);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("multiple database task succeed, taskId={}", taskId);
        // 判断是否为最后一个多库变更任务节点
        if (this.batchId == batchNumber - 1) {
            // 查询所有单库工单是否成功。
            List<ServiceTaskInstanceEntity> byTargetTaskId = serviceTaskInstanceRepository.findByTargetTaskId(
                    taskId).orElseThrow(() -> new NoSuchElementException("获取工单列表失败"));
            List<FlowInstanceEntity> flowInstanceByParentId = flowInstanceService.getFlowInstanceByParentId(
                    byTargetTaskId.get(0).getFlowInstanceId());
            boolean allSucceeded = flowInstanceByParentId.stream()
                    .map(FlowInstanceEntity::getId)
                    .map(flowInstanceService::detail)
                    .allMatch(detail -> FlowStatus.EXECUTION_SUCCEEDED.equals(detail.getStatus()));
            if (allSucceeded) {
                updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
            } else {
                updateFlowInstanceStatus(FlowStatus.EXECUTION_FAILED);
            }
        }
        taskService.succeed(taskId, generateResult(true));
        super.onSuccessful(taskId, taskService);

    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        taskService.fail(taskId, 100, generateResult(false));
        log.warn("multiple database task timeout, taskId={}", taskId);
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {}

    private MultipleDatabaseChangeTaskResult generateResult(boolean success) {
        MultipleDatabaseChangeTaskResult result = new MultipleDatabaseChangeTaskResult();
        result.setSuccess(success);
        return result;
    }
}
