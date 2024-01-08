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

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.util.JobDateUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2024-01-04
 * @since 4.2.4
 */
@Slf4j
@DisallowConcurrentExecution
public class CheckRunningExpiredJob implements Job {

    private JobConfiguration configuration;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        configuration = JobConfigurationHolder.getJobConfiguration();
        if (configuration == null) {
            log.debug("Job configuration is null, abort continue execute.");
            return;
        }
        TaskFrameworkProperties taskFrameworkProperties = getConfiguration().getTaskFrameworkProperties();
        // scan preparing and running job
        List<JobEntity> jobs;
        int offset = 0;
        int limit = taskFrameworkProperties.getSingleFetchJobRowsForCheckReportTimeout();
        do {
            // check timeout job
            jobs = getConfiguration().getTaskFrameworkService()
                    .find(Lists.newArrayList(JobStatus.RUNNING, JobStatus.RETRYING), offset, limit);
            jobs.forEach(this::checkJob);
            offset = offset * jobs.size() + 1;

        } while (jobs.isEmpty() || offset >= taskFrameworkProperties.getMaxFetchJobRowsForCheckExpired());
    }

    private void checkJob(JobEntity a) {
        if (!checkJobReportTimeout(a)) {
            return;
        }
        if (checkRetryNeeded(a)) {
            log.info("Retry start job {}.", a.getId());
            try {
                getConfiguration().getTaskFrameworkService().updateStatus(a.getId(), JobStatus.RETRYING);
                JobContext jc = new DefaultJobContextBuilder().build(a, getConfiguration().getHostUrlProvider());
                getConfiguration().getJobDispatcher().start(jc);
            } catch (Throwable e) {
                log.warn("Retry start job {} failed: ", a.getId(), e);
            }
        } else {
            log.info("No need to restart job {}, try to stop it and set status to failed.", a.getId());
            try {
                getConfiguration().getJobDispatcher().stop(JobIdentity.of(a.getId()));
                getConfiguration().getTaskFrameworkService().updateStatus(a.getId(), JobStatus.FAILED);
                getConfiguration().getTaskFrameworkService()
                        .updateDescription(a.getId(), "Report timeout and job failed.");
            } catch (Throwable e) {
                log.warn("try to stop job {} failed: ", a.getId(), e);
            }
        }
    }

    private boolean checkJobReportTimeout(JobEntity a) {
        long baseTime = (a.getLastReportTime() != null ? a.getLastReportTime()
                : (a.getStartedTime() != null ? a.getStartedTime() : a.getCreateTime())).getTime();
        return JobDateUtils.getCurrentDate().getTime() - baseTime > TimeUnit.MILLISECONDS.convert(
                getConfiguration().getTaskFrameworkProperties().getJobExpiredDurationSeconds(), TimeUnit.SECONDS);
    }

    private boolean checkRetryNeeded(JobEntity je) {
        JobProperties jobProperties = JsonUtils.fromJson(je.getJobPropertiesJson(), JobProperties.class);
        if (jobProperties == null || !jobProperties.isEnableRetryAfterReportTimeout()) {
            return false;
        }
        int maxRetryTimes = jobProperties.getMaxRetryTimesAfterReportTimeout() != null
                ? jobProperties.getMaxRetryTimesAfterReportTimeout()
                : getConfiguration().getTaskFrameworkProperties().getMaxRetryTimesAfterReportTimeout();

        return maxRetryTimes - je.getExecutionTimes() > 0;
    }

    private JobConfiguration getConfiguration() {
        return configuration;
    }
}
