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
import java.util.Objects;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.task.model.MockDataTaskResult;
import com.oceanbase.odc.service.flow.task.model.MockProperties;
import com.oceanbase.odc.service.flow.task.model.OdcMockTaskConfig;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.tools.datamocker.ObDataMocker;
import com.oceanbase.tools.datamocker.ObMockerFactory;
import com.oceanbase.tools.datamocker.core.task.TableTaskContext;
import com.oceanbase.tools.datamocker.model.config.MockTaskConfig;
import com.oceanbase.tools.datamocker.model.enums.MockTaskStatus;
import com.oceanbase.tools.datamocker.schedule.MockContext;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/2/18
 */

@Slf4j
public class MockDataRuntimeFlowableTask extends BaseODCFlowTaskDelegate<Void> {

    @Autowired
    private MockProperties mockProperties;
    @Autowired
    private CloudObjectStorageService cloudObjectStorageService;
    @Autowired
    private OssTaskReferManager ossTaskReferManager;
    private volatile MockContext context;
    private volatile ConnectionConfig connectionConfig;
    private Exception thrown = null;

    @Override
    public boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService) {
        Verify.notNull(context, "MockContext is null");
        boolean isInterrupted = context.shutdown();
        taskService.cancel(taskId, getResult());
        TableTaskContext tableContext = context.getTables().get(0);
        logInfo(taskId, "The mock data task was cancelled, taskId={}, status={}, cancelResult={}", taskId,
                tableContext == null ? null : tableContext.getStatus(), isInterrupted);
        return isInterrupted;
    }

    @Override
    public boolean isCancelled() {
        if (context == null) {
            return false;
        }
        TableTaskContext tableTaskContext = context.getTables().get(0);
        Verify.notNull(tableTaskContext, "TableTaskContext");
        return MockTaskStatus.CANCELED == tableTaskContext.getStatus();
    }

    private MockTaskConfig getMockTaskConfig(Long taskId, DelegateExecution execution) {
        OdcMockTaskConfig config = FlowTaskUtil.getMockParameter(execution);
        this.connectionConfig = FlowTaskUtil.getConnectionConfig(execution);
        return FlowTaskUtil.generateMockConfig(taskId, execution, getTimeoutMillis(),
                config, mockProperties);
    }

    @Override
    protected Void start(Long taskId, TaskService taskService, DelegateExecution execution) {
        try {
            logInfo(taskId, "MockData task starts, taskId={}, activityId={}", taskId,
                    execution.getCurrentActivityId());
            MockTaskConfig mockTaskConfig = getMockTaskConfig(taskId, execution);
            ObMockerFactory factory = new ObMockerFactory(mockTaskConfig);
            ObDataMocker mocker = factory.create(new CustomMockScheduler(TraceContextHolder.getTraceContext(),
                    ossTaskReferManager, cloudObjectStorageService));
            context = mocker.start();
            taskService.start(taskId, getResult());
            Verify.notNull(context, "MockContext can not be null");
            return null;
        } catch (Exception e) {
            thrown = e;
            throw e;
        }
    }

    @Override
    protected boolean isSuccessful() {
        if (context == null) {
            return false;
        }
        TableTaskContext tableTaskContext = context.getTables().get(0);
        Verify.notNull(tableTaskContext, "TableTaskContext");
        return MockTaskStatus.SUCCESS == tableTaskContext.getStatus();
    }

    @Override
    protected boolean isFailure() {
        if (thrown != null) {
            return true;
        } else if (context == null) {
            return false;
        }
        TableTaskContext tableTaskContext = context.getTables().get(0);
        Verify.notNull(tableTaskContext, "TableTaskContext");
        return MockTaskStatus.FAILED == tableTaskContext.getStatus();
    }

    @Override
    protected void onFailure(Long taskId, TaskService taskService) {
        logWarn(taskId, "Mock data task failed, taskId={}", taskId);
        if (context == null) {
            /**
             * 这里进行{@link TraceContextHolder}设置的原因在于：模拟数据任务有可能在启动时{@link ObDataMocker#start()}
             * 报错，之前的版本里该调用是用户的request线程在执行，因此如果出错用户可以感知到。现在集成到{@code flowable}中
             * 之后模拟数据的启动发生在非用户线程中，一旦启动报错用户将难以感知。在这里进行设置的原因在于想把启动异常通过日志的
             * 方式"带出去"，由于启动异常时任务还没有运行因此需要手动设定一下MDC以将日志打印到目标位置。
             */
            logWarn(taskId, "Mock data task failed, taskId={}", taskId, thrown);
            taskService.fail(taskId, 0, new MockDataTaskResult(connectionConfig, taskId + ""));
        } else {
            context.shutdown();
            taskService.fail(taskId, context.getProgress(), getResult());
        }
        super.onFailure(taskId, taskService);
    }

    @Override
    protected void onSuccessful(Long taskId, TaskService taskService) {
        logInfo(taskId, "Mock data task succeed, taskId={}", taskId);
        taskService.succeed(taskId, getResult());
        updateFlowInstanceStatus(FlowStatus.EXECUTION_SUCCEEDED);
        context.shutdown();
        super.onSuccessful(taskId, taskService);
    }

    @Override
    protected void onTimeout(Long taskId, TaskService taskService) {
        logWarn(taskId, "Mock data task timeout, taskId={}", taskId);
        taskService.fail(taskId, context.getProgress(), getResult());
        context.shutdown();
    }

    @Override
    protected void onProgressUpdate(Long taskId, TaskService taskService) {
        if (Objects.nonNull(context)) {
            /**
             * Update percentage, every {@link RuntimeTaskConstants.DEFAULT_TASK_CHECK_INTERVAL_SECONDS}
             */
            taskService.updateProgress(taskId, context.getProgress());
        }
    }

    private MockDataTaskResult getResult() {
        return new MockDataTaskResult(connectionConfig, context);
    }

    private void logInfo(Long taskId, @NonNull String s, Object... objects) {
        Map<String, String> variables = new HashMap<>();
        variables.putIfAbsent("mocktask.workspace", taskId + "");
        TraceContextHolder.span(variables);
        try {
            log.info(s, objects);
        } finally {
            TraceContextHolder.clear();
        }
    }

    private void logWarn(Long taskId, @NonNull String s, Object... objects) {
        Map<String, String> variables = new HashMap<>();
        variables.putIfAbsent("mocktask.workspace", taskId + "");
        TraceContextHolder.span(variables);
        try {
            log.warn(s, objects);
        } finally {
            TraceContextHolder.clear();
        }
    }

}
