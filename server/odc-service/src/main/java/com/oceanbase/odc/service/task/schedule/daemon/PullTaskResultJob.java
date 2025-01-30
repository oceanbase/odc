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
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

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

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();
        this.taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        this.taskFrameworkService = configuration.getTaskFrameworkService();

        int singlePullResultJobRows = taskFrameworkProperties.getSinglePullResultJobRows();
        Page<JobEntity> runningJobs = taskFrameworkService.findRunningJobs(0, singlePullResultJobRows);
        runningJobs.forEach(job -> taskFrameworkService.refreshResult(job.getId()));
    }
}
