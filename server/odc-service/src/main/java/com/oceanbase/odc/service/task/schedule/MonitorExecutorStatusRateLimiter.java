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

import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;

/**
 * @author yaobin
 * @date 2024-02-05
 * @since 4.2.4
 */
public class MonitorExecutorStatusRateLimiter implements StartJobRateLimiter {

    @Override
    public boolean tryAcquire() {
        return isExecutorWaitingToRunNotExceedThreshold();
    }

    private boolean isExecutorWaitingToRunNotExceedThreshold() {
        JobConfiguration jobConfiguration = JobConfigurationHolder.getJobConfiguration();
        TaskFrameworkProperties taskFrameworkProperties = jobConfiguration.getTaskFrameworkProperties();
        long count = jobConfiguration.getTaskFrameworkService().countRunningNeverHeartJobs(
                taskFrameworkProperties.getExecutorWaitingToRunThresholdSeconds());

        return taskFrameworkProperties.getExecutorWaitingToRunThresholdCount() - count > 0;
    }

}
