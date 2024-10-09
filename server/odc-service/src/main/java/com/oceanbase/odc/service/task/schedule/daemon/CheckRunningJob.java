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
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
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
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * find heartbeat timeout running job, and set to retrying or failed
 * 
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
        // find heartbeat timeout job
        Page<JobEntity> jobs = getConfiguration().getTaskFrameworkService()
                .findHeartTimeTimeoutJobs(heartTimeoutSeconds, 0,
                        taskFrameworkProperties.getSingleFetchCheckHeartTimeoutJobRows());
        jobs.forEach(this::handleJobRetryingOrFailed);
    }

    private void handleJobRetryingOrFailed(JobEntity jobEntity) {
        SilentExecutor.executeSafely(() -> getConfiguration().getTransactionManager()
                .doInTransactionWithoutResult(() -> doHandleJobRetryingOrFailed(jobEntity)));
    }

    private void doHandleJobRetryingOrFailed(JobEntity jobEntity) {
        log.info("Start to handle heartbeat timeout job, jobId={}.", jobEntity.getId());
        TaskFrameworkService taskFrameworkService = getConfiguration().getTaskFrameworkService();
        JobEntity a = taskFrameworkService.findWithPessimisticLock(jobEntity.getId());
        if (a.getStatus() != JobStatus.RUNNING) {
            log.warn("Current job is not RUNNING, abort continue, jobId={}.", a.getId());
            return;
        }
        boolean isNeedRetry = checkJobIfRetryNecessary(a);
        if (isNeedRetry) {
            log.info("Need to restart job, try to set status to RETRYING, jobId={}, oldStatus={}.",
                    a.getId(), a.getStatus());
            int rows;
            if (TaskRunMode.K8S == a.getRunMode()) {
                rows = taskFrameworkService.updateExecutorEndpoint(a.getId(), null);
                log.info("Clear executor endpoint why retry task, jobId={}, rows={}", a.getId(), rows);
            }
            rows = taskFrameworkService
                    .updateStatusDescriptionByIdOldStatus(a.getId(), JobStatus.RUNNING,
                            JobStatus.RETRYING, "Heart timeout and retrying job");
            if (rows > 0) {
                log.info("Set job status to RETRYING, jobId={}, oldStatus={}.", a.getId(), a.getStatus());
            } else {
                throw new TaskRuntimeException("Set job status to RETRYING failed, jobId=" + jobEntity.getId());
            }

        } else {
            log.info("No need to restart job, try to set status to FAILED, jobId={},oldStatus={}.",
                    a.getId(), a.getStatus());
            TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
            int rows = taskFrameworkService
                    .updateStatusToFailedWhenHeartTimeout(a.getId(),
                            taskFrameworkProperties.getJobHeartTimeoutSeconds(),
                            "Heart timeout and set job to status FAILED.");
            if (rows > 0) {
                log.info("Set job status to FAILED accomplished, jobId={}, oldStatus={}.", a.getId(), a.getStatus());
                AlarmUtils.alarm(AlarmEventNames.TASK_HEARTBEAT_TIMEOUT,
                        JsonUtils.createJsonNodeBuilder()
                                .item("OrganizationId", jobEntity.getOrganizationId())
                                .item("CreatorId", jobEntity.getCreatorId())
                                .item("JobId", jobEntity.getId())
                                .item("TaskType", jobEntity.getJobType())
                                .item("Message", "Job running failed due to heart timeout")
                                .build());
            } else {
                throw new TaskRuntimeException("Set job status to FAILED failed, jobId=" + jobEntity.getId());
            }
        }
        if (!getConfiguration().getJobDispatcher().canBeDestroy(JobIdentity.of(a.getId()))) {
            log.info("Cannot destroy executor, jobId={}.", a.getId());
            throw new TaskRuntimeException("Cannot destroy executor, jobId={}" + jobEntity.getId());
        }

        // First try to stop remote job
        try {
            log.info("Try to stop remote job, jobId={}.", a.getId());
            getConfiguration().getJobDispatcher().stop(JobIdentity.of(a.getId()));
        } catch (JobException e) {
            // Process will continue if stop failed and not rollback transaction
            log.warn("Try to stop remote failed, jobId={}.", a.getId(), e);
        }

        // Second destroy executor
        try {
            log.info("Try to destroy executor, jobId={}.", a.getId());
            getConfiguration().getJobDispatcher().destroy(JobIdentity.of(a.getId()));
        } catch (JobException e) {
            throw new TaskRuntimeException(e);
        }
        if (!isNeedRetry) {
            getConfiguration().getEventPublisher().publishEvent(
                    new JobTerminateEvent(JobIdentity.of(a.getId()), JobStatus.FAILED));
        }
    }

    private boolean checkJobIfRetryNecessary(JobEntity je) {
        SingleJobProperties jobProperties = SingleJobProperties.fromJobProperties(je.getJobProperties());
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
