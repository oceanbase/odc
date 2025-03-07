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
package com.oceanbase.odc.service.task.supervisor;

import java.text.MessageFormat;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.listener.JobCallerEvent;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/11/28 17:37
 */
@Slf4j
public class DefaultJobEventListener implements JobEventHandler {
    @Override
    public void beforeStartJob(JobContext context) throws JobException {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        JobIdentity ji = context.getJobIdentity();
        int rows = taskFrameworkService.beforeStart(ji.getId());
        if (rows <= 0) {
            throw new JobException("Start job failed, jobId={0}", ji.getId());
        }
    }

    @Override
    public void afterStartJob(ExecutorEndpoint executorIdentifier, JobContext jobContext) throws JobException {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        int rows = taskFrameworkService.startSuccess(jobContext.getJobIdentity().getId(),
                executorIdentifier.getIdentifier(), jobContext);
        if (rows <= 0) {
            throw new JobException("Update job status to RUNNING failed, jobId={0}.",
                    jobContext.getJobIdentity().getId());
        }

    }

    @Override
    public void afterFinished(ExecutorEndpoint executorIdentifier, JobContext jobContext) throws JobException {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkService taskFrameworkService = jobConfiguration.getTaskFrameworkService();
        int rows = taskFrameworkService.updateExecutorToDestroyed(jobContext.getJobIdentity().getId());
        if (rows > 0) {
            log.info("Destroy job executor succeed, jobId={}.", jobContext.getJobIdentity().getId());
        } else {
            throw new JobException("Update executor to destroyed failed, JodId={0}",
                    jobContext.getJobIdentity().getId());
        }
    }

    @Override
    public void finishFailed(ExecutorEndpoint executorIdentifier, JobContext jobContext) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        JobEntity jobEntity = configuration.getTaskFrameworkService().find(jobContext.getJobIdentity().getId());
        if (jobEntity.getStatus() == JobStatus.RUNNING) {
            // Cannot connect to target identifier,we cannot kill the process,
            // so we set job to FAILED and avoid two process running
            configuration.getTaskFrameworkService().updateStatusDescriptionByIdOldStatus(
                    jobContext.getJobIdentity().getId(), JobStatus.RUNNING, JobStatus.FAILED,
                    MessageFormat.format("Cannot connect to target odc server, jodId={0}, identifier={1}",
                            jobContext.getJobIdentity().getId(), executorIdentifier));
        }
    }

    @Override
    public void onNewEvent(JobCallerEvent jobCallerEvent) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        configuration.getEventPublisher().publishEvent(jobCallerEvent);
    }
}
