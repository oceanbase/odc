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

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;

/**
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
public class StdJobScheduler implements JobScheduler {

    private final Scheduler scheduler;
    private final JobConfiguration configuration;

    public StdJobScheduler(JobConfiguration configuration) {
        this.configuration = configuration;
        this.scheduler = configuration.getScheduler();
        PreConditions.notNull(configuration.getScheduler(), "quartz scheduler");
        PreConditions.notNull(configuration.getJobDispatcher(), "job dispatcher");
        PreConditions.notNull(configuration.getHostUrlProvider(), "host url provider");
        JobConfigurationHolder.setJobConfiguration(configuration);
    }

    @Override
    public void scheduleJob(JobDefinition jd) throws JobException {
        PreConditions.notNull(jd, "job definition");

        Trigger trigger = TriggerBuilder.build(jd);
        JobIdentity jobIdentity = jd.getJobIdentity();
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(jobIdentity);
        JobDetailImpl detail = new JobDetailImpl();
        detail.setKey(jobKey);
        detail.setJobClass(PrepareCallJob.class);

        try {
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new JobException("add and schedule job failed:", e);
        }
    }

    @Override
    public void scheduleJobNow(JobDefinition jd) throws JobException {
        PreConditions.notNull(jd, "job definition");
        // if trigger config is null, will set schedule right now
        if (jd.getTriggerConfig() != null) {
            ((DefaultJobDefinition) jd).setTriggerConfig(null);
        }
        scheduleJob(jd);
    }

    @Override
    public void cancelJob(JobIdentity ji) throws JobException {
        configuration.getJobDispatcher().stop(ji);
    }
}
