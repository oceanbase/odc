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
package com.oceanbase.odc.service.task.schedule.daemon.v2;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.executor.TaskResult;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.state.JobStatusFsm;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.service.task.util.TaskExecutorClient;
import com.oceanbase.odc.service.task.util.TaskResultWrap;

import lombok.extern.slf4j.Slf4j;

/**
 * pull task result, update heartbeatTime and taskResult. do as pull task result means the task is
 * active, we also update heartbeatTime here. and heart timeout will also checked here
 * 
 * @author longpeng.zlp
 * @date 2024-12-13
 */
@Slf4j
@DisallowConcurrentExecution
public class PullTaskResultJobV2 implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        // safe check
        if (!configuration.getTaskFrameworkProperties().isEnableTaskSupervisorAgent()) {
            return;
        }
        List<JobEntity> runningJobs = getRunningJobs(configuration);
        doRefreshJob(configuration, runningJobs);
    }

    @VisibleForTesting
    protected List<JobEntity> getRunningJobs(JobConfiguration configuration) {

        int singlePullResultJobRows = configuration.getTaskFrameworkProperties().getSinglePullResultJobRows();
        Instant now = Instant.now();
        // first pull heartbeat not received job, order by create time
        // then pull heartbeat task, order by heartbeat time
        return configuration.getTaskFrameworkService().findNeedPullResultJobs(0, singlePullResultJobRows).stream()
                .sorted((j1, j2) -> {
                    // desc order
                    return Long.compare(getLastUpdateTimeInMillDurationInMillSeconds(j2, now),
                            getLastUpdateTimeInMillDurationInMillSeconds(j1, now));
                }).collect(Collectors.toList());
    }

    // refresh job has a timeout, if this round is timeout, next round will go again
    public void doRefreshJob(JobConfiguration configuration, List<JobEntity> runningJobs) {
        if (CollectionUtils.isEmpty(runningJobs)) {
            return;
        }
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        CountDownLatch countDownLatch = new CountDownLatch(runningJobs.size());
        runningJobs.forEach(job -> taskFrameworkService.getTaskResultPullerExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    tryProcessTaskResult(configuration, job);
                } catch (Throwable e) {
                    log.info("pull job result failed, job = {}, reason = {}", job, e);
                } finally {
                    countDownLatch.countDown();
                }
            }
        }));
        boolean waitSuccess = false;
        long currentTime = System.currentTimeMillis();
        while (!waitSuccess) {
            try {
                countDownLatch.await(30, TimeUnit.SECONDS);
                waitSuccess = true;
            } catch (Throwable e) {
                log.warn("count down interrupted, cause {}", e.getMessage());
            }
            log.info("wait refresh round done {}, time cost {}", waitSuccess ? "success" : "failed",
                    (System.currentTimeMillis() - currentTime + 1000) / 1000);
        }
    }

    // 1. filter current job need to be pulled, including doCanceling and running
    // 2. try pull result
    // 3. if pull success, update heartbeat, result json, if finished, transfer to correct status
    // if pull failed, check if heartbeat if outdated
    // async httpclient maybe needed
    private void tryProcessTaskResult(JobConfiguration configuration, JobEntity jobEntity) throws JobException {
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        TaskExecutorClient taskExecutorClient = configuration.getTaskExecutorClient();
        // only process RUNNING and DO_CANCELING state
        if (JobStatus.RUNNING != jobEntity.getStatus() && JobStatus.DO_CANCELING != jobEntity.getStatus()) {
            log.info("Invalid status, jobId={}, currentStatus={}", jobEntity.getId(), jobEntity.getStatus());
            return;
        }
        // try get result
        TaskResultWrap resultWarp = tryPullTaskResult(taskExecutorClient, jobEntity);
        TaskResult result = resultWarp.getTaskResult();
        // if result get failed and timeout, try terminate task
        if (null == result) {
            if (isTaskTimeout(taskFrameworkProperties.getJobHeartTimeoutSeconds(), jobEntity)) {
                // there may exist a situation may update failed, status from running to canceling updated by user
                // but they all lead to stop task
                int updateRows = taskFrameworkService.updateStatusByIdOldStatus(jobEntity.getId(),
                        jobEntity.getStatus(), JobStatus.TIMEOUT);
                log.info("job id = {} is  expired after {} seconds, so try mark it as timeout, success = {}",
                        jobEntity.getId(), taskFrameworkProperties.getJobHeartTimeoutSeconds(),
                        updateRows == 0 ? "false" : "true");
                JobUtils.alarmJobEvent(jobEntity, AlarmEventNames.TASK_HEARTBEAT_TIMEOUT,
                        "Job heartbeat timeout, jobId=" + jobEntity.getId());
            } else {
                log.info("job id = {} not receive result from task, reason = {}",
                        jobEntity.getId(), resultWarp.getE() != null ? resultWarp.getE().getMessage() : "");
            }
            return;
        }

        TaskResult previous = JsonUtils.fromJson(jobEntity.getResultJson(), TaskResult.class);
        // always try update heartbeat
        taskFrameworkService.updateHeartbeatWithExpectStatus(jobEntity.getId(), jobEntity.getStatus());

        if (!result.isProgressChanged(previous) || result.getStatus() == TaskStatus.PREPARING) {
            log.info("Progress not changed, skip update result to metadb, jobId={}, currentProgress={}",
                    jobEntity.getId(), result.getProgress());
            return;
        }
        log.info("Progress changed, will update result, jobId={}, currentProgress={}", jobEntity.getId(),
                result.getProgress());
        taskFrameworkService.propagateTaskResult(jobEntity.getJobType(), result);
        taskFrameworkService.saveOrUpdateLogMetadata(result, jobEntity.getId(), jobEntity.getStatus());

        // not upload result
        // TODO(lx): DO_CANCELING should add time out here?
        // if a task alive, but always not terminate like not upload log meta. this situation should add a
        // timeout
        if (MapUtils.isEmpty(result.getLogMetadata())) {
            log.info("Job is finished but log have not uploaded, continue monitor result, jobId={}, currentStatus={}",
                    jobEntity.getId(), jobEntity.getStatus());
            return;
        }
        // try finish task
        tryCompleteTask(taskFrameworkService, jobEntity, result);
    }

    protected TaskResultWrap tryPullTaskResult(TaskExecutorClient taskExecutorClient, JobEntity jobEntity)
            throws JobException {
        // executor endpoint should be provided
        if (StringUtils.isBlank(jobEntity.getExecutorEndpoint())) {
            log.warn("executor endpoint should not be null", jobEntity.getId());
            return TaskResultWrap.unreachedTaskResult(
                    new JobException("executor endpoint should not be null, job id = " + jobEntity.getId()));
        } else {
            // try get result
            return taskExecutorClient.getResult(jobEntity.getExecutorEndpoint(), JobIdentity.of(jobEntity.getId()));
        }
    }

    // to judge if task has timeout
    protected boolean isTaskTimeout(int heartbeatTimeout, JobEntity jobEntity) {
        // first check if heartbeat has received
        long timeoutInMillionSeconds = getLastUpdateTimeInMillDurationInMillSeconds(jobEntity, Instant.now());
        return timeoutInMillionSeconds / 1000 > heartbeatTimeout;
    }

    protected long getLastUpdateTimeInMillDurationInMillSeconds(JobEntity job, Instant nowInstant) {
        // check last heartbeat timeout
        // check create time
        if (job.getLastHeartTime() == null) {
            return Duration.between(job.getCreateTime().toInstant(), nowInstant).toMillis();
        } else {
            return Duration.between(job.getLastHeartTime().toInstant(), nowInstant).toMillis();
        }
    }

    // try complete task to terminate state and publish event
    @VisibleForTesting
    protected void tryCompleteTask(TaskFrameworkService taskFrameworkService, JobEntity jobEntity, TaskResult result) {
        JobStatusFsm jobStatusFsm = new JobStatusFsm();
        JobStatus expectedJobStatus = jobStatusFsm.determinateJobStatus(jobEntity.getStatus(), result.getStatus());
        // has received canceling command and receive task last response
        // changed it to canceled no matter what task returned
        if (jobEntity.getStatus() == JobStatus.DO_CANCELING) {
            expectedJobStatus = JobStatus.CANCELED;
        }
        int rows = taskFrameworkService.updateTaskResult(result, jobEntity, expectedJobStatus);
        if (rows == 0) {
            log.warn("Update task result failed, the job may finished or deleted already, jobId={}", jobEntity.getId());
            return;
        }
        taskFrameworkService.publishEvent(result, jobEntity, expectedJobStatus);
    }
}
