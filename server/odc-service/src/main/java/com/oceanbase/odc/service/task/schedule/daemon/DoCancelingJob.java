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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
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
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-12
 * @since 4.2.4
 */
@Slf4j
@DisallowConcurrentExecution
public class DoCancelingJob implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        JobConfigurationValidator.validComponent();
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        Page<JobEntity> jobs = taskFrameworkService.findCancelingJob(0,
                taskFrameworkProperties.getSingleFetchCancelingJobRows());
        jobs.forEach(a -> {
            try {
                cancelJob(taskFrameworkService, a);
            } catch (Throwable e) {
                log.warn("Try to start job {} failed: ", a.getId(), e);
            }
        });
    }

    private void cancelJob(TaskFrameworkService taskFrameworkService, JobEntity jobEntity) {
        getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
            JobEntity lockedEntity = taskFrameworkService.findWithPessimisticLock(jobEntity.getId());
            if (lockedEntity.getStatus() == JobStatus.CANCELING) {
                // For transaction atomic, first update to CANCELED, then stop remote job in executor,
                // if stop remote failed, transaction will be rollback
                int rows = getConfiguration().getTaskFrameworkService()
                        .updateStatusDescriptionByIdOldStatus(lockedEntity.getId(),
                                JobStatus.CANCELING, JobStatus.CANCELED, "stop job completed.");
                if (rows <= 0) {
                    throw new TaskRuntimeException(
                            "Update job status to CANCELED failed, jobId=" + lockedEntity.getId());
                }
                log.info("Prepare cancel task, jobId={}.", lockedEntity.getId());
                try {
                    getConfiguration().getJobDispatcher().stop(JobIdentity.of(lockedEntity.getId()));
                } catch (JobException e) {
                    log.warn("Stop job occur error: ", e);
                    AlarmUtils.alarm(AlarmEventNames.TASK_CANCELED_FAILED,
                            MessageFormat.format("Cancel job failed, jobId={0}", lockedEntity.getId()));
                    throw new TaskRuntimeException(e);
                }
                log.info("Job be cancelled successfully, jobId={}, oldStatus={}.", lockedEntity.getId(),
                        lockedEntity.getStatus());
                getConfiguration().getEventPublisher().publishEvent(
                        new JobTerminateEvent(JobIdentity.of(lockedEntity.getId()), JobStatus.CANCELED));
            }
        });
    }

    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
