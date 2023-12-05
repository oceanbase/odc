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
package com.oceanbase.odc.service.resultset;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.service.datasecurity.DataMaskingService;
import com.oceanbase.odc.service.flow.task.BaseODCFlowTaskDelegate;
import com.oceanbase.odc.service.flow.task.model.ResultSetExportResult;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.task.TaskService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultSetExportFlowableTask extends BaseODCFlowTaskDelegate<ResultSetExportResult> {
    public static final String TASK_WORKSPACE = "task.workspace";

    @Autowired
    private DumperResultSetExportTaskManager taskManager;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private DataMaskingService maskingService;
    private ResultSetExportTaskContext context;
    private volatile TaskStatus status;

    @Override
    protected ResultSetExportResult start(Long taskId, TaskService taskService, DelegateExecution execution)
            throws Exception {
        taskService.start(taskId);
        status = TaskStatus.RUNNING;

        ResultSetExportTaskParameter parameter = FlowTaskUtil.getResultSetExportTaskParameter(execution);
        parameter.setDatabase(FlowTaskUtil.getSchemaName(execution));

        context = taskManager.start(FlowTaskUtil.getConnectionConfig(execution), parameter, taskId.toString());

        return null;
    }

    @Override
    protected boolean isSuccessful() {
        if (context == null || isCancelled()) {
            return false;
        }
        return status == TaskStatus.DONE;
    }

    @Override
    protected boolean isFailure() {
        if (context == null || isCancelled()) {
            return false;
        }
        return status == TaskStatus.FAILED;
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        log.info("Result set export task succeed, taskId={}", taskId);
        try {
            taskService.succeed(taskId, context.get());
            updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        } catch (Exception e) {
            log.warn("Failed to get result.", e);
        }
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        log.warn("Result set export task failed, taskId={}", taskId);
        taskService.fail(taskId, 0, "");
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        log.warn("Result set export task timeout, taskId={}", taskId);
        taskService.fail(taskId, context.progress(), "");
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (context != null) {
            try {
                context.get();
                status = TaskStatus.DONE;
            } catch (InterruptedException | ExecutionException e) {
                status = TaskStatus.FAILED;
                log.warn("Result set export failed: ", e);
            } catch (TimeoutException ignore) {
                // eat exception
            }
        }
        taskService.updateProgress(taskId, context == null ? 0 : context.progress());
    }

    @Override
    protected boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        context.cancel(mayInterruptIfRunning);
        return true;
    }

    @Override
    public boolean isCancelled() {
        return context.isCanceled();
    }

}
