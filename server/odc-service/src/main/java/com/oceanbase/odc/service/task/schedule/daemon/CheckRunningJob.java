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

import java.util.function.Consumer;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifier;
import com.oceanbase.odc.service.task.caller.ExecutorIdentifierParser;
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
import com.oceanbase.odc.service.task.util.HttpUtil;

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
        handleGeneralHeartTimeoutJobs(size, heartTimeoutPeriod);
        if (taskFrameworkProperties.getRunMode().isProcess()) {
            handleNotLocalAndProcessIsNotExitsJobs(size, heartTimeoutPeriod);
        }
    }

    private void handleGeneralHeartTimeoutJobs(int size, int heartTimeoutPeriod) {
        // find heart timeout job
        Page<JobEntity> jobs = getConfiguration().getTaskFrameworkService()
                .findHeartTimeTimeoutJobs(heartTimeoutPeriod, 0, size);
        jobs.forEach(j -> handleJobRetryingOrFailed(j, a -> {
            try {
                // destroy executor
                getConfiguration().getJobDispatcher().destroy(JobIdentity.of(a.getId()));
            } catch (JobException e) {
                throw new TaskRuntimeException(e);
            }
        }));
    }

    private void handleNotLocalAndProcessIsNotExitsJobs(int size, int heartTimeoutPeriod) {
        // ip has been changed docker restart and process has been interrupted,
        // so we should find jobs which heart timeout and not running in local
        Page<JobEntity> jobs = getConfiguration().getTaskFrameworkService()
                .findHeartTimeTimeoutNotLocalJobs(heartTimeoutPeriod, 0, size);
        jobs.stream().filter(a -> {
            ExecutorIdentifier ei = ExecutorIdentifierParser.parser(a.getExecutorIdentifier());
            return !(HttpUtil.isConnectable(ei.getHost(), ei.getPort(),
                    getConfiguration().getTaskFrameworkProperties().getCheckOdcServerCanBeConnectedTimes()));
        }).forEach(j -> handleJobRetryingOrFailed(j, a -> {
            int rows = getConfiguration().getTaskFrameworkService().updateExecutorToDestroyed(a.getId());
            if (rows > 0) {
                log.info("Executor is not exists, update job executor to destroyed, jobId={}", a.getId());
            } else {
                throw new TaskRuntimeException("update executor to destroyed failed, jobId=" + a.getId());
            }
        }));
    }

    private void handleJobRetryingOrFailed(JobEntity a, Consumer<JobIdentity> jobIdentityConsumer) {
        try {
            getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
                doHandleJobRetryingOrFailed(a, jobIdentityConsumer);
            });
        } catch (Exception e) {
            log.warn("handleJobRetryingOrFailed occur exception:", e);
        }
    }

    private void doHandleJobRetryingOrFailed(JobEntity a, Consumer<JobIdentity> jobIdentityConsumer) {
        if (jobIdentityConsumer != null) {
            jobIdentityConsumer.accept(JobIdentity.of(a.getId()));
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
