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
package com.oceanbase.odc.service.task.executor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.base.BaseTask;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.constants.JobServerUrls;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.logger.LogBiz;
import com.oceanbase.odc.service.task.executor.logger.LogBizImpl;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
@Slf4j
public class TaskMonitor {

    private static final int REPORT_RESULT_RETRY_TIMES = Integer.MAX_VALUE;
    private static final long WAIT_AFTER_LOG_METADATA_COLLECT_MILLS = 5000L;
    private final TaskReporter reporter;
    private final BaseTask<?> task;
    private final CloudObjectStorageService cloudObjectStorageService;
    private volatile long startTimeMilliSeconds;
    private ScheduledExecutorService reportScheduledExecutor;
    private ScheduledExecutorService heartScheduledExecutor;
    private Map<String, String> logMetadata = new HashMap<>();
    private AtomicLong logMetaCollectedMillis = new AtomicLong(0L);

    public TaskMonitor(BaseTask<?> task, CloudObjectStorageService cloudObjectStorageService) {
        this.task = task;
        this.reporter = new TaskReporter(task.getJobContext().getHostUrls());;
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

        ThreadFactory threadFactory =
                new TraceDecoratorThreadFactory(new TaskThreadFactory(("Task-Monitor-Job-" + getJobId())));
        this.reportScheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        reportScheduledExecutor.scheduleAtFixedRate(() -> {
            if (isTimeout() && !getTask().getStatus().isTerminated()) {
                log.info("Task timeout, try stop, jobId={}", getJobId());
                getTask().stop();
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

    private void reportTaskResult() {
        if (JobUtils.isReportDisabled()) {
            return;
        }
        DefaultTaskResult taskResult = DefaultTaskResultBuilder.build(getTask());
        DefaultTaskResult copiedResult = ObjectUtil.deepCopy(taskResult, DefaultTaskResult.class);
        if (copiedResult.getStatus().isTerminated()) {
            log.info("job {} status {} is terminate, monitor report be ignored.",
                    copiedResult.getJobIdentity().getId(), copiedResult.getStatus());
            return;
        }

        getReporter().report(JobServerUrls.TASK_UPLOAD_RESULT, copiedResult);
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}", getJobId(),
                copiedResult.getStatus(), String.format("%.2f", copiedResult.getProgress()), getTask().getTaskResult());
    }

    private boolean isTimeout() {
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

    private void doFinal() {

        DefaultTaskResult finalResult = DefaultTaskResultBuilder.build(getTask());
        // Report final result
        log.info("Task id: {}, finished with status: {}, start to report final result", getJobId(),
                finalResult.getStatus());

        // todo wait timeout for upload log file
        log.info("Task id: {}, start to do remained work.", getJobId());
        uploadLogFileToCloudStorage(finalResult);

        Map<String, String> latestLogMeta = finalResult.getLogMetadata();
        if (latestLogMeta != null) {
            this.logMetadata.putAll(latestLogMeta);
        }

        log.info("Task id: {}, remained work be completed, report finished status.", getJobId());

        if (JobUtils.isReportEnabled()) {
            // Report finish signal to task server
            reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES);
        } else {
            waitForTaskResultPulled();
        }
        log.info("Task id: {} exit.", getJobId());
    }

    private void waitForTaskResultPulled() {
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

    private void uploadLogFileToCloudStorage(DefaultTaskResult finalResult) {
        if (cloudObjectStorageService != null && cloudObjectStorageService.supported()
                && JobUtils.isK8sRunModeOfEnv()) {
            LogBiz biz = new LogBizImpl();
            Map<String, String> logMap = null;
            try {
                logMap = biz.uploadLogFileToCloudStorage(finalResult.getJobIdentity(), cloudObjectStorageService);
            } catch (Throwable e) {
                log.warn("Upload job log file to cloud storage occur error, jobId={}", getJobId(), e);
                // putAll will throw NPE if it returns null.
                logMap = new HashMap<>();
            }
            finalResult.setLogMetadata(logMap);
        }
    }

    private void reportTaskResultWithRetry(DefaultTaskResult result, int retries) {
        if (result.getStatus() == JobStatus.DONE) {
            result.setProgress(100.0);
        }
        int retryTimes = 0;
        while (retryTimes++ < retries) {
            try {
                boolean success = reporter.report(JobServerUrls.TASK_UPLOAD_RESULT, result);
                if (success) {
                    log.info("Report task result successfully");
                    break;
                } else {
                    log.warn("Report task result failed, will retry after {} seconds, remaining retries: {}",
                            JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS, retries - retryTimes);
                    Thread.sleep(JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS * 1000L);
                }
            } catch (Throwable e) {
                log.warn("Report task result failed, taskId: {}", getJobId(), e);
            }
        }
    }

    private HeartbeatRequest buildHeartRequest() {
        HeartbeatRequest request = new HeartbeatRequest();
        request.setJobIdentity(getTask().getJobContext().getJobIdentity());
        request.setExecutorEndpoint(JobUtils.getExecutorPoint());
        return request;
    }

    private BaseTask<?> getTask() {
        return task;
    }

    private TaskReporter getReporter() {
        return reporter;
    }

    private Long getJobId() {
        return getTask().getJobContext().getJobIdentity().getId();
    }
}
