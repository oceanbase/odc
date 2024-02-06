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

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.structurecompare.StructureComparisonContext;
import com.oceanbase.odc.service.structurecompare.StructureComparisonService;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/9
 * @since ODC_release_4.2.4
 */
@Slf4j
public class DBStructureComparisonFlowableTask extends BaseODCFlowTaskDelegate<Void> {
    private volatile StructureComparisonContext context;
    @Autowired
    private StructureComparisonService service;

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        TaskEntity taskEntity = taskService.detail(taskId);
        if (taskEntity == null) {
            throw new IllegalStateException("Can not find task entity by id " + taskId);
        }
        log.info("Structure comparison task starts, taskId={}, activityId={}", taskId,
                execution.getCurrentActivityId());

        DBStructureComparisonParameter parameters = FlowTaskUtil.getDBStructureComparisonParameter(execution);
        Validate.notNull(parameters, "Structure comparison task parameters can not be null");
        Long flowInstanceId = FlowTaskUtil.getFlowInstanceId(execution);

        context = service.create(parameters, taskId, taskEntity.getCreatorId(), flowInstanceId);
        taskService.start(taskId, context.getStatus());
        return null;
    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        if (context == null) {
            throw new IllegalStateException("Context is null, structure comparison task may not be running");
        }
        // TODO: fix the current termination operation will not take effect
        boolean result = context.cancel(true);
        log.info("Structure comparison task has been cancelled, taskId={}, result={}", taskId, result);
        taskService.cancel(taskId, context.getStatus());
        return result;
    }

    @Override
    public boolean isCancelled() {
        if (context == null) {
            return false;
        }
        return context.isCancelled();
    }

    @Override
    protected boolean isSuccessful() {
        if (context == null || !context.isDone()) {
            return false;
        }
        try {
            return context.get(1, TimeUnit.MILLISECONDS).getStatus() == TaskStatus.DONE;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected boolean isFailure() {
        if (context == null || !context.isDone()) {
            return false;
        }
        return context.getStatus().getStatus() == TaskStatus.FAILED;
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Structure comparison task timeout, taskId={}", taskId);
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Structure comparison task failed, taskId={}", taskId);
        if (context == null) {
            taskService.fail(taskId, 0, null);
        } else {
            taskService.fail(taskId, context.getProgress(), context.getStatus());
        }
        super.onFailure(taskId, taskService);

    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Structure comparison task succeed, taskId={}", taskId);
        try {
            taskService.succeed(taskId, context.get());
            updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        } catch (Exception e) {
            log.warn("Failed to get structure comparison task result", e);
        }
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (Objects.nonNull(context)) {
            taskService.updateProgress(taskId, context.getProgress());
        }
    }

}
