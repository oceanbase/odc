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
package com.oceanbase.odc.service.task.schedule;

import org.springframework.data.domain.Page;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-03-29
 * @since 4.2.4
 */
@Slf4j
public class DefaultTaskFrameworkDisabledHandler implements TaskFrameworkDisabledHandler {

    @Override
    public void handleJobToFailed() {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkEnabledProperties taskFrameworkProperties = configuration.getTaskFrameworkEnabledProperties();
        if (taskFrameworkProperties.isEnabled()) {
            return;
        }
        Page<JobEntity> jobs = configuration.getTaskFrameworkService().findIncompleteJobs(0,
                configuration.getTaskFrameworkProperties().getSingleFetchCheckHeartTimeoutJobRows());

        jobs.getContent().forEach(j -> {
            try {
                doHandleJobToFailed(configuration, JobIdentity.of(j.getId()));
            } catch (Throwable e) {
                log.warn("Try to handle job to failed occur error, jobId={}", j.getId(), e);
            }
        });

    }

    private void doHandleJobToFailed(JobConfiguration configuration, JobIdentity ji) {
        configuration.getTransactionManager().doInTransactionWithoutResult(() -> {
            JobEntity je = configuration.getTaskFrameworkService()
                    .findWithPessimisticLock(ji.getId());
            if (je.getStatus().isTerminated()) {
                return;
            }

            if (je.getStatus() == JobStatus.RUNNING) {
                try {
                    configuration.getJobDispatcher().destroy(ji);
                } catch (JobException e) {
                    throw new TaskRuntimeException(e);
                }
            }
            if (je.getStatus() == JobStatus.RUNNING &&
                    configuration.getTaskFrameworkService().findFromDatabase(ji.getId())
                            .getExecutorDestroyedTime() == null) {
                log.warn("Job executor has not been destroyed, may not on this machine, jobId={}", ji.getId());
                return;
            }

            configuration.getTaskFrameworkService().updateStatusDescriptionByIdOldStatus(ji.getId(),
                    je.getStatus(), JobStatus.FAILED, "Update status to failed due to task-framework disabled.");
            configuration.getEventPublisher()
                    .publishEvent(new JobTerminateEvent(ji, JobStatus.FAILED));

        });
    }
}
