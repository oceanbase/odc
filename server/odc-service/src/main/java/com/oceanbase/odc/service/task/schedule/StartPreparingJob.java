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

package com.oceanbase.odc.service.task.schedule;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
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
        if (configuration == null) {
            log.debug("configuration is null, abort continue execute");
            return;
        }
        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();
        TaskFrameworkProperties taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        List<JobEntity> jobs = taskFrameworkService.find(JobStatus.PREPARING, 0,
                taskFrameworkProperties.getSingleFetchJobRowsForSchedule());
        jobs.forEach(a -> {
            try {
                if (checkJobIsExpired(a)) {
                    getConfiguration().getJobDispatcher().stop(JobIdentity.of(a.getId()));
                    getConfiguration().getTaskFrameworkService().updateStatus(a.getId(), JobStatus.FAILED);
                    getConfiguration().getTaskFrameworkService()
                            .updateDescription(a.getId(), "Job expired and failed.");
                } else {
                    JobContext jc = new DefaultJobContextBuilder().build(a, configuration.getHostUrlProvider());
                    configuration.getJobDispatcher().start(jc);
                }
            } catch (JobException e) {
                log.warn("try to start job {} failed: ", a.getId(), e);
            }
        });
    }

    private boolean checkJobIsExpired(JobEntity a) {

        JobProperties jobProperties = JsonUtils.fromJson(a.getJobPropertiesJson(), JobProperties.class);
        if (jobProperties == null || jobProperties.getJobExpiredAfterSeconds() == null) {
            return false;
        }

        long baseTime = a.getCreateTime().getTime();
        return JobDateUtils.getCurrentDate().getTime() - baseTime > TimeUnit.MILLISECONDS.convert(
                jobProperties.getJobExpiredAfterSeconds(), TimeUnit.SECONDS);

    }

    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
