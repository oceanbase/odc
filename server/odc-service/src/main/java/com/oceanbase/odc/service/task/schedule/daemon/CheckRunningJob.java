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
package com.oceanbase.odc.service.task.schedule.daemon;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.listener.DestroyExecutorEvent;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-04
 * @since 4.2.4
 */
@Slf4j
@DisallowConcurrentExecution
public class CheckRunningJob implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        if (configuration == null) {
            log.debug("Job configuration is null, abort continue execute.");
            return;
        }
        TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
        int size = taskFrameworkProperties.getSingleFetchJobRowsForCheckHeartTimeout();
        int heartTimeoutPeriod = taskFrameworkProperties.getJobHeartTimeoutSeconds();
        // find heart timeout job
        Page<JobEntity> jobs = getConfiguration().getTaskFrameworkService()
                .findHeartTimeTimeoutJobs(heartTimeoutPeriod, 0, size);
        jobs.forEach(this::handleJobRetryingOrFailed);
    }

    private void handleJobRetryingOrFailed(JobEntity a) {
        getConfiguration().getEventPublisher().publishEvent(new DestroyExecutorEvent(JobIdentity.of(a.getId())));

        if (checkJobIfRetryNecessary(a)) {
            log.info("Need to restart job {}, try to destroy executor.", a.getId());
            int count = getConfiguration().getTaskFrameworkService()
                    .updateStatusDescriptionByIdOldStatusAndExecutorDestroyed(a.getId(), JobStatus.RUNNING,
                            JobStatus.RETRYING, "Heart timeout and retrying job");
            if (count > 0) {
                log.info("Job {} set status to RETRYING.", a.getId());
            }

        } else {
            log.info("No need to restart job {}, try to set status FAILED.", a.getId());
            TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
            int count = getConfiguration().getTaskFrameworkService()
                    .updateStatusToCanceledWhenHeartTimeout(a.getId(),
                            taskFrameworkProperties.getJobHeartTimeoutSeconds(),
                            "Heart timeout and job failed.");
            if (count >= 0) {
                log.info("Set job {} status FAILED accomplished.", a.getId());
            }
        }
    }

    private boolean checkJobIfRetryNecessary(JobEntity je) {
        SingleJobProperties jobProperties = JsonUtils.fromJson(je.getJobPropertiesJson(), SingleJobProperties.class);
        if (jobProperties == null || !jobProperties.isEnableRetryAfterHeartTimeout()) {
            return false;
        }
        int maxRetryTimes = jobProperties.getMaxRetryTimesAfterHeartTimeout() != null
                ? jobProperties.getMaxRetryTimesAfterHeartTimeout()
                : getConfiguration().getTaskFrameworkProperties().getMaxRetryTimesAfterHeartTimeout();

        return maxRetryTimes - je.getExecutionTimes() > 0;
    }


    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
