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

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.flow.model.AbstractFlowTaskResult;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.flow.FlowTaskInstanceService;
import com.oceanbase.odc.service.flow.exception.ServiceTaskCancelledException;
import com.oceanbase.odc.service.flow.exception.ServiceTaskError;
import com.oceanbase.odc.service.flow.exception.ServiceTaskExpiredException;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.flow.task.util.TaskDownloadUrlsProvider;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.flow.util.TaskLogFilenameGenerator;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.notification.Broker;
import com.oceanbase.odc.service.notification.NotificationProperties;
import com.oceanbase.odc.service.notification.helper.EventBuilder;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * The abstraction of process tasks on the {@code ODC} side is used to encapsulate some general
 * logic
 *
 * @author yh263208
 * @date 2022-03-05 21:20
 * @since ODC_release_3.3.0
 * @see BaseRuntimeFlowableDelegate
 */
@Slf4j
public abstract class BaseODCFlowTaskDelegate<T> extends BaseRuntimeFlowableDelegate<T> implements FlowTaskCallBack {

    @Autowired
    private TaskService taskService;
    @Autowired
    protected HostProperties hostProperties;
    @Autowired
    protected ServiceTaskInstanceRepository serviceTaskRepository;
    private final CountDownLatch taskLatch = new CountDownLatch(1);
    @Getter
    private volatile Long taskId;
    @Getter
    private volatile long timeoutMillis;
    @Getter
    private volatile long startTimeMilliSeconds;
    private ScheduledExecutorService scheduleExecutor;
    @Autowired
    private Broker broker;
    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private EventBuilder eventBuilder;
    @Autowired
    protected FlowTaskCallBackApprovalService flowTaskCallBackApprovalService;
    @Autowired
    protected CloudObjectStorageService cloudObjectStorageService;
    @Autowired
    private FlowTaskInstanceService flowTaskInstanceService;

    private void init(DelegateExecution execution) {
        this.taskId = FlowTaskUtil.getTaskId(execution);
        this.timeoutMillis = FlowTaskUtil.getExecutionExpirationIntervalMillis(execution);
        this.taskService.updateExecutorInfo(taskId, new ExecutorInfo(hostProperties));
        SecurityContextUtils.setCurrentUser(FlowTaskUtil.getTaskCreator(execution));
    }

    private void initMonitorExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("Task-Periodically-Scheduled-%d")
                .build();
        scheduleExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);
        int interval = RuntimeTaskConstants.DEFAULT_TASK_CHECK_INTERVAL_SECONDS;
        scheduleExecutor.scheduleAtFixedRate(() -> {
            try {
                onProgressUpdate(taskId, taskService);
            } catch (Exception e) {
                log.warn("Update task progress callback failed, taskId={}", taskId, e);
            }
            try {
                if (isCompleted() || isTimeout()) {
                    taskLatch.countDown();
                }
            } catch (Exception e) {
                log.warn("Task monitoring thread failed, taskId={}", taskId, e);
                taskLatch.countDown();
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    @Override
    protected void preHandle(DelegateExecution execution) {
        ExecutionStrategyConfig strategyConfig = getStrategyConfig();
        if (strategyConfig.getStrategy() == FlowTaskExecutionStrategy.TIMER) {
            Date executionTime = strategyConfig.getExecutionTime();
            Verify.notNull(executionTime, "executionTime");
            // The timing execution datetime must be after the current datetime, extra 60 seconds for buffer.
            if (executionTime.before(new Date(System.currentTimeMillis() - 60 * 1000))) {
                throw new ServiceTaskCancelledException("Execution time " + executionTime + " is before current time");
            }
        }
    }

    @Override
    protected void run(DelegateExecution execution) throws Exception {
        this.startTimeMilliSeconds = System.currentTimeMillis();
        try {
            init(execution);
            initMonitorExecutor();
            super.run(execution);
        } catch (Exception e) {
            log.warn("Failed to run task, activityId={}", execution.getCurrentActivityId(), e);
            if (scheduleExecutor != null) {
                scheduleExecutor.shutdownNow();
            }
            try {
                onFailure(taskId, taskService);
            } catch (Exception target) {
                log.warn("Task failure callback method execution failed, taskId={}", taskId, target);
            }
            if (e instanceof BaseFlowException) {
                throw e;
            }
            SecurityContextUtils.clear();
            throw new ServiceTaskError(e);
        }
        try {
            taskLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
            if (isCancelled()) {
                throw new ServiceTaskCancelledException();
            }
            if (isFailure()) {
                try {
                    onFailure(taskId, taskService);
                } catch (Exception e) {
                    log.warn("Task failure callback method execution failed, taskId={}", taskId, e);
                }
                throw new ServiceTaskError(new RuntimeException("Internal Error"));
            }
            if (isTimeout()) {
                try {
                    onTimeout(taskId, taskService);
                } catch (Exception e) {
                    log.warn("Task timeout callback method execution failed, taskId={}", taskId, e);
                }
                throw new InterruptedException();
            }
            if (!isSuccessful()) {
                // 监控线程出错导致闭锁失效，此种情况任务必须终止
                try {
                    cancel(true);
                } catch (Exception e) {
                    log.warn("Failed to cancel, taskId={}", taskId, e);
                }
                throw new ServiceTaskError(new RuntimeException("Executor Error"));
            }
            try {
                onSuccessful(taskId, taskService);
            } catch (Exception e) {
                log.warn("Task successful callback method execution failed, taskId={}", taskId, e);
            }
        } catch (InterruptedException e) {
            log.warn("The task times out, an error will be thrown, taskId={}, startTime={}, timeoutMillis={}",
                    taskId, new Date(this.startTimeMilliSeconds), timeoutMillis, e);
            try {
                cancel(true);
            } catch (Exception e1) {
                log.warn("Failed to cancel, taskId={}", taskId, e1);
            }
            if (isTimeout()) {
                throw new ServiceTaskExpiredException();
            }
            throw new ServiceTaskCancelledException();
        } finally {
            SecurityContextUtils.clear();
            scheduleExecutor.shutdownNow();
        }
    }

    @Override
    protected Callable<T> initCallable(DelegateExecution execution) {
        return () -> start(taskId, taskService, execution);
    }

    abstract protected T start(Long taskId, TaskService taskService, DelegateExecution execution) throws Exception;

    /**
     * Mark whether the task has been completed, failed, canceled, and successful execution is regarded
     * as completed
     */
    private boolean isCompleted() {
        return isSuccessful() || isFailure() || isCancelled();
    }

    /**
     * Mark whether the task execution timed out
     */
    protected boolean isTimeout() {
        return System.currentTimeMillis() - startTimeMilliSeconds > timeoutMillis;
    }

    /**
     * Mark whether the task was executed successfully
     */
    protected abstract boolean isSuccessful();

    /**
     * Mark whether the task execution failed
     */
    protected abstract boolean isFailure();

    @Override
    public void callback(@NotNull long flowInstanceId, @NotNull long flowTaskInstanceId,
            @NotNull FlowNodeStatus flowNodeStatus, Map<String, Object> approvalVariables) {
        try {
            setDownloadLogUrl(flowInstanceId);
        } catch (Exception e) {
            log.warn("Failed to set download log URL, either because the log file does not exist "
                    + "or the upload of the OSS failed, flowInstanceId={}", flowInstanceId, e);
        }
        flowTaskCallBackApprovalService.approval(flowInstanceId, flowTaskInstanceId, flowNodeStatus, approvalVariables);
    }

    /**
     * The callback method when the task fails, which is used to update the status and other operations
     */
    protected void onFailure(Long taskId, TaskService taskService) {
        callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.FAILED, null);
        if (notificationProperties.isEnabled()) {
            try {
                Event event = eventBuilder.ofFailedTask(taskService.detail(taskId));
                broker.enqueueEvent(event);
            } catch (Exception e) {
                log.warn("Failed to enqueue event.", e);
            }
        }
    }

    /**
     * The callback method when the task is successful, used to update the status and other operations
     */
    protected void onSuccessful(Long taskId, TaskService taskService) {
        callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.COMPLETED, null);
        if (notificationProperties.isEnabled()) {
            try {
                Event event = eventBuilder.ofSucceededTask(taskService.detail(taskId));
                broker.enqueueEvent(event);
            } catch (Exception e) {
                log.warn("Failed to enqueue event.", e);
            }
        }
    }

    /**
     * The callback method of the task execution timeout, which is used to update the status and other
     * operations
     */
    protected void onTimeout(Long taskId, TaskService taskService) {
        callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.EXPIRED, null);
        if (notificationProperties.isEnabled()) {
            try {
                Event event = eventBuilder.ofTimeoutTask(taskService.detail(taskId));
                broker.enqueueEvent(event);
            } catch (Exception e) {
                log.warn("Failed to enqueue event.", e);
            }
        }
    }

    /**
     * This method is scheduled periodically to update the progress of the task
     */
    protected abstract void onProgressUpdate(Long taskId, TaskService taskService);

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean result = cancel(mayInterruptIfRunning, taskId, taskService);
        if (result) {
            callback(getFlowInstanceId(), getTargetTaskInstanceId(), FlowNodeStatus.CANCELLED, null);
        }
        return result;
    }

    protected abstract boolean cancel(boolean mayInterruptIfRunning, Long taskId, TaskService taskService);

    private void setDownloadLogUrl(@NonNull Long flowInstanceId) throws IOException, NotFoundException {
        TaskEntity taskEntity = taskService.detail(taskId);
        File logFile;
        try {
            logFile = taskService.getLogFile(taskEntity.getCreatorId(), taskId + "", taskEntity.getTaskType(),
                    OdcTaskLogLevel.ALL);
        } catch (UnsupportedException e) {
            // If the log file does not exist, the download URL will not be set
            return;
        }
        String downloadUrl = String.format("/api/v2/flow/flowInstances/%s/tasks/log/download", flowInstanceId);
        if (Objects.nonNull(cloudObjectStorageService) && cloudObjectStorageService.supported()) {
            String fileName = TaskLogFilenameGenerator.generate(flowInstanceId);
            try {
                String objectName = cloudObjectStorageService.uploadTemp(fileName, logFile);
                downloadUrl = TaskDownloadUrlsProvider
                        .concatBucketAndObjectName(cloudObjectStorageService.getBucketName(), objectName);
                log.info("Upload task log file to OSS, taskId={}, logFileName={}", taskId, fileName);
            } catch (Exception e) {
                log.warn("Failed to upload task log file to OSS, taskId={}", taskId, e);
                throw new RuntimeException(
                        String.format("Failed to upload task log file to OSS, file name: %s", fileName),
                        e.getCause());
            }
        }
        List<? extends FlowTaskResult> flowTaskResults =
                flowTaskInstanceService.getTaskResultFromEntity(taskEntity, false);
        Verify.singleton(flowTaskResults, "flowTaskResults");
        if (flowTaskResults.get(0) instanceof AbstractFlowTaskResult) {
            FlowTaskResult flowTaskResult = flowTaskResults.get(0);
            AbstractFlowTaskResult abstractFlowTaskResult = (AbstractFlowTaskResult) flowTaskResult;
            abstractFlowTaskResult.setFullLogDownloadUrl(downloadUrl);
            taskEntity.setResultJson(JsonUtils.toJson(flowTaskResult));
            taskService.update(taskEntity);
        }
    }
}
