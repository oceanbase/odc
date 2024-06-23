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

import com.oceanbase.odc.common.util.SilentExecutor;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.JobConfigurationValidator;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;
import com.oceanbase.odc.service.task.service.TransactionManager;

import lombok.extern.slf4j.Slf4j;

/**
 * pull task result, update heartbeatTime and taskResult. <br>
 * as pull task result means the task is active, we also update heartbeatTime here.
 */
@Slf4j
@DisallowConcurrentExecution
public class PullTaskResultJob implements Job {

    private TaskFrameworkProperties taskFrameworkProperties;
    private TaskFrameworkService taskFrameworkService;
    private TransactionManager transactionManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        this.taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        this.taskFrameworkService = configuration.getTaskFrameworkService();
        this.transactionManager = configuration.getTransactionManager();

        JobConfigurationValidator.validComponent();

        int jobPullResultIntervalSeconds = taskFrameworkProperties.getJobPullResultIntervalSeconds();
        // TODO: list all running jobs, and pull task result, then update heartbeatTime and taskResult for
        // each running job

        int singlePullResultJobRows = taskFrameworkProperties.getSinglePullResultJobRows();
        Page<JobEntity> runningJobs = taskFrameworkService.findRunningJobs(0, singlePullResultJobRows);
        runningJobs.forEach(this::pullJobResult);
    }

    private void pullJobResult(JobEntity jobEntity) {
        SilentExecutor.executeSafely(() -> this.transactionManager.doInTransactionWithoutResult(
                () -> doPullJobResult(jobEntity)));
    }

    private void doPullJobResult(JobEntity jobEntity) {
        JobEntity a = this.taskFrameworkService.findWithPessimisticLock(jobEntity.getId());
        if (a.getStatus() != JobStatus.RUNNING) {
            log.info("Current job is not RUNNING, abort continue, jobId={}.", a.getId());
            return;
        }
        // do pull job result

    }

}
