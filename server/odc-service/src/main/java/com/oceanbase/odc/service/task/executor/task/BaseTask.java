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
import java.util.Objects;
import java.util.Optional;

import com.oceanbase.odc.service.objectstorage.cloud.CloudObjectStorageService;
import com.oceanbase.odc.service.objectstorage.cloud.model.ObjectStorageConfiguration;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.server.TaskMonitor;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/11/22 20:16
 */
@Slf4j
public abstract class BaseTask<RESULT> implements Task<RESULT> {

    private JobContext context;
    private Map<String, String> jobParameters;
    private volatile JobStatus status = JobStatus.PREPARING;
    private CloudObjectStorageService cloudObjectStorageService;

    @Override
    public void start(JobContext context) {
        this.context = context;
        this.jobParameters = Collections.unmodifiableMap(context.getJobParameters());
        initCloudObjectStorageService();
        TaskMonitor taskMonitor = new TaskMonitor(this, cloudObjectStorageService);
        try {
            doInit(context);
            updateStatus(JobStatus.RUNNING);
            taskMonitor.monitor();
            doStart(context);
            updateStatus(JobStatus.DONE);
        } catch (Throwable e) {
            log.info("Task failed, id={}.", context.getJobIdentity().getId(), e);
            updateStatus(JobStatus.FAILED);
            onFail(e);
        } finally {
            try {
                doFinal();
            } catch (Throwable e) {
                // do nothing
            }
            log.info("Task be completed, id={}, status={}.", getJobId(), getStatus());
            taskMonitor.finalWork();
        }
    }

    @Override
    public boolean stop() {
        if (getStatus().isTerminated()) {
            log.warn("Task is already finished and cannot be canceled, id={}",
                    getJobContext().getJobIdentity().getId());
            return true;
        }
        try {
            doStop();
        } catch (Throwable e) {
            log.warn("stop task failed, id={}", getJobContext().getJobIdentity().getId(), e);
            return false;
        }
        updateStatus(JobStatus.CANCELED);
        log.info("Task be canceled, id={}", getJobContext().getJobIdentity().getId());
        return true;
    }

    @Override
    public boolean modify(Map<String, String> jobParameters) {
        if (Objects.isNull(jobParameters) || jobParameters.isEmpty()) {
            log.warn("Job parameter cannot be null, id={}",
                    getJobContext().getJobIdentity().getId());
            return false;
        }
        if (getStatus().isTerminated()) {
            log.warn("Task is already finished, cannot modify parameters, id={}",
                    getJobContext().getJobIdentity().getId());
            return false;
        }
        DefaultJobContext ctx = (DefaultJobContext) getJobContext();
        // change the value in job context
        ctx.setJobParameters(jobParameters);
        this.jobParameters = Collections.unmodifiableMap(jobParameters);
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

    private Long getJobId() {
        return getJobContext().getJobIdentity().getId();
    }

    protected abstract void doInit(JobContext context) throws Exception;

    protected abstract void doStart(JobContext context) throws Exception;

    protected abstract void doStop() throws Exception;

    // this method be invoked finally to release resource
    protected void doFinal() {};

    protected abstract void onFail(Throwable e);

}
