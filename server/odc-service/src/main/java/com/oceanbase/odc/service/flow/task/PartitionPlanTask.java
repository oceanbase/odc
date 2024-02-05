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

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.service.flow.task.model.PartitionPlanTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.PartitionPlanTaskTraceContextHolder;
import com.oceanbase.odc.service.partitionplan.model.DatabasePartitionPlan;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTaskParameters;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tianke
 * @Date: 2022/9/18 19:31
 * @Descripition:
 */
@Slf4j
public class PartitionPlanTask extends BaseODCFlowTaskDelegate<PartitionPlanTaskResult> {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private PartitionPlanService partitionPlanService;
    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;

    @Override
    protected PartitionPlanTaskResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        PartitionPlanTaskTraceContextHolder.trace(authenticationFacade.currentUser().getId(), taskId);
        log.info("Partition plan task executing...");
        try {

            PartitionPlanTaskParameters taskParameters = FlowTaskUtil.getPartitionPlanParameter(execution);
            DatabasePartitionPlan databasePartitionPlan = taskParameters.getConnectionPartitionPlan();
            taskService.start(taskId);
            // Create and enable partition plan.
            databasePartitionPlan.setFlowInstanceId(getFlowInstanceId());
            partitionPlanService.createDatabasePartitionPlan(databasePartitionPlan);
            PartitionPlanTaskResult taskResult = new PartitionPlanTaskResult();
            taskResult.setFlowInstanceId(getFlowInstanceId());
            taskResult.setDatabasePartitionPlan(databasePartitionPlan);
            isSuccessful = true;
            taskService.succeed(taskId, taskResult);
            log.info("Partition plan task succeed.");
            return taskResult;
        } catch (Exception e) {
            isFailure = true;
            PartitionPlanTaskResult taskResult = new PartitionPlanTaskResult();
            taskResult.setFlowInstanceId(getFlowInstanceId());
            taskService.fail(taskId, 0, taskResult);
            log.warn("Partition plan task failed,error={}", e.getMessage());
            return taskResult;
        } finally {
            PartitionPlanTaskTraceContextHolder.clear();
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
        log.warn("Partition plan task failed, taskId={}", taskId);
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Partition plan task succeed, taskId={}", taskId);
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Partition plan task timeout, taskId={}", taskId);
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {

    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        taskService.cancel(taskId);
        return true;

    }

    @Override
    public boolean isCancelled() {
        return false;
    }
}
