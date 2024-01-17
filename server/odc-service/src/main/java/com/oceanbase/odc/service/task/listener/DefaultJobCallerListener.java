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

package com.oceanbase.odc.service.task.listener;

import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Slf4j
public class DefaultJobCallerListener extends JobCallerListener {

    private final TaskFrameworkService taskFrameworkService;
    private final JobConfiguration configuration;

    public DefaultJobCallerListener(JobScheduler jobScheduler) {
        JobConfigurationValidator.validComponent();
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        this.taskFrameworkService = configuration.getTaskFrameworkService();
        this.configuration = configuration;
    }

    @Override
    protected void startSucceed(JobIdentity ji, ExecutorIdentifier identifier) {
        try {
            taskFrameworkService.startSuccess(ji.getId(), identifier.toString());
        } catch (Exception ex) {
            // if transaction timeout, we should destroy executor
            try {
                configuration.getJobDispatcher().destroy(identifier);
            } catch (JobException e) {
                // if destroy failed, domain job will destroy it
                log.warn("Destroy executor {} occur exception", identifier.toString());
            }
            throw ex;
        }
    }

    @Override
    protected void startFailed(JobIdentity ji, Exception ex) {

    }

    @Override
    protected void stopSucceed(JobIdentity ji) {
        int rows = taskFrameworkService.updateStatusDescriptionByIdOldStatus(ji.getId(), JobStatus.CANCELING,
                JobStatus.CANCELED, "cancel job completed");
        if (rows > 0) {
            log.info("Update job {} status to {}", ji.getId(), JobStatus.CANCELED.name());
        }
    }

    @Override
    protected void stopFailed(JobIdentity ji, Exception ex) {

    }

    @Override
    protected void destroySucceed(JobIdentity ji) {
        int rows = taskFrameworkService.updateExecutorToDestroyed(ji.getId());
        if (rows > 0) {
            log.info("Destroy job {} executor succeed.", ji.getId());
        }
    }
}
