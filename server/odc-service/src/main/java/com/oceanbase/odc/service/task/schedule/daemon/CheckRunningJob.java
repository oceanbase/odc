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
import com.oceanbase.odc.common.util.SilentExecutor;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
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
        int heartTimeoutSeconds = taskFrameworkProperties.getJobHeartTimeoutSeconds();
        // find heart timeout job
        Page<JobEntity> jobs = getConfiguration().getTaskFrameworkService()
                .findHeartTimeTimeoutJobs(heartTimeoutSeconds, 0,
                        getFetchRowSize(taskFrameworkProperties.getRunMode()));
        jobs.forEach(this::handleJobRetryingOrFailed);

    }

    private int getFetchRowSize(TaskRunMode taskRunMode) {
        TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
        return taskRunMode.isProcess() ? taskFrameworkProperties.getSingleMaxFetchCheckHeartTimeoutJobRows()
                : taskFrameworkProperties.getSingleFetchCheckHeartTimeoutJobRows();
    }

    private void handleJobRetryingOrFailed(JobEntity jobEntity) {
        SilentExecutor.executeSafely(() -> getConfiguration().getTransactionManager()
                .doInTransactionWithoutResult(() -> doHandleJobRetryingOrFailed(jobEntity)));
    }

    private void doHandleJobRetryingOrFailed(JobEntity jobEntity) {
        JobEntity a = getConfiguration().getTaskFrameworkService().findWithPessimisticLock(jobEntity.getId());
        // destroy executor
        try {
            getConfiguration().getJobDispatcher().destroy(JobIdentity.of(a.getId()));
        } catch (JobException e) {
            throw new TaskRuntimeException(e);
        }

        JobEntity checkedEntity = getConfiguration().getTaskFrameworkService().find(jobEntity.getId());
        if (checkedEntity.getStatus() == JobStatus.FAILED) {
            log.info("Job has been FAILED, jobId={}", jobEntity.getId());
            return;
        }
        if (checkedEntity.getExecutorDestroyedTime() == null) {
            log.info("Job executor has not been destroyed, may not on this machine, jobId={}", jobEntity.getId());
            return;
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
                    .updateStatusToFailedWhenHeartTimeout(a.getId(),
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
