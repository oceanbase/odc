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

package com.oceanbase.odc.service.task.executor.task;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.task.TaskThreadFactory;
import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobAttributeKeyConstants;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask implements Task {

    private static final int REPORT_TASK_INFO_INTERVAL_SECONDS = 5;
    private static final int REPORT_RESULT_RETRY_TIMES = 10;
    private static final int REPORT_RESULT_RETRY_INTERVAL_SECONDS = 10;
    private static final int DEFAULT_TASK_TIMEOUT_MILLI_SECONDS = 48 * 60 * 60 * 1000;

    private JobContext context;
    private Map<String, String> jobData;
    private TaskReporter reporter;

    private volatile JobStatus status = JobStatus.PREPARING;
    private volatile boolean canceled = false;
    private volatile long startTimeMilliSeconds;

    private CloudObjectStorageService cloudObjectStorageService;

    @Override
    public void start(JobContext context) {
        this.startTimeMilliSeconds = System.currentTimeMillis();
        this.context = context;
        try {
            this.jobData = Collections.unmodifiableMap(getJobContext().getJobParameters());
            initCloudObjectStorageService();
            this.reporter = new TaskReporter(context.getHostUrls());
            onInit();
            initTaskMonitor();
            onStart();
        } catch (Exception e) {
            log.info("Task failed, id: {}, details: {}", context.getJobIdentity().getId(), e);
            updateStatus(JobStatus.FAILED);
            onFail(e);
        } finally {
            doFinal();
        }
    }

    @Override
    public void stop() {
        if (getStatus().isTerminated()) {
            log.warn("Task is already finished and cannot be canceled, id: {}",
                    getJobContext().getJobIdentity().getId());
            return;
        }
        canceled = true;
        onStop();
        updateStatus(JobStatus.CANCELED);
        log.info("Task canceled, id: {}", getJobContext().getJobIdentity().getId());
    }


    @Override
    public JobStatus getStatus() {
        return status;
    }

    @Override
    public JobContext getJobContext() {
        return context;
    }

    protected void updateStatus(JobStatus status) {
        this.status = status;
    }

    protected Map<String, String> getJobData() {
        return this.jobData;
    }

    protected boolean isCanceled() {
        return canceled;
    }

    private boolean isTimeout() {
        String milliSecStr = getJobData().get(JobDataMapConstants.TIMEOUT_MILLI_SECONDS);
        long milliSec = milliSecStr != null ? Long.parseLong(milliSecStr) : DEFAULT_TASK_TIMEOUT_MILLI_SECONDS;
        return System.currentTimeMillis() - startTimeMilliSeconds > milliSec;
    }

    private void initTaskMonitor() {
        ThreadFactory threadFactory =
                new TaskThreadFactory(("Task-Monitor-" + getJobId()));
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            // check task is timeout or not
            if (isTimeout()) {
                // when task execution is timeout then stop it.
                stop();
            }
            if (getStatus().isTerminated()) {
                scheduledExecutor.shutdown();
            } else {
                try {
                    reportTaskResult();
                } catch (Exception e) {
                    log.warn("Update task info failed, id: {}", getJobContext().getJobIdentity().getId(), e);
                }
            }
        }, 1, REPORT_TASK_INFO_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("Task monitor init success");
    }

    private void doFinal() {
        // Report final result
        log.info("Task id: {}, finished with status: {}, start to report final result", getJobId(), getStatus());
        DefaultTaskResult finalResult = buildCurrentResult();

        // todo wait timeout for upload log file
        log.info("Task id: {}, start to do remained work.", getJobId());
        uploadLogFileToCloudStorage(finalResult);

        log.info("Task id: {}, remained work be completed, report finished status.", getJobId());
        // Report finish signal to task server
        reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES, REPORT_RESULT_RETRY_INTERVAL_SECONDS);
        log.info("Task id: {} exit.", getJobId());
    }

    private void uploadLogFileToCloudStorage(DefaultTaskResult finalResult) {
        if (Objects.isNull(getCloudObjectStorageService()) || !getCloudObjectStorageService().supported()) {
            return;
        }
        log.info("Task id: {}, upload log", getJobId());
        String jobLog = LogUtils.getJobLogFileWithPath(getJobId(), OdcTaskLogLevel.ALL);
        String fileId = StringUtils.uuid();
        File jobLogFile = new File(jobLog);
        if (!jobLogFile.exists()) {
            return;
        }
        try {
            String objectName = getCloudObjectStorageService().uploadTemp(fileId, jobLogFile);
            if (finalResult.getLogMetadata() == null) {
                finalResult.setLogMetadata(new HashMap<>());
            }
            finalResult.getLogMetadata().put(JobAttributeKeyConstants.LOG_ALL_OBJECT_ID, objectName);
            finalResult.getLogMetadata().put(JobAttributeKeyConstants.LOG_WARN_OBJECT_ID, objectName);
            finalResult.getLogMetadata().put(JobAttributeKeyConstants.OSS_BUCKET_NAME,
                    getCloudObjectStorageService().getBucketName());
            log.info("upload task log to OSS successfully, file name={}", fileId);
        } catch (Exception exception) {
            log.warn("upload task log to OSS failed, file name={}", fileId);
        }

    }

    private void initCloudObjectStorageService() {
        if (getJobData().get(JobDataMapConstants.OBJECT_STORAGE_CONFIGURATION) != null) {
            ObjectStorageConfiguration storageConfig = JsonUtils.fromJson(
                    getJobData().get(JobDataMapConstants.OBJECT_STORAGE_CONFIGURATION),
                    ObjectStorageConfiguration.class);
            this.cloudObjectStorageService = CloudObjectStorageServiceBuilder.build(storageConfig);
        }
    }

    protected CloudObjectStorageService getCloudObjectStorageService() {
        return cloudObjectStorageService;
    }

    private void reportTaskResult() {
        double progress = getProgress();
        if (new BigDecimal(progress).compareTo(new BigDecimal("100.0")) > 0) {
            log.warn("progress value {} is illegal, bigger than 100.0", progress);
            return;
        }
        // onUpdate();
        if (getStatus() == JobStatus.DONE) {
            progress = 100.0;
        }
        reporter.report(buildCurrentResult());
        log.info("Report task info, id: {}, status: {}, progress: {}%, result: {}",
                getJobContext().getJobIdentity().getId(),
                getStatus(), String.format("%.2f", progress), getTaskResult());
    }

    private void reportTaskResultWithRetry(TaskResult result, int retries, int intervalSeconds) {
        int retryTimes = 0;
        while (retryTimes < retries) {
            try {
                retryTimes++;
                boolean success = reporter.report(result);
                if (success) {
                    log.info("Report task result successfully");
                    break;
                } else {
                    log.warn("Report task result failed, will retry after {} seconds, remaining retries: {}",
                            intervalSeconds, retries - retryTimes);
                    Thread.sleep(intervalSeconds * 1000L);
                }
            } catch (Exception e) {
                log.warn("Report task result failed, taskId: {}", getJobId(), e);
            }
        }
    }

    private DefaultTaskResult buildCurrentResult() {
        DefaultTaskResult result = new DefaultTaskResult();
        result.setResultJson(JsonUtils.toJson(getTaskResult()));
        result.setStatus(getStatus());
        result.setProgress(getProgress());
        result.setJobIdentity(getJobContext().getJobIdentity());
        result.setExecutorEndpoint("http://" + SystemUtils.getLocalIpAddress() + ":" + JobUtils.getPort());
        return result;
    }

    private Long getJobId() {
        return getJobContext().getJobIdentity().getId();
    }

    protected abstract void onInit();

    protected abstract void onStart();

    protected abstract void onStop();

    protected abstract void onFail(Exception e);

}
