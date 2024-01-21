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
package com.oceanbase.odc.service.task.executor.server;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobParametersKeyConstants;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.logger.LogBiz;
import com.oceanbase.odc.service.task.executor.logger.LogBizImpl;
import com.oceanbase.odc.service.task.executor.task.DefaultTaskResult;
import com.oceanbase.odc.service.task.executor.task.DefaultTaskResultBuilder;
import com.oceanbase.odc.service.task.executor.task.Task;
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
    private final TaskReporter reporter;
    private final Task<?> task;
    private final CloudObjectStorageService cloudObjectStorageService;
    private volatile long startTimeMilliSeconds;
    private ScheduledExecutorService reportScheduledExecutor;
    private ScheduledExecutorService heartScheduledExecutor;


    public TaskMonitor(Task<?> task, TaskReporter reporter, CloudObjectStorageService cloudObjectStorageService) {
        this.task = task;
        this.reporter = reporter;
        this.cloudObjectStorageService = cloudObjectStorageService;
    }

    public void monitor() {
        this.startTimeMilliSeconds = System.currentTimeMillis();

        ThreadFactory threadFactory =
                new TraceDecoratorThreadFactory(new TaskThreadFactory(("Task-Monitor-Job-" + getJobId())));
        this.reportScheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        reportScheduledExecutor.scheduleAtFixedRate(() -> {
            if (isTimeout()) {
                getTask().stop();
            }
            try {
                reportTaskResult();
            } catch (Throwable e) {
                log.warn("Update task info failed, id: {}", getJobId(), e);
            }
        }, 1, JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task monitor init success");

        heartScheduledExecutor = Executors.newSingleThreadScheduledExecutor(
                new TaskThreadFactory(("Task-Heart-Job-" + getJobId())));

        heartScheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                getReporter().report(JobUrlConstants.TASK_HEART, buildHeartRequest());
            } catch (Throwable e) {
                log.warn("Update heart info failed, id: {}", getJobId(), e);
            }
        }, 1, JobConstants.REPORT_TASK_HEART_INTERVAL_SECONDS, TimeUnit.SECONDS);
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
        DefaultTaskResult taskResult = DefaultTaskResultBuilder.build(getTask());
        DefaultTaskResult copiedResult = ObjectUtil.deepCopy(taskResult, DefaultTaskResult.class);
        if (copiedResult.getStatus().isTerminated()) {
            log.info("job {} status {} is terminate, monitor report be ignored.",
                    copiedResult.getJobIdentity().getId(), copiedResult.getStatus());
            return;
        }
        getReporter().report(JobUrlConstants.TASK_RESULT_UPLOAD, copiedResult);
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}", getJobId(),
                copiedResult.getStatus(), String.format("%.2f", copiedResult.getProgress()), getTask().getTaskResult());
    }

    private boolean isTimeout() {
        String milliSecStr =
                getTask().getJobContext().getJobParameters()
                        .get(JobParametersKeyConstants.TASK_EXECUTION_TIMEOUT_MILLIS);

        if (milliSecStr != null) {
            return System.currentTimeMillis() - startTimeMilliSeconds > Long.parseLong(milliSecStr);
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

        log.info("Task id: {}, remained work be completed, report finished status.", getJobId());

        // Report finish signal to task server
        reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES);
        log.info("Task id: {} exit.", getJobId());
    }

    private void uploadLogFileToCloudStorage(DefaultTaskResult finalResult) {
        if (cloudObjectStorageService != null && cloudObjectStorageService.supported()) {
            LogBiz biz = new LogBizImpl();
            Map<String, String> logMap = null;
            try {
                logMap = biz.uploadLogFileToCloudStorage(finalResult.getJobIdentity(),
                        cloudObjectStorageService);
            } catch (Throwable e) {
                log.warn("Upload job {} log file to cloud storage occur error", getJobId(), e);
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
                boolean success = reporter.report(JobUrlConstants.TASK_RESULT_UPLOAD, result);
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

    private HeartRequest buildHeartRequest() {
        HeartRequest request = new HeartRequest();
        request.setJobIdentity(getTask().getJobContext().getJobIdentity());
        request.setExecutorEndpoint(JobUtils.getExecutorPoint());
        return request;
    }

    private Task<?> getTask() {
        return task;
    }

    private TaskReporter getReporter() {
        return reporter;
    }

    private Long getJobId() {
        return getTask().getJobContext().getJobIdentity().getId();
    }
}
