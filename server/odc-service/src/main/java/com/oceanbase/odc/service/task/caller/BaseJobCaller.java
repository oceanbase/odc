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

import com.oceanbase.odc.common.event.AbstractEvent;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.enums.JobCallerAction;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;
import com.oceanbase.odc.service.task.util.TaskSupervisorUtil;

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
            rows = taskFrameworkService.startSuccess(ji.getId(), executorIdentifier.toString(), context);
            if (rows > 0) {
                afterStartSucceed(executorIdentifier, ji);
            } else {
                afterStartFailed(ji, executorIdentifier,
                        new JobException("Update job status to RUNNING failed, jobId={0}.", ji.getId()));
            }

        } catch (Exception ex) {
            log.info("start job failed, cause={}", ex.getMessage());
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
                finish(ji);
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
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        TaskExecutorClient taskExecutorClient = jobConfiguration.getTaskExecutorClient();

        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorEndpoint = jobEntity.getExecutorEndpoint();
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(jobEntity.getExecutorIdentifier());
        ResourceID resourceID = ResourceIDUtil.getResourceID(identifier, jobEntity);
        try {
            if (executorEndpoint != null
                    && isExecutorExist(identifier, resourceID)) {
                taskExecutorClient.stop(executorEndpoint, ji);
            }
            afterStopSucceed(ji);
        } catch (Exception e) {
            afterStopFailed(ji, e);
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
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        TaskExecutorClient taskExecutorClient = jobConfiguration.getTaskExecutorClient();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        taskExecutorClient.modifyJobParameters(jobEntity.getExecutorEndpoint(), ji, jobParametersJson);
    }

    @Override
    public void finish(JobIdentity ji) throws JobException {
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
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(executorIdentifier);
        ResourceID resourceID = ResourceIDUtil.getResourceID(identifier, jobEntity);
        log.info("Preparing destroy,jobId={}, executorIdentifier={}.", ji.getId(), executorIdentifier);
        doFinish(ji, identifier, resourceID);
        if (TaskSupervisorUtil.isTaskSupervisorEnabled(jobConfiguration.getTaskFrameworkProperties())) {
            jobConfiguration.getSupervisorAgentAllocator().deallocateSupervisorEndpoint(jobEntity.getId());
        }
    }


    @Override
    public boolean canBeFinish(JobIdentity ji) {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobEntity jobEntity = taskFrameworkService.find(ji.getId());
        String executorIdentifier = jobEntity.getExecutorIdentifier();
        if (executorIdentifier == null) {
            return true;
        }
        ExecutorIdentifier identifier = ExecutorIdentifierParser.parser(executorIdentifier);
        ResourceID resourceID = ResourceIDUtil.getResourceID(identifier, jobEntity);
        return canBeFinish(ji, identifier, resourceID);
    }

    /**
     * detect if job on resource id can be finished
     * 
     * @param ji
     * @param ei
     * @param resourceID resource id task working on
     * @return
     */
    protected abstract boolean canBeFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID);

    protected abstract void doFinish(JobIdentity ji, ExecutorIdentifier ei, ResourceID resourceID)
            throws JobException;


    private <T extends AbstractEvent> void publishEvent(T event) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        configuration.getEventPublisher().publishEvent(event);
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

    protected abstract boolean isExecutorExist(ExecutorIdentifier identifier, ResourceID resourceID)
            throws JobException;
}
