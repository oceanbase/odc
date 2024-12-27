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
import java.util.Map;
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
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.task.caller.JobCallerBuilder;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobEnvironmentFactory;
import com.oceanbase.odc.service.task.caller.ProcessJobCaller;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.supervisor.endpoint.ExecutorEndpoint;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;
import com.oceanbase.odc.service.task.util.JobDateUtils;
import com.oceanbase.odc.service.task.util.JobUtils;

import cn.hutool.core.util.StrUtil;
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

        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        Page<JobEntity> jobs = taskFrameworkService.find(
                Lists.newArrayList(JobStatus.PREPARING), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows());

        for (JobEntity jobEntity : jobs) {
            if (!configuration.getStartJobRateLimiter().tryAcquire()) {
                break;
            }
            try {
                if (checkJobIsExpired(jobEntity)) {
                    // expired task transfer to timeout, to try to send stop command
                    JobUtils.updateStatusAndCheck(jobEntity.getId(), jobEntity.getStatus(), JobStatus.TIMEOUT,
                            taskFrameworkService);
                } else {
                    JobContext jobContext =
                            new DefaultJobContextBuilder().build(jobEntity);
                    Optional<SupervisorEndpoint> supervisorEndpoint = configuration.getSupervisorAgentAllocator()
                            .tryAllocateSupervisorEndpoint(jobEntity.getRunMode().name(), jobContext,
                                    retrieveJobRunningLocation(jobEntity, jobContext));
                    // no resource found current round, try allocate next
                    if (!supervisorEndpoint.isPresent()) {
                        continue;
                    }
                    configuration.getTransactionManager().doInTransactionWithoutResult(
                            () -> startJob(supervisorEndpoint.get(), configuration, jobContext, jobEntity));
                }
            } catch (Throwable e) {
                log.warn("Start job failed, jobId={}, terminate job.", jobEntity.getId(), e);
                JobUtils.updateStatusAndCheck(jobEntity.getId(), jobEntity.getStatus(), JobStatus.FAILED,
                        taskFrameworkService);
            }
        }
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
        ProcessJobCaller jobCaller = JobCallerBuilder.buildProcessCaller(jobContext,
                new JobEnvironmentFactory().build(jobContext, TaskRunMode.PROCESS));
        try {
            ExecutorEndpoint executorEndpoint = configuration.getTaskSupervisorJobCaller()
                    .startTask(supervisorEndpoint, jobContext, jobCaller.getProcessConfig());
            log.info("start job success with endpoint={}", executorEndpoint);
        } catch (JobException e) {
            Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                    .item(AlarmUtils.ORGANIZATION_NAME, Optional.ofNullable(jobEntity.getOrganizationId()).map(
                            Object::toString).orElse(StrUtil.EMPTY))
                    .item(AlarmUtils.TASK_JOB_ID_NAME, String.valueOf(jobEntity.getId()))
                    .item(AlarmUtils.MESSAGE_NAME,
                            MessageFormat.format("Start job failed, jobId={0}, message={1}",
                                    jobEntity.getId(),
                                    e.getMessage()))
                    .build();
            AlarmUtils.alarm(AlarmEventNames.TASK_START_FAILED, eventMessage);
            // rollback load
            configuration.getSupervisorAgentAllocator()
                    .deallocateSupervisorEndpoint(jobContext.getJobIdentity().getId());
            throw new TaskRuntimeException(e);
        }
    }

    private boolean checkJobIsExpired(JobEntity jobEntity) {
        SingleJobProperties jobProperties = SingleJobProperties.fromJobProperties(jobEntity.getJobProperties());
        if (jobProperties == null || jobProperties.getJobExpiredIfNotRunningAfterSeconds() == null) {
            return false;
        }

        long baseTimeMills = jobEntity.getCreateTime().getTime();
        return JobDateUtils.getCurrentDate().getTime() - baseTimeMills > TimeUnit.MILLISECONDS.convert(
                jobProperties.getJobExpiredIfNotRunningAfterSeconds(), TimeUnit.SECONDS);
    }
}
