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
package com.oceanbase.odc.service.task.base;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.TaskMonitor;
import com.oceanbase.odc.service.task.util.CloudObjectStorageServiceBuilder;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask<RESULT> implements Task<RESULT> {

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private JobContext context;
    private Map<String, String> jobParameters;
    private volatile JobStatus status = JobStatus.PREPARING;
    private CloudObjectStorageService cloudObjectStorageService;

    @Getter
    private TaskMonitor taskMonitor;

    @Override
    public void start(JobContext context) {
        log.info("Start task, id={}.", context.getJobIdentity().getId());

        this.context = context;

        this.jobParameters = Collections.unmodifiableMap(context.getJobParameters());
        log.info("Init task parameters success, id={}.", context.getJobIdentity().getId());

        try {
            initCloudObjectStorageService();
        } catch (Exception e) {
            log.warn("Init cloud object storage service failed, id={}.", getJobId(), e);
        }

        this.taskMonitor = new TaskMonitor(this, cloudObjectStorageService);
        try {
            doInit(context);
            updateStatus(JobStatus.RUNNING);
            taskMonitor.monitor();
            if (doStart(context)) {
                updateStatus(JobStatus.DONE);
            } else {
                updateStatus(JobStatus.FAILED);
            }
        } catch (Throwable e) {
            log.warn("Task failed, id={}.", getJobId(), e);
            updateStatus(JobStatus.FAILED);
        } finally {
            close();
        }
    }

    @Override
    public boolean stop() {
        try {
            if (getStatus().isTerminated()) {
                log.warn("Task is already finished and cannot be canceled, id={}, status={}.", getJobId(), getStatus());
            } else {
                doStop();
                // doRefresh cannot execute if update status to 'canceled'.
                updateStatus(JobStatus.CANCELING);
            }
            return true;
        } catch (Throwable e) {
            log.warn("Stop task failed, id={}", getJobId(), e);
            return false;
        } finally {
            close();
        }
    }

    @Override
    public boolean modify(Map<String, String> jobParameters) {
        if (Objects.isNull(jobParameters) || jobParameters.isEmpty()) {
            log.warn("Job parameter cannot be null, id={}", getJobId());
            return false;
        }
        if (getStatus().isTerminated()) {
            log.warn("Task is already finished, cannot modify parameters, id={}", getJobId());
            return false;
        }
        DefaultJobContext ctx = (DefaultJobContext) getJobContext();
        ctx.setJobParameters(jobParameters);
        this.jobParameters = Collections.unmodifiableMap(jobParameters);
        try {
            afterModifiedJobParameters();
        } catch (Exception e) {
            log.warn("Do after modified job parameters failed", e);
        }
        return true;
    }

    private void initCloudObjectStorageService() {
        Optional<ObjectStorageConfiguration> storageConfig = JobUtils.getObjectStorageConfiguration();
        storageConfig.ifPresent(osc -> this.cloudObjectStorageService = CloudObjectStorageServiceBuilder.build(osc));
    }

    private void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                doClose();
            } catch (Throwable e) {
                // do nothing
            }
            log.info("Task completed, id={}, status={}.", getJobId(), getStatus());
            taskMonitor.finalWork();
        }
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
        log.info("Update task status, id={}, status={}.", getJobId(), status);
        this.status = status;
    }

    protected Map<String, String> getJobParameters() {
        return this.jobParameters;
    }

    private Long getJobId() {
        return getJobContext().getJobIdentity().getId();
    }

    protected abstract void doInit(JobContext context) throws Exception;

    /**
     * start a task return succeed or failed after completed.
     *
     * @return return true if execute succeed, else return false
     */
    protected abstract boolean doStart(JobContext context) throws Exception;

    protected abstract void doStop() throws Exception;

    /**
     * task can release relational resource in this method
     */
    protected abstract void doClose() throws Exception;

    protected void afterModifiedJobParameters() throws Exception {
        // do nothing
    }
}
