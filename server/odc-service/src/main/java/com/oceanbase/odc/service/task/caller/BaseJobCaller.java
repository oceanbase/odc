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
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.enums.JobStatus;
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
            int rows = taskFrameworkService.startSuccess(ji.getId(), executorIdentifier.toString());
            if (rows > 0) {
                afterStartSucceed(executorIdentifier, ji);
            } else {
                afterStartFailed(ji, jobConfiguration, executorIdentifier,
                        new JobException("Update job status to RUNNING failed."));
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
        throw new JobException(ex);
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
        String url = executorEndpoint + String.format(JobUrlConstants.STOP_TASK, ji.getId());
        SuccessResponse<Boolean> response =
                HttpUtil.request(url, new TypeReference<SuccessResponse<Boolean>>() {});
        log.info("Stop job {} response is {}.", ji.getId(), JsonUtils.toJson(response));
        if (response != null && response.getSuccessful() && response.getData()) {
            int rows = jobConfiguration.getTaskFrameworkService()
                    .updateStatusDescriptionByIdOldStatus(ji.getId(),
                            JobStatus.CANCELING, JobStatus.CANCELED, "stop job completed");
            if (rows > 0) {
                log.info("Update job {} status to {}", ji.getId(), JobStatus.CANCELED.name());
            }
            afterStopSucceed(jobConfiguration, ji);
        } else {
            afterStopFailed(ji, new JobException("Stop job response is " + response + ",not succeed"));
        }
    }

    protected void afterStopSucceed(JobConfiguration jobConfiguration, JobIdentity ji) {
        log.info("Stop job {} successfully.", ji.getId());
        jobConfiguration.getEventPublisher().publishEvent(new JobTerminateEvent(ji, JobStatus.CANCELED));
        publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, true, null));
    }

    protected void afterStopFailed(JobIdentity ji, Exception e) throws JobException {
        log.info("Stop job {} failed.", ji.getId());
        publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, false, null));
        throw new JobException("job " + ji.getId() + "be stop failed.", e);
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
        log.info("Preparing destroy job {} executor {}.", ji.getId(), executorIdentifier);

        destroy(ExecutorIdentifierParser.parser(executorIdentifier));
        int rows = taskFrameworkService.updateExecutorToDestroyed(ji.getId());
        if (rows > 0) {
            log.info("Destroy job {} executor {} succeed.", ji.getId(), executorIdentifier);
        }
        publishEvent(new JobCallerEvent(ji, JobCallerAction.DESTROY, true, null));
    }


    private void publishEvent(JobCallerEvent event) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        configuration.getEventPublisher().publishEvent(event);
    }

    @Override
    public void destroy(ExecutorIdentifier identifier) throws JobException {
        doDestroy(identifier);
    }


    protected abstract ExecutorIdentifier doStart(JobContext context) throws JobException;

    protected abstract void doStop(JobIdentity ji) throws JobException;

    protected abstract void doDestroy(ExecutorIdentifier identifier) throws JobException;

}
