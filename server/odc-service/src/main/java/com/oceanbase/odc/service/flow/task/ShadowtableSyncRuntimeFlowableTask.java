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

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.shadowtable.ShadowTableSyncService;
import com.oceanbase.odc.service.shadowtable.ShadowTableSyncTaskContext;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/9/22 下午6:53
 * @Description: []
 */
@Slf4j
public class ShadowtableSyncRuntimeFlowableTask extends BaseODCFlowTaskDelegate<ShadowTableSyncTaskResult> {

    @Autowired
    private ShadowTableSyncService syncService;

    private volatile ShadowTableSyncTaskContext context;


    @Override
    protected ShadowTableSyncTaskResult start(Long taskId, TaskService taskService,
            DelegateExecution execution) {
        log.info("Shadow table sync task starts, taskId={}", taskId);
        ShadowTableSyncTaskParameter taskParameter = FlowTaskUtil.getShadowTableSyncTaskParameter(execution);
        taskParameter.setSchemaName(FlowTaskUtil.getSchemaName(execution));
        taskParameter.setConnectionConfig(FlowTaskUtil.getConnectionConfig(execution));
        this.context = syncService.create(taskParameter, taskId);
        taskService.start(taskId, context.getResult());
        return null;
    }

    @Override
    protected boolean isSuccessful() {
        if (context == null || !context.isDone()) {
            return false;
        }
        return context.isSuccessful();
    }

    @Override
    protected boolean isFailure() {
        if (context == null || !context.isDone()) {
            return false;
        }
        return context.isFailed();
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Shadowtable sync task failed, taskId={}", taskId);
        ShadowTableSyncTaskResult result = context.getResult();
        if (context == null) {
            taskService.fail(taskId, 0, result);
        } else {
            taskService.fail(taskId, context.getProgress(), result);
        }
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Shadowtable sync task succeed, taskId={}", taskId);
        try {
            taskService.succeed(taskId, context.getResult());
            updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        } catch (Exception e) {
            log.warn("Failed to get result", e);
        }
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Shadowtable sync task timeout, taskId={}", taskId);
        taskService.fail(taskId, context.getProgress(), context.getResult());
        super.onTimeout(taskId, taskService);
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (Objects.isNull(context)) {
            return;
        }
        taskService.updateProgress(taskId, context.getProgress());
    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        if (context == null) {
            throw new IllegalStateException("Context is null, task may not be running");
        }
        boolean result = context.cancel(true);
        log.info("Shadowtable sync task has been cancelled, taskId={}, result={}", taskId, result);
        taskService.cancel(taskId, context.getResult());
        return true;
    }

    @Override
    public boolean isCancelled() {
        if (context == null) {
            return false;
        }
        return context.isCancelled();
    }
}
