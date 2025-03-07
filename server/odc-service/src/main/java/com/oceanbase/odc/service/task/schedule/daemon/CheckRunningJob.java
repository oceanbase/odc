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

import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.common.util.SilentExecutor;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import cn.hutool.core.util.StrUtil;
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
        // query from db
        JobEntity refreshedJobEntity = taskFrameworkService.findWithPessimisticLock(jobEntity.getId());
        // TODO(lx): confirm this logic why job not in running status not handle
        if (refreshedJobEntity.getStatus() != JobStatus.RUNNING) {
            log.warn("Current job is not RUNNING, abort continue, jobId={}.", refreshedJobEntity.getId());
            return;
        }
        try {
            tryFinishJob(taskFrameworkService, refreshedJobEntity);
        } finally {
            // mark resource as released to let resource collector collect resource
            if (TaskRunMode.K8S == refreshedJobEntity.getRunMode()) {
                ResourceManagerUtil.markResourceReleased(refreshedJobEntity, refreshedJobEntity.getExecutorIdentifier(),
                        getConfiguration().getResourceManager(), configuration.getTaskFrameworkProperties());
                log.info("CheckRunningJob release resource for job = {}", jobEntity);
            }
        }
    }


    private void tryFinishJob(TaskFrameworkService taskFrameworkService, JobEntity jobEntity) {
        boolean isNeedRetry = checkJobIfRetryNecessary(jobEntity);

        log.info("No need to restart job, try to set status to FAILED, jobId={},oldStatus={}.",
                jobEntity.getId(), jobEntity.getStatus());
        TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
        int rows = taskFrameworkService
                .updateStatusToFailedWhenHeartTimeout(jobEntity.getId(),
                        taskFrameworkProperties.getJobHeartTimeoutSeconds(),
                        "Heart timeout and set job to status FAILED.");
        if (rows > 0) {
            log.info("Set job status to FAILED accomplished, jobId={}, oldStatus={}.", jobEntity.getId(),
                    jobEntity.getStatus());
            Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                    .item(AlarmUtils.ORGANIZATION_NAME, Optional.ofNullable(jobEntity.getOrganizationId()).map(
                            Object::toString).orElse(StrUtil.EMPTY))
                    .item(AlarmUtils.TASK_JOB_ID_NAME, String.valueOf(jobEntity.getId()))
                    .item(AlarmUtils.MESSAGE_NAME,
                            MessageFormat.format("Job running failed due to heart timeout, jobId={0}",
                                    jobEntity.getId()))
                    .build();
            AlarmUtils.alarm(AlarmEventNames.TASK_HEARTBEAT_TIMEOUT, eventMessage);
        } else {
            throw new TaskRuntimeException("Set job status to FAILED failed, jobId=" + jobEntity.getId());
        }

        if (!getConfiguration().getJobDispatcher().canBeFinish(JobIdentity.of(jobEntity.getId()))) {
            log.info("Cannot destroy executor, jobId={}.", jobEntity.getId());
            throw new TaskRuntimeException("Cannot destroy executor, jobId={}" + jobEntity.getId());
        }

        // First try to stop remote job
        try {
            log.info("Try to stop remote job, jobId={}.", jobEntity.getId());
            if (StringUtils.isEmpty(jobEntity.getExecutorIdentifier())) {
                log.info("found invalid job = {}, resource destroy not confirmed, set status to failed", jobEntity);
                taskFrameworkService
                        .updateStatusDescriptionByIdOldStatus(jobEntity.getId(), JobStatus.RUNNING,
                                JobStatus.FAILED, "old job not determinate resource has created");
                return;
            }
            getConfiguration().getJobDispatcher().stop(JobIdentity.of(jobEntity.getId()));
        } catch (JobException e) {
            // Process will continue if stop failed and not rollback transaction
            log.warn("Try to stop remote failed, jobId={}.", jobEntity.getId(), e);
        }

        // Second finish job and clean it all
        try {
            log.info("Try to destroy executor, jobId={}.", jobEntity.getId());
            getConfiguration().getJobDispatcher().finish(JobIdentity.of(jobEntity.getId()));
        } catch (JobException e) {
            throw new TaskRuntimeException(e);
        }
        if (!isNeedRetry) {
            // set status to destroyed
            taskFrameworkService.updateExecutorToDestroyed(jobEntity.getId());
            getConfiguration().getEventPublisher().publishEvent(
                    new JobTerminateEvent(JobIdentity.of(jobEntity.getId()), JobStatus.FAILED));
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
