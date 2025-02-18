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

import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentFactory;
import com.oceanbase.odc.service.task.caller.ProcessConfig;
import com.oceanbase.odc.service.task.caller.ProcessJobCaller;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.listener.JobTerminateEvent;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.util.JobDateUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * prepare job run by supervisor agent
 * 
 * @author longpeng.zlp
 * @date 2024/11/29 14:08
 */
@Slf4j
@DisallowConcurrentExecution
public class StartPreparingJobV2 implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();

        if (!configuration.getTaskFrameworkEnabledProperties().isEnabled()) {
            configuration.getTaskFrameworkDisabledHandler().handleJobToFailed();
            return;
        }
        // safe check
        if (!configuration.getTaskFrameworkProperties().isEnableTaskSupervisorAgent()) {
            return;
        }
        // process preparing job with rate limiter
        processPreparingJob(configuration);
        // process ready start job
        processReadyStartJob(configuration);
    }

    /**
     * process job in preparing status, it will do rate limit status
     */
    protected void processPreparingJob(JobConfiguration configuration) {
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        Page<JobEntity> jobs = taskFrameworkService.find(
                Lists.newArrayList(JobStatus.PREPARING), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows());

        for (JobEntity jobEntity : jobs) {
            try {
                // first check if job is expired
                if (checkJobIsExpired(jobEntity)) {
                    // expired task transfer to timeout, to try to send stop command
                    JobUtils.updateStatusAndCheck(jobEntity.getId(), jobEntity.getStatus(), JobStatus.TIMEOUT,
                            taskFrameworkService);
                    continue;
                }
                // check rate limiter
                if (!configuration.getStartJobRateLimiter().tryAcquire()) {
                    break;
                }
                // summit job resource allocate request
                configuration.getTransactionManager().doInTransactionWithoutResult(
                        () -> allocateResource(configuration, jobEntity));
            } catch (Throwable e) {
                log.warn("Start job failed cause prepare resource failed, jobId={}, terminate job.", jobEntity.getId(),
                        e);
                afterJobFailed(e, jobEntity, configuration);
            }
        }
    }

    protected void allocateResource(JobConfiguration configuration, JobEntity jobEntity) {
        JobEntity lockedEntity = configuration.getTaskFrameworkService().findWithPessimisticLock(jobEntity.getId());
        // may operate by old version odc, for compatible
        if (lockedEntity.getStatus() != jobEntity.getStatus()) {
            log.warn("job status bas been modified, prev job = {}, current job = {}", jobEntity, lockedEntity);
            return;
        }
        JobContext jobContext =
                new DefaultJobContextBuilder().build(jobEntity, configuration);
        configuration.getSupervisorAgentAllocator()
                .submitAllocateSupervisorEndpointRequest(jobEntity.getRunMode().name(), jobContext,
                        retrieveJobRunningLocation(jobEntity, jobContext));
        JobUtils.updateStatusAndCheck(jobEntity.getId(), jobEntity.getStatus(), JobStatus.PREPARING_RESR,
                configuration.getTaskFrameworkService());
    }

    /**
     * process job in allocate resource status
     */
    protected void processReadyStartJob(JobConfiguration configuration) {
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        Page<JobEntity> jobs = taskFrameworkService.find(
                Lists.newArrayList(JobStatus.PREPARING_RESR), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows());

        for (JobEntity jobEntity : jobs) {
            try {
                // first check if job is expired
                if (checkJobIsExpired(jobEntity)) {
                    // expired task transfer to timeout, to try to send stop command
                    JobUtils.updateStatusAndCheck(jobEntity.getId(), jobEntity.getStatus(), JobStatus.TIMEOUT,
                            taskFrameworkService);
                } else {
                    JobContext jobContext =
                            new DefaultJobContextBuilder().build(jobEntity, configuration);
                    Optional<SupervisorEndpoint> supervisorEndpoint = configuration.getSupervisorAgentAllocator()
                            .checkAllocateSupervisorEndpointState(jobContext);
                    // resource not ready yet, wait another round
                    if (!supervisorEndpoint.isPresent()) {
                        continue;
                    }
                    try {
                        configuration.getTransactionManager().doInTransactionWithoutResult(
                                () -> startJob(supervisorEndpoint.get(), configuration, jobContext, jobEntity));
                    } catch (Throwable e) {
                        // rollback load
                        configuration.getSupervisorAgentAllocator()
                                .deallocateSupervisorEndpoint(jobContext.getJobIdentity().getId());
                        throw e;
                    }
                }
            } catch (Throwable e) {
                log.warn("Start job failed cause start job failed, jobId={}, terminate job.", jobEntity.getId(), e);
                afterJobFailed(e, jobEntity, configuration);
            }
        }
    }

    private void afterJobFailed(Throwable e, JobEntity jobEntity, JobConfiguration configuration) {
        JobUtils.updateStatusAndCheck(jobEntity.getId(), jobEntity.getStatus(), JobStatus.FAILED,
                configuration.getTaskFrameworkService());
        JobUtils.alarmJobEvent(jobEntity, AlarmEventNames.TASK_START_FAILED,
                MessageFormat.format("Start job failed, jobId={0}, message={1}", jobEntity.getId(),
                        e.getMessage()));
        // send terminate event
        configuration.getEventPublisher().publishEvent(
                new JobTerminateEvent(JobIdentity.of(jobEntity.getId()), JobStatus.FAILED));
    }

    protected ResourceLocation retrieveJobRunningLocation(JobEntity jobEntity, JobContext jobContext) {
        if (jobEntity.getRunMode() == TaskRunMode.PROCESS) {
            return ResourceIDUtil.PROCESS_RESOURCE_LOCATION;
        } else {
            return ResourceIDUtil.getResourceLocation(jobContext.getJobProperties());
        }
    }

    private void startJob(SupervisorEndpoint supervisorEndpoint, JobConfiguration configuration, JobContext jobContext,
            JobEntity jobEntity) {
        // todo user id should be not null when submit job
        if (jobEntity.getCreatorId() != null) {
            TraceContextHolder.setUserId(jobEntity.getCreatorId());
        }

        log.info("Prepare start job, jobId={}, currentStatus={}.",
                jobEntity.getId(), jobEntity.getStatus());
        try {
            ProcessConfig processConfig = buildProcessConfig(configuration, jobContext);
            ExecutorEndpoint executorEndpoint = configuration.getTaskSupervisorJobCaller()
                    .startTask(supervisorEndpoint, jobContext, processConfig);
            log.info("start job success with endpoint={}", executorEndpoint);
        } catch (JobException e) {
            JobUtils.alarmJobEvent(jobEntity, AlarmEventNames.TASK_START_FAILED,
                    MessageFormat.format("Start job failed, jobId={0}, message={1}",
                            jobEntity.getId(),
                            e.getMessage()));
            throw new TaskRuntimeException(e);
        }
    }

    protected ProcessConfig buildProcessConfig(JobConfiguration configuration, JobContext jobContext) {
        TaskFrameworkProperties properties = configuration.getTaskFrameworkProperties();
        String logPath = properties.getRunMode() == TaskRunMode.K8S
                ? JobUtils.getLogBasePath(properties.getK8sProperties().getMountPath())
                : LogUtils.getBaseLogPath();
        ProcessJobCaller jobCaller = JobCallerBuilder.buildProcessCaller(jobContext,
                new JobEnvironmentFactory().build(jobContext, TaskRunMode.PROCESS, configuration, logPath),
                configuration);
        return jobCaller.getProcessConfig();
    }

    protected boolean checkJobIsExpired(JobEntity jobEntity) {
        SingleJobProperties jobProperties = SingleJobProperties.fromJobProperties(jobEntity.getJobProperties());
        if (jobProperties == null || jobProperties.getJobExpiredIfNotRunningAfterSeconds() == null) {
            return false;
        }

        long baseTimeMills = jobEntity.getCreateTime().getTime();
        return JobDateUtils.getCurrentDate().getTime() - baseTimeMills > TimeUnit.MILLISECONDS.convert(
                jobProperties.getJobExpiredIfNotRunningAfterSeconds(), TimeUnit.SECONDS);
    }
}
