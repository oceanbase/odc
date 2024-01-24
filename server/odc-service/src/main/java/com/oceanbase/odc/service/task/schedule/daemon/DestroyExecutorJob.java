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

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
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
        if (configuration == null) {
            log.debug("configuration is null, abort continue execute");
            return;
        }
        // scan terminate job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        Page<JobEntity> jobs = taskFrameworkService.findTerminalJob(0,
                taskFrameworkProperties.getSingleFetchDestroyExecutorJobRows());
        jobs.forEach(a -> {
            try {
                destroyExecutor(taskFrameworkService, a);
            } catch (Throwable e) {
                log.warn("Try to start job {} failed: ", a.getId(), e);
            }
        });
    }

    private void destroyExecutor(TaskFrameworkService taskFrameworkService, JobEntity oldEntity) {
        getConfiguration().getTransactionManager().doInTransactionWithoutResult(() -> {
            JobEntity newEntity = taskFrameworkService.findWithPessimisticLock(oldEntity.getId());

            if (newEntity.getStatus().isTerminated() && newEntity.getExecutorIdentifier() != null) {
                log.info("Job {} current status is {}, prepare destroy executor.", newEntity.getId(),
                        newEntity.getStatus());
                try {
                    getConfiguration().getJobDispatcher().destroy(JobIdentity.of(newEntity.getId()));
                } catch (JobException e) {
                    log.warn("Destroy executor occur error: ", e);
                    throw new TaskRuntimeException(e);
                }
                log.info("Job {} destroy executor succeed.", newEntity.getId());
            }
            return null;
        });
    }


    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
