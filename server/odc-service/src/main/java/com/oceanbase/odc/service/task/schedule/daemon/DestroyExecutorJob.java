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

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;

import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.common.util.AlarmHelper;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-22
 * @since 4.2.4
 */
@Slf4j
@DisallowConcurrentExecution
public class DestroyExecutorJob implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        JobConfigurationValidator.validComponent();

        // scan terminate job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        Page<JobEntity> jobs = taskFrameworkService.findTerminalJob(0,
                taskFrameworkProperties.getSingleFetchDestroyExecutorJobRows());
        jobs.forEach(a -> {
            try {
                destroyExecutor(taskFrameworkService, a);
            } catch (Throwable e) {
                log.warn("Try to destroy failed, jobId={}.", a.getId(), e);
            }
        });
    }

    private void destroyExecutor(TaskFrameworkService taskFrameworkService, JobEntity jobEntity) {
        getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
            JobEntity lockedEntity = taskFrameworkService.findWithPessimisticLock(jobEntity.getId());
            if (lockedEntity.getStatus().isTerminated() && lockedEntity.getExecutorIdentifier() != null) {
                log.info("Job prepare destroy executor, jobId={},status={}.", lockedEntity.getId(),
                        lockedEntity.getStatus());
                try {
                    getConfiguration().getJobDispatcher().destroy(JobIdentity.of(lockedEntity.getId()));
                } catch (JobException e) {
                    log.warn("Destroy executor occur error, jobId={}: ", lockedEntity.getId(), e);
                    if (e.getMessage() != null &&
                            !e.getMessage().startsWith(JobConstants.ODC_EXECUTOR_CANNOT_BE_DESTROYED)) {
                        Map<String, Object> eventMessage =
                                AlarmHelper.buildAlarmMessageWithJob(jobEntity.getId());
                        String eventName = eventMessage.get(AlarmUtils.TASK_TYPE_NAME) + "_"
                                + AlarmEventNames.TASK_EXECUTOR_DESTROY_FAILED;
                        eventMessage.put(AlarmUtils.ALARM_TARGET_NAME, eventName);
                        eventMessage.put(AlarmUtils.ORGANIZATION_NAME, jobEntity.getOrganizationId());
                        eventMessage.put(AlarmUtils.MESSAGE_NAME,
                                MessageFormat.format("Job executor destroy failed, message={0}",
                                        e.getMessage()));
                        AlarmUtils.alarm(eventName, eventMessage);
                    }
                    throw new TaskRuntimeException(e);
                }
                log.info("Job destroy executor succeed, jobId={}, status={}.", lockedEntity.getId(),
                        lockedEntity.getStatus());
            } else if (lockedEntity.getStatus().isTerminated() && lockedEntity.getExecutorIdentifier() == null) {
                // It is necessary to update the finish time when the job is terminated but the
                // executorIdentifier is null, otherwise, the job cannot be released.
                log.info("Executor not found, updating executor to destroyed,jobId={}", lockedEntity.getId());
                taskFrameworkService.updateExecutorToDestroyed(lockedEntity.getId());
            }
        });
    }


    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
