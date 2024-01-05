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
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-24
 * @since 4.2.4
 */
@Slf4j
public class StartPreparingJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();

        // scan preparing job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();

        List<JobEntity> jobs = taskFrameworkService.find(JobStatus.PREPARING, 0, 100);
        jobs.forEach(a -> {
            try {
                DefaultJobContext jobContext = new DefaultJobContext();
                jobContext.setJobIdentity(JobIdentity.of(a.getId()));
                jobContext.setJobClass(a.getJobClass());
                jobContext.setJobParameters(JsonUtils.fromJson(a.getJobParametersJson(),
                        new TypeReference<Map<String, String>>() {}));
                jobContext.setHostUrls(configuration.getHostUrlProvider().hostUrl());
                JobConfigurationHolder.getJobConfiguration().getJobDispatcher().start(jobContext);
            } catch (JobException e) {
                log.warn("try to start job {} failed: ", a.getId(), e);
            }
        });
    }
}
