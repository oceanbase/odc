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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.logger.LogBiz;
import com.oceanbase.odc.service.task.executor.logger.LogBizImpl;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask<RESULT> implements Task<RESULT> {

    private static final int REPORT_RESULT_RETRY_TIMES = Integer.MAX_VALUE;

    private JobContext context;
    private Map<String, String> jobParameters;
    private TaskReporter reporter;

    private volatile JobStatus status = JobStatus.PREPARING;
    private CloudObjectStorageService cloudObjectStorageService;

    @Override
    public void start(JobContext context) {
        this.context = context;
        this.reporter = new TaskReporter(context.getHostUrls());
        this.jobParameters = Collections.unmodifiableMap(getJobContext().getJobParameters());
        initCloudObjectStorageService();
        updateStatus(JobStatus.RUNNING);
        TaskMonitor taskMonitor = new TaskMonitor(this, this.reporter);
        try {
            taskMonitor.monitor();
            doStart(context);
        } catch (Throwable e) {
            log.info("Task failed, id: {}, details: {}", context.getJobIdentity().getId(), e);
            updateStatus(JobStatus.FAILED);
            onFail(e);
        } finally {
            try {
                doFinal();
            } finally {
                taskMonitor.destroy();
            }
        }
    }

    @Override
    public boolean stop() {
        if (getStatus().isTerminated()) {
            log.warn("Task id: {} is already finished and cannot be canceled",
                    getJobContext().getJobIdentity().getId());
            return true;
        }
        try {
            doStop();
        } catch (Throwable e) {
            log.warn("stop task id : {} failed", getJobContext().getJobIdentity().getId(), e);
            return false;
        }
        updateStatus(JobStatus.CANCELED);
        log.info("Task canceled, id: {}", getJobContext().getJobIdentity().getId());
        return true;
    }


    private void initCloudObjectStorageService() {
        Optional<ObjectStorageConfiguration> storageConfig = JobUtils.getObjectStorageConfiguration();
        storageConfig.ifPresent(osc -> this.cloudObjectStorageService = CloudObjectStorageServiceBuilder.build(osc));
    }

    protected CloudObjectStorageService getCloudObjectStorageService() {
        return cloudObjectStorageService;
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

    protected Map<String, String> getJobParameters() {
        return this.jobParameters;
    }

    private void doFinal() {
        // Report final result
        log.info("Task id: {}, finished with status: {}, start to report final result", getJobId(), getStatus());
        DefaultTaskResult finalResult = DefaultTaskResultBuilder.build(this);

        // todo wait timeout for upload log file
        log.info("Task id: {}, start to do remained work.", getJobId());
        uploadLogFileToCloudStorage(finalResult);

        log.info("Task id: {}, remained work be completed, report finished status.", getJobId());

        // Report finish signal to task server
        reportTaskResultWithRetry(finalResult, REPORT_RESULT_RETRY_TIMES);
        log.info("Task id: {} exit.", getJobId());
    }

    private void uploadLogFileToCloudStorage(DefaultTaskResult finalResult) {
        if (getCloudObjectStorageService() != null && getCloudObjectStorageService().supported()) {
            LogBiz biz = new LogBizImpl();
            Map<String, String> logMap = null;
            try {
                logMap = biz.uploadLogFileToCloudStorage(getJobContext().getJobIdentity(),
                        getCloudObjectStorageService());
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

    private Long getJobId() {
        return getJobContext().getJobIdentity().getId();
    }

    protected abstract void doStart(JobContext context) throws Exception;

    protected abstract void doStop() throws Exception;

    protected abstract void onFail(Throwable e);

}
