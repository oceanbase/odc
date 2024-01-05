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

import org.apache.commons.collections4.CollectionUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
import com.oceanbase.odc.service.task.util.JobDateUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-01-04
 * @since 4.2.4
 */
@Slf4j
public class CheckExpiredJob implements Job {

    // job expired duration, last report time more than this time job will be set failed
    private final long jobExpiredDuration = TimeUnit.MILLISECONDS.convert(
            5 * 60 + 10 * JobConstants.REPORT_TASK_INFO_INTERVAL_SECONDS * 10, TimeUnit.SECONDS);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobConfiguration configuration = JobConfigurationHolder.getJobConfiguration();

        // scan preparing and running job
        TaskFrameworkService taskFrameworkService = configuration.getTaskFrameworkService();

        long currentTime = JobDateUtils.getCurrentDate().getTime();
        List<JobEntity> jobs;
        int offset = 1;
        int limit = 100;
        do {
            // check timeout job
            jobs = taskFrameworkService.find(JobStatus.RUNNING, offset, limit);
            jobs.forEach(a -> {
                long baseTime = (a.getLastReportTime() != null ? a.getLastReportTime() : a.getStartedTime()).getTime();
                if (currentTime - baseTime > jobExpiredDuration) {
                    try {
                        configuration.getJobDispatcher().stop(JobIdentity.of(a.getId()));
                        taskFrameworkService.updateStatus(a.getId(), JobStatus.FAILED);
                    } catch (JobException e) {
                        log.warn("try to stop job {} failed: ", a.getId(), e);
                    }
                }
            });
            offset = offset * jobs.size() + 1;

        } while (CollectionUtils.isEmpty(jobs) || offset >= 2000);
    }
}
