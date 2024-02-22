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

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.delegate.DelegateExecution;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.partitionplan.PartitionPlanScheduleService;
import com.oceanbase.odc.service.partitionplan.PartitionPlanTaskTraceContextHolder;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author tianke
 * @author yh263208
 * @date 2024-02-21 20:07
 * @since ODC_release_4.2.4
 */
@Slf4j
public class PartitionPlanRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    private volatile boolean isSuccessful = false;
    private volatile boolean isFailure = false;
    @Autowired
    private PartitionPlanScheduleService partitionPlanService;

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws ClassNotFoundException, SchedulerException {
        PartitionPlanTaskTraceContextHolder.trace(taskId);
        log.info("Partition plan task starts, taskId={}", taskId);
        try {
            taskService.start(taskId);
            PartitionPlanConfig parameters = FlowTaskUtil.getPartitionPlanParameter(execution);
            parameters.setFlowInstanceId(getFlowInstanceId());
            parameters.setTaskId(taskId);
            this.partitionPlanService.submit(parameters);
            this.isFailure = false;
            this.isSuccessful = true;
            return null;
        } catch (Exception e) {
            log.warn("Partition plan task failed, taskId={}", taskId, e);
            this.isFailure = true;
            this.isSuccessful = false;
            throw e;
        }
    }

    @Override
    protected boolean isSuccessful() {
        return this.isSuccessful;
    }

    @Override
    protected boolean isFailure() {
        return this.isFailure;
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        taskService.fail(taskId, 100, generateResult(false));
        super.onFailure(taskId, taskService);
        PartitionPlanTaskTraceContextHolder.clear();
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        taskService.succeed(taskId, generateResult(true));
        super.onSuccessful(taskId, taskService);
        log.info("Partition plan task succeed, taskId={}", taskId);
        PartitionPlanTaskTraceContextHolder.clear();
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        taskService.fail(taskId, 100, generateResult(false));
        super.onTimeout(taskId, taskService);
        PartitionPlanTaskTraceContextHolder.clear();
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {}

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    private Map<String, Object> generateResult(boolean success) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        return result;
    }

}
