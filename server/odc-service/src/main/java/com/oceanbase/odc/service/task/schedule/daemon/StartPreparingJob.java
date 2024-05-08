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
import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.trace.TraceContextHolder;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.ResourceDetectUtil;
import com.oceanbase.odc.service.task.schedule.SingleJobProperties;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.util.JobDateUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-24
 * @since 4.2.4
 */
@Slf4j
@DisallowConcurrentExecution
public class StartPreparingJob implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        JobConfigurationValidator.validComponent();

        if (!configuration.getTaskFrameworkEnabledProperties().isEnabled()) {
            configuration.getTaskFrameworkDisabledHandler().handleJobToFailed();
            return;
        }
        if (!ResourceDetectUtil.isResourceAvailable(configuration.getTaskFrameworkProperties())) {
            return;
        }
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        Page<JobEntity> jobs = taskFrameworkService.find(
                Lists.newArrayList(JobStatus.PREPARING, JobStatus.RETRYING), 0,
                taskFrameworkProperties.getSingleFetchPreparingJobRows());

        for (JobEntity a : jobs) {
            if (!configuration.getStartJobRateLimiter().tryAcquire()) {
                break;
            }
            try {
                if (checkJobIsExpired(a)) {
                    taskFrameworkService.updateStatusDescriptionByIdOldStatus(a.getId(),
                            a.getStatus(), JobStatus.CANCELED, "Job expired and failed.");
                } else {
                    startJob(taskFrameworkService, a);
                }
            } catch (Throwable e) {
                log.warn("Start job failed, jobId={}.", a.getId(), e);
            }
        }

    }

    private void startJob(TaskFrameworkService taskFrameworkService, JobEntity jobEntity) {
        getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
            JobEntity lockedEntity = taskFrameworkService.findWithPessimisticLock(jobEntity.getId());

            if (lockedEntity.getStatus() == JobStatus.PREPARING || lockedEntity.getStatus() == JobStatus.RETRYING) {

                // todo user id should be not null when submit job
                if (jobEntity.getCreatorId() != null) {
                    TraceContextHolder.setUserId(jobEntity.getCreatorId());
                }

                log.info("Prepare start job, jobId={}, currentStatus={}.",
                        lockedEntity.getId(), lockedEntity.getStatus());
                JobContext jc =
                        new DefaultJobContextBuilder().build(lockedEntity, getConfiguration().getHostUrlProvider());
                try {
                    getConfiguration().getJobDispatcher().start(jc);
                } catch (JobException e) {
                    AlarmUtils.alarm(AlarmEventNames.TASK_START_FAILED,
                            MessageFormat.format("Start job failed, jobId={0}", lockedEntity.getId()));
                    throw new TaskRuntimeException(e);
                }
            } else {
                log.warn("Job {} current status is {} but not preparing or retrying, start explain is aborted.",
                        lockedEntity.getId(), lockedEntity.getStatus());
            }
        });
    }

    private boolean checkJobIsExpired(JobEntity a) {

        SingleJobProperties jobProperties = JsonUtils.fromJson(a.getJobPropertiesJson(), SingleJobProperties.class);
        if (jobProperties == null || jobProperties.getJobExpiredIfNotRunningAfterSeconds() == null) {
            return false;
        }

        long baseTimeMills = a.getCreateTime().getTime();
        return JobDateUtils.getCurrentDate().getTime() - baseTimeMills > TimeUnit.MILLISECONDS.convert(
                jobProperties.getJobExpiredIfNotRunningAfterSeconds(), TimeUnit.SECONDS);

    }

    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
