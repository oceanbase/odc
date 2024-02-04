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

package com.oceanbase.odc.service.task.caller;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.validate.ValidatorUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.HttpUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-16
 * @since 4.2.4
 */
@Slf4j
public abstract class BaseJobCaller implements JobCaller {

    @Override
    public void start(JobContext context) throws JobException {
        JobConfigurationValidator.validComponent();
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        ExecutorIdentifier executorIdentifier = null;
        JobIdentity ji = context.getJobIdentity();
        try {
            executorIdentifier = doStart(context);
            ValidatorUtils.verifyField(executorIdentifier);
            int rows = taskFrameworkService.startSuccess(ji.getId(), executorIdentifier.toString());
            if (rows > 0) {
                afterStartSucceed(executorIdentifier, ji);
            } else {
                afterStartFailed(ji, jobConfiguration, executorIdentifier,
                        new JobException("Update job status to RUNNING failed, jobId={0}.", ji.getId()));
            }

        } catch (Exception ex) {
            afterStartFailed(ji, jobConfiguration, executorIdentifier, ex);
        }
    }

    private void afterStartSucceed(ExecutorIdentifier executorIdentifier, JobIdentity ji) {
        log.info("Start job {} succeed.", ji.getId());
        publishEvent(new JobCallerEvent(ji, JobCallerAction.START, true, executorIdentifier, null));
    }

    private void afterStartFailed(JobIdentity ji, JobConfiguration jobConfiguration,
            ExecutorIdentifier executorIdentifier, Exception ex) throws JobException {
        if (executorIdentifier != null) {
            try {
                jobConfiguration.getJobDispatcher().destroy(executorIdentifier);
            } catch (JobException e) {
                // if destroy failed, domain job will destroy it
                log.warn("Destroy executor {} occur exception", executorIdentifier);
            }
        }
        publishEvent(new JobCallerEvent(ji, JobCallerAction.START, false, ex));
        throw new JobException("Start job failed", ex);
    }

    @Override
    public void stop(JobIdentity ji) throws JobException {
        JobConfigurationValidator.validComponent();
        // send stop to executor
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorEndpoint = jobEntity.getExecutorEndpoint();

        try {
            if (executorEndpoint != null) {
                tryStop(jobConfiguration, ji, executorEndpoint);
            } else {
                afterStopSucceed(jobConfiguration, ji);
            }

        } catch (Exception e) {
            afterStopFailed(ji, e);
        }
    }

    private void tryStop(JobConfiguration jobConfiguration, JobIdentity ji, String executorEndpoint)
            throws IOException, JobException {
        // For transaction atomic, first update to CANCELED, then stop remote job in executor,
        // if stop remote failed, transaction will be rollback
        int rows = jobConfiguration.getTaskFrameworkService()
                .updateStatusDescriptionByIdOldStatus(ji.getId(),
                        JobStatus.CANCELING, JobStatus.CANCELED, "stop job completed");
        if (rows <= 0) {
            log.info("Update job {} status to CANCELED failed ", ji.getId());
        }
        String url = executorEndpoint + String.format(JobUrlConstants.STOP_TASK, ji.getId());
        log.info("Try stop job {} in executor {}.", ji.getId(), url);
        SuccessResponse<Boolean> response =
                HttpUtil.request(url, new TypeReference<SuccessResponse<Boolean>>() {});
        log.info("Stop job {} in executor, response is {}.", ji.getId(), JsonUtils.toJson(response));
        if (response != null && response.getSuccessful() && response.getData()) {
            afterStopSucceed(jobConfiguration, ji);
        } else {
            afterStopFailed(ji,
                    new JobException("Stop job response not succeed, response={0}", JsonUtils.toJson(response)));
        }
    }

    protected void afterStopSucceed(JobConfiguration jobConfiguration, JobIdentity ji) {
        log.info("Stop job {}, set status to CANCELED successfully.", ji.getId());
        jobConfiguration.getEventPublisher().publishEvent(new JobTerminateEvent(ji, JobStatus.CANCELED));
        publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, true, null));
    }

    protected void afterStopFailed(JobIdentity ji, Exception e) throws JobException {
        log.info("Stop job {} failed.", ji.getId());
        publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, false, null));
        throw new JobException("job be stop failed, jobId={0}.", e, ji.getId());
    }

    @Override
    public void destroy(JobIdentity ji) throws JobException {
        JobConfigurationValidator.validComponent();
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorIdentifier = jobEntity.getExecutorIdentifier();
        if (executorIdentifier == null) {
            return;
        }
        log.info("Preparing destroy,jobId={}, executorIdentifier={}.", ji.getId(), executorIdentifier);

        destroy(ExecutorIdentifierParser.parser(jobEntity.getRunMode(), executorIdentifier));
        int rows = taskFrameworkService.updateExecutorToDestroyed(ji.getId());
        if (rows > 0) {
            log.info("Destroy job {} executor {} succeed.", ji.getId(), executorIdentifier);
            publishEvent(new JobCallerEvent(ji, JobCallerAction.DESTROY, true, null));
        } else {
            throw new JobException("update executor to destroyed failed, executor={0}", executorIdentifier);
        }
    }


    private void publishEvent(JobCallerEvent event) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        configuration.getEventPublisher().publishEvent(event);
    }

    @Override
    public void destroy(ExecutorIdentifier identifier) throws JobException {
        if (identifier == null || identifier.getExecutorName() == null) {
            return;
        }
        doDestroy(identifier);
    }


    protected abstract ExecutorIdentifier doStart(JobContext context) throws JobException;

    protected abstract void doStop(JobIdentity ji) throws JobException;

    protected abstract void doDestroy(ExecutorIdentifier identifier) throws JobException;

}
