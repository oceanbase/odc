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
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
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
        JobConfigurationValidator.validComponent();
        TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
        int size = taskFrameworkProperties.getSingleFetchCheckHeartTimeoutJobRows();
        int heartTimeoutPeriod = taskFrameworkProperties.getJobHeartTimeoutSeconds();
        // find heart timeout job
        Page<JobEntity> jobs = getConfiguration().getTaskFrameworkService()
                .findHeartTimeTimeoutJobs(heartTimeoutPeriod, 0, size);
        jobs.forEach(this::handleJobRetryingOrCanceled);

    }

    private void handleJobRetryingOrCanceled(JobEntity a) {
        getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
            doHandleJobRetryingOrFailed(a);
        });

    }

    private void doHandleJobRetryingOrFailed(JobEntity a) {
        // destroy executor
        try {
            getConfiguration().getJobDispatcher().destroy(JobIdentity.of(a.getId()));
        } catch (JobException e) {
            throw new TaskRuntimeException(e);
        }

        if (checkJobIfRetryNecessary(a)) {
            log.info("Need to restart job, destroy old executor completed, jobId={}.", a.getId());
            int rows = getConfiguration().getTaskFrameworkService()
                    .updateStatusDescriptionByIdOldStatusAndExecutorDestroyed(a.getId(), JobStatus.RUNNING,
                            JobStatus.RETRYING, "Heart timeout and retrying job");
            if (rows > 0) {
                log.info("Job {} set status to RETRYING.", a.getId());
            }

        } else {
            log.info("No need to restart job, try to set status to FAILED, jobId={}.", a.getId());
            TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
            int rows = getConfiguration().getTaskFrameworkService()
                    .updateStatusToCanceledWhenHeartTimeout(a.getId(),
                            taskFrameworkProperties.getJobHeartTimeoutSeconds(),
                            "Heart timeout and set job to status FAILED.");
            if (rows >= 0) {
                getConfiguration().getEventPublisher().publishEvent(
                        new JobTerminateEvent(JobIdentity.of(a.getId()), JobStatus.FAILED));
                log.info("Set job status to FAILED accomplished, jobId={}.", a.getId());
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
                : getConfiguration().getTaskFrameworkProperties().getMaxHeartTimeoutRetryTimes();

        return maxRetryTimes - je.getExecutionTimes() > 0;
    }


    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
