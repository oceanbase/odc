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
package com.oceanbase.odc.agent.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.constants.JobAttributeKeyConstants;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.constants.JobServerUrls;
import com.oceanbase.odc.service.task.executor.HeartbeatRequest;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.executor.TraceDecoratorThreadFactory;
import com.oceanbase.odc.service.task.executor.logger.LogBizImpl;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
@Slf4j
class TaskMonitor {

    private static final int REPORT_RESULT_RETRY_TIMES = Integer.MAX_VALUE;
    private static final long WAIT_AFTER_LOG_METADATA_COLLECT_MILLS = 5000L;
    private final TaskReporter reporter;
    private final TaskContainer<?> taskContainer;
    private final CloudObjectStorageService cloudObjectStorageService;
    private volatile long startTimeMilliSeconds;
    private ScheduledExecutorService reportScheduledExecutor;
    private ScheduledExecutorService heartScheduledExecutor;
    private Map<String, String> logMetadata = new HashMap<>();
    private AtomicLong logMetaCollectedMillis = new AtomicLong(0L);
    private Lock reportLock = new ReentrantLock();
    private AtomicBoolean metaDataReported = new AtomicBoolean(false);

    public TaskMonitor(TaskContainer<?> task, TaskReporter taskReporter,
            CloudObjectStorageService cloudObjectStorageService) {
        this.taskContainer = task;
        this.reporter = taskReporter;
        this.cloudObjectStorageService = cloudObjectStorageService;
    }

    public void markLogMetaCollected() {
        this.logMetaCollectedMillis.set(System.currentTimeMillis());
    }

    public Map<String, String> getLogMetadata() {
        return new HashMap<>(this.logMetadata);
    }

    public void monitor() {
        log.info("monitor starting, jobId={}", getJobId());
        this.startTimeMilliSeconds = System.currentTimeMillis();
        initReportScheduler();
        initHeartbeatScheduler();
    }

    private void initReportScheduler() {
        ThreadFactory threadFactory =
                new TraceDecoratorThreadFactory(new TaskThreadFactory(("Task-Monitor-Job-" + getJobId())));
        this.reportScheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        reportScheduledExecutor.scheduleAtFixedRate(() -> {
            if (isTimeout() && !getTaskContainer().getStatus().isTerminated()) {
                log.info("Task timeout, try stop, jobId={}", getJobId());
                getTaskContainer().stopTask();
            }
            try {
                if (JobUtils.getExecutorPort().isPresent()) {
                    reportTaskResult();
                }
            } catch (Throwable e) {
                log.warn("Update task info failed, id: {}", getJobId(), e);
            }
        }, JobConstants.REPORT_TASK_INFO_DELAY_SECONDS,
                JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
        log.info("Task monitor init success");
    }

    private void initHeartbeatScheduler() {
        if (JobUtils.getExecutorPort().isPresent() && JobUtils.isReportEnabled()) {
            heartScheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                    new TaskThreadFactory(("Task-Heart-Job-" + getJobId())));

            heartScheduledExecutor.scheduleAtFixedRate(() -> {
                try {
                    if (JobUtils.getExecutorPort().isPresent() && JobUtils.isReportEnabled()) {
                        getReporter().report(JobServerUrls.TASK_HEARTBEAT, buildHeartRequest());
                    }
                } catch (Throwable e) {
                    log.warn("Update heart info failed, id: {}", getJobId(), e);
                }
            }, JobConstants.REPORT_TASK_HEART_DELAY_SECONDS,
                    JobConstants.REPORT_TASK_HEART_INTERVAL_SECONDS,
                    TimeUnit.SECONDS);
            log.info("Task heart init success");
        } else {
            log.info("heart beat not needed, cause report not needed");
        }
    }

    public void finalWork() {
        try {
            doFinal();
        } finally {
            destroy(reportScheduledExecutor);
            destroy(heartScheduledExecutor);
        }
    }

    private void destroy(ExecutorService executorService) {
        try {
            if (executorService != null) {
                executorService.shutdownNow();
            }
        } catch (Throwable ex) {
            // shutdown quietly
        }
    }

    @VisibleForTesting
    protected void reportTaskResult() {
        if (JobUtils.isReportDisabled()) {
            return;
        }
        TaskResult taskResult = DefaultTaskResultBuilder.build(getTaskContainer());
        if (taskResult.getStatus().isTerminated()) {
            log.info("job {} status {} is terminate, monitor report be ignored.",
                    taskResult.getJobIdentity().getId(), taskResult.getStatus());
            return;
        }
        // 1. exclusive report
        // 2. if doFinal called, ignore following report
        reportLock.lock();
        try {
            if (metaDataReported.get()) {
                log.info("doFinal has called, monitor report be ignored., jobId = {}, status = {}",
                        taskResult.getJobIdentity().getId(), taskResult.getStatus());
                return;
            }
            getReporter().report(JobServerUrls.TASK_UPLOAD_RESULT, taskResult);
            log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}", getJobId(),
                    taskResult.getStatus(), String.format("%.2f", taskResult.getProgress()), getTask().getTaskResult());
        } finally {
            reportLock.unlock();
        }
    }

    @VisibleForTesting
    protected boolean isTimeout() {
        String milliSecStr =
                getTask().getJobContext().getJobParameters()
                        .get(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS);

        if (StringUtils.isNotBlank(milliSecStr)) {
            boolean timeout = System.currentTimeMillis() - startTimeMilliSeconds > Long.parseLong(milliSecStr);
            if (timeout) {
                log.info("Task timeout, jobId={}, timeoutMills={}", getJobId(), milliSecStr);
            }
            return timeout;
        } else {
            return false;
        }
    }

    @VisibleForTesting
    protected void doFinal() {
        TaskResult finalResult = DefaultTaskResultBuilder.build(getTaskContainer());
        // Report final result
        log.info("Task id: {}, finished with status: {}, start to report final result", getJobId(),
                finalResult.getStatus());

        // todo wait timeout for upload log file
        log.info("Task id: {}, start to do remained work.", getJobId());
        uploadLogFileToCloudStorage(finalResult);

        Map<String, String> latestLogMeta = finalResult.getLogMetadata();
        log.info("Task id: {}, latest log meta: {}", getJobId(), latestLogMeta);
        if (latestLogMeta != null) {
            this.logMetadata.putAll(latestLogMeta);
        }

        log.info("Task id: {}, remained work be completed, report finished status.", getJobId());

        if (JobUtils.isReportEnabled()) {
            // assign error for last report
            DefaultTaskResultBuilder.assignErrorMessage(finalResult, taskContainer.getError());
            // Report finish signal to task server
            reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES,
                    JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS);
        } else {
            waitForTaskResultPulled();
        }
        log.info("Task id: {} exit.", getJobId());
    }

    @VisibleForTesting
    protected void waitForTaskResultPulled() {
        long currentTimeMillis = System.currentTimeMillis();
        while (this.logMetaCollectedMillis.get() == 0
                || currentTimeMillis > this.logMetaCollectedMillis.get() + WAIT_AFTER_LOG_METADATA_COLLECT_MILLS) {
            log.info("wait for log meta pulled by odc-server, jobId={}, logMetaCollectedMillis={}",
                    getJobId(), this.logMetaCollectedMillis.get());
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("log meta pulled by odc-server, jobId={}, logMetaCollectedMillis={}",
                getJobId(), this.logMetaCollectedMillis.get());
    }

    @VisibleForTesting
    protected void uploadLogFileToCloudStorage(TaskResult finalResult) {

        Map<String, String> logMap = finalResult.getLogMetadata();
        Map<String, String> logMetaData = new HashMap<>();
        // append previous result
        if (null != logMap) {
            logMetaData.putAll(logMap);
        }
        if (cloudObjectStorageService != null && cloudObjectStorageService.supported()
                && JobUtils.isK8sRunModeOfEnv()) {
            logMetaData.putAll(new LogBizImpl().uploadLogFileToCloudStorage(finalResult.getJobIdentity(),
                    cloudObjectStorageService));
        } else {
            logMetaData.put(JobAttributeKeyConstants.LOG_STORAGE_FAILED_REASON,
                    "cloudObjectStorageService is null or not supported");
        }
        finalResult.setLogMetadata(logMetaData);
    }

    @VisibleForTesting
    protected boolean reportTaskResultWithRetry(TaskResult result, int retries, int retryIntervalSeconds) {
        reportLock.lock();
        try {
            if (result.getStatus() == TaskStatus.DONE) {
                result.setProgress(100.0);
            }
            int retryTimes = 0;
            while (retryTimes++ < retries) {
                try {
                    boolean success = reporter.report(JobServerUrls.TASK_UPLOAD_RESULT, result);
                    if (success) {
                        log.info("Report task result successfully");
                        metaDataReported.set(true);
                        return true;
                    } else {
                        log.warn("Report task result failed, will retry after {} seconds, remaining retries: {}",
                                retryIntervalSeconds, retries - retryTimes);
                        Thread.sleep(retryIntervalSeconds * 1000L);
                    }
                } catch (Throwable e) {
                    log.warn("Report task result failed, taskId: {}", getJobId(), e);
                }
            }
            return false;
        } finally {
            reportLock.unlock();
        }
    }

    private HeartbeatRequest buildHeartRequest() {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setJobIdentity(getTask().getJobContext().getJobIdentity());
        request.setExecutorEndpoint(JobUtils.getExecutorPoint());
        return request;
    }

    private TaskContainer<?> getTaskContainer() {
        return taskContainer;
    }

    @VisibleForTesting
    protected TaskReporter getReporter() {
        return reporter;
    }

    private Long getJobId() {
        return getTask().getJobContext().getJobIdentity().getId();
    }

    private Task<?> getTask() {
        return getTaskContainer().getTask();
    }
}
