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

import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.schedule.ExecutorIdentifier;
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

    public DefaultJobCallerListener(JobScheduler jobScheduler) {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        this.taskFrameworkService = configuration.getTaskFrameworkService();
    }

    @Override
    protected void startSucceed(JobIdentity ji, ExecutorIdentifier identifier) {
        if (taskFrameworkService != null) {
            taskFrameworkService.startSuccess(ji.getId(), identifier.toString());
        }
    }

    @Override
    protected void startFailed(JobIdentity ji, Exception ex) {

    }

    @Override
    protected void stopSucceed(JobIdentity ji) {

    }

    @Override
    protected void stopFailed(JobIdentity ji, Exception ex) {
        if (taskFrameworkService != null) {
            String desc = "Try to cancel job failed.";
            taskFrameworkService.updateDescription(ji.getId(), desc);
            log.info("Stop job " + ji.getId() + " failed, error is: ", ex);
        }
    }
}
