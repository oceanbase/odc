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
import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.constants.JobUrlConstants;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
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
        int rows = taskFrameworkService.beforeStart(ji.getId());
        if (rows <= 0) {
            throw new JobException("Start job failed, jobId={0}", ji.getId());
        }
        try {
            executorIdentifier = doStart(context);
            rows = taskFrameworkService.startSuccess(ji.getId(), executorIdentifier.toString());
            if (rows > 0) {
                afterStartSucceed(executorIdentifier, ji);
            } else {
                afterStartFailed(ji, executorIdentifier,
                        new JobException("Update job status to RUNNING failed, jobId={0}.", ji.getId()));
            }

        } catch (Exception ex) {
            afterStartFailed(ji, executorIdentifier, ex);
        }
    }

    private void afterStartSucceed(ExecutorIdentifier executorIdentifier, JobIdentity ji) {
        log.info("Start job succeed, jobId={}.", ji.getId());
        publishEvent(new JobCallerEvent(ji, JobCallerAction.START, true, executorIdentifier, null));
    }

    private void afterStartFailed(JobIdentity ji,
            ExecutorIdentifier executorIdentifier, Exception ex) throws JobException {
        if (executorIdentifier != null) {
            try {
                destroy(ji);
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
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorEndpoint = jobEntity.getExecutorEndpoint();
        try {
            if (executorEndpoint != null
                    && isExecutorExist(ExecutorIdentifierParser.parser(jobEntity.getExecutorIdentifier()))) {
                tryStop(ji, executorEndpoint);
            }
            afterStopSucceed(ji);
        } catch (Exception e) {
            afterStopFailed(ji, e);
        }
    }


    private void tryStop(JobIdentity ji, String executorEndpoint)
            throws IOException, JobException {

        String url = executorEndpoint + String.format(JobUrlConstants.STOP_TASK, ji.getId());
        log.info("Try stop job {} in executor {}.", ji.getId(), url);
        SuccessResponse<Boolean> response =
                HttpUtil.request(url, new TypeReference<SuccessResponse<Boolean>>() {});
        if (response != null && response.getSuccessful() && response.getData()) {
            log.info("Stop job {} in executor succeed, response is {}.", ji.getId(), JsonUtils.toJson(response));
        } else {
            throw new JobException("Stop job response not succeed, response={0}", JsonUtils.toJson(response));
        }
    }

    protected void afterStopSucceed(JobIdentity ji) {
        log.info("Stop job successfully, jobId={}.", ji.getId());
        publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, true, null));
    }

    protected void afterStopFailed(JobIdentity ji, Exception e) throws JobException {
        log.info("Stop job failed,jobId={}.", ji.getId());
        publishEvent(new JobCallerEvent(ji, JobCallerAction.STOP, false, null));
        throw new JobException("job be stop failed, jobId={0}.", e, ji.getId());
    }

    @Override
    public void modify(JobIdentity ji, String jobParametersJson) throws JobException {
        JobConfigurationValidator.validComponent();
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorEndpoint = getExecutorPoint(jobEntity);
        String url = executorEndpoint + String.format(JobUrlConstants.MODIFY_JOB_PARAMETERS, ji.getId());
        log.info("Try to modify job parameters, jobId={}.", ji.getId());
        try {
            SuccessResponse<Boolean> response =
                    HttpUtil.request(url, jobParametersJson, new TypeReference<SuccessResponse<Boolean>>() {});
            if (response != null && response.getSuccessful() && response.getData()) {
                log.info("Modify job parameters success, jobId={}, response={}.", ji.getId(),
                        JsonUtils.toJson(response));
            } else {
                throw new JobException("Modify job parameters not succeed, jobId={0}, response={1}", ji.getId(),
                        response);
            }
        } catch (IOException e) {
            throw new JobException("Modify job parameters not succeed, jobId={0}, response={1}", ji.getId(),
                    JsonUtils.toJson(e.getMessage()));
        }
    }

    private String getExecutorPoint(JobEntity jobEntity)
            throws JobException {
        String executorEndpoint = jobEntity.getExecutorEndpoint();
        if (executorEndpoint == null) {
            throw new JobException("Executor point is null, cannot modify executor point, jobId={}", jobEntity.getId());
        }
        return executorEndpoint;
    }

    @Override
    public void destroy(JobIdentity ji) throws JobException {
        JobConfigurationValidator.validComponent();
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorIdentifier = jobEntity.getExecutorIdentifier();
        if (jobEntity.getExecutorDestroyedTime() != null) {
            return;
        }
        if (executorIdentifier == null) {
            updateExecutorDestroyed(ji);
            return;
        }
        log.info("Preparing destroy,jobId={}, executorIdentifier={}.", ji.getId(), executorIdentifier);
        doDestroy(ji, ExecutorIdentifierParser.parser(executorIdentifier));
    }

    protected abstract void doDestroy(JobIdentity ji, ExecutorIdentifier ei) throws JobException;

    private <T extends AbstractEvent> void publishEvent(T event) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        configuration.getEventPublisher().publishEvent(event);
    }

    protected void destroyInternal(ExecutorIdentifier identifier) throws JobException {
        if (identifier == null || identifier.getExecutorName() == null) {
            return;
        }
        doDestroyInternal(identifier);
    }

    protected void updateExecutorDestroyed(JobIdentity ji) throws JobException {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        int rows = taskFrameworkService.updateExecutorToDestroyed(ji.getId());
        if (rows > 0) {
            log.info("Destroy job executor succeed, jobId={}.", ji.getId());
        } else {
            throw new JobException("Update executor to destroyed failed, JodId={0}", ji.getId());
        }
    }

    protected abstract ExecutorIdentifier doStart(JobContext context) throws JobException;

    protected abstract void doStop(JobIdentity ji) throws JobException;

    protected abstract void doDestroyInternal(ExecutorIdentifier identifier) throws JobException;

    protected abstract boolean isExecutorExist(ExecutorIdentifier identifier) throws JobException;

}
