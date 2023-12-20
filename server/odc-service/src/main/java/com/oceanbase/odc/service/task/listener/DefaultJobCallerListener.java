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

package com.oceanbase.odc.service.task.listener;

import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.schedule.DefaultJobContextBuilder;
import com.oceanbase.odc.service.task.schedule.JobDefinition;
import com.oceanbase.odc.service.task.schedule.JobIdentity;
import com.oceanbase.odc.service.task.schedule.JobScheduler;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-12-15
 * @since 4.2.4
 */
@Slf4j
public class DefaultJobCallerListener extends JobCallerListener {

    private final TaskFrameworkService taskFrameworkService;
    private final JobConfiguration configuration;

    public DefaultJobCallerListener(JobScheduler jobScheduler) {
        this.configuration = JobConfigurationHolder.getJobConfiguration();
        this.taskFrameworkService = configuration.getTaskFrameworkService();
    }

    @Override
    protected void startSucceed(JobIdentity ji) {
        if (taskFrameworkService != null) {
            taskFrameworkService.startSuccess(ji.getId(), ji.getName());
        }
    }

    @Override
    protected void startFailed(JobIdentity ji, Exception ex) {
        if (taskFrameworkService != null) {
            JobEntity jobEntity = taskFrameworkService.find(ji.getId());
            if (jobEntity.getScheduleTimes() >= 5) {
                jobEntity.setDescription("After retry 5 times to schedule job but failed.");
                taskFrameworkService.update(jobEntity);
                return;
            }

            log.info("Start job " + ji.getId() + " failed and retry again, error is: ", ex);
            taskFrameworkService.updateScheduleTimes(ji.getId(), jobEntity.getScheduleTimes() + 1);
            JobDefinition jd = taskFrameworkService.getJobDefinition(ji.getId());
            JobContext jc = new DefaultJobContextBuilder().build(ji, jd);
            try {
                configuration.getJobDispatcher().start(jc);
            } catch (JobException e) {
                log.warn("Try start failed.", e);
            }
        }
    }

    @Override
    protected void stopSucceed(JobIdentity ji) {}

    @Override
    protected void stopFailed(JobIdentity ji, Exception ex) {}
}
