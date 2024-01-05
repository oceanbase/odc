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

import java.util.concurrent.TimeUnit;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.JobDetailImpl;

import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.listener.DefaultJobCallerListener;
import com.oceanbase.odc.service.task.listener.DestroyJobListener;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
@Slf4j
public class StdJobScheduler implements JobScheduler {

    private final Scheduler scheduler;
    private final JobConfiguration configuration;

    public StdJobScheduler(JobConfiguration configuration) {
        this.configuration = configuration;
        this.scheduler = configuration.getScheduler();
        PreConditions.notNull(configuration.getScheduler(), "quartz scheduler");
        PreConditions.notNull(configuration.getJobDispatcher(), "job dispatcher");
        PreConditions.notNull(configuration.getHostUrlProvider(), "host url provider");
        PreConditions.notNull(configuration.getTaskFrameworkService(), "task framework service");
        JobConfigurationHolder.setJobConfiguration(configuration);

        getEventPublisher().addEventListener(new DestroyJobListener(this, configuration));
        getEventPublisher().addEventListener(new DefaultJobCallerListener(this));
    }

    @Override
    public Long scheduleJobNow(JobDefinition jd) throws JobException {
        PreConditions.notNull(jd, "job definition");
        return scheduleJob(jd);
    }

    private Long scheduleJob(JobDefinition jd) throws JobException {
        JobEntity jobEntity = configuration.getTaskFrameworkService().save(jd);
        JobIdentity jobIdentity = JobIdentity.of(jobEntity.getId());
        Trigger trigger = TriggerBuilder.build(jobIdentity, jd, null);
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(jobEntity.getId());
        JobDetailImpl detail = new JobDetailImpl();
        detail.setKey(jobKey);
        detail.setJobClass(PrepareCallJob.class);

        try {
            scheduler.scheduleJob(detail, trigger);
        } catch (SchedulerException e) {
            throw new JobException("add and schedule job failed:", e);
        }
        return jobEntity.getId();
    }

    @Override
    public void cancelJob(Long id) throws JobException {
        JobEntity jobEntity = configuration.getTaskFrameworkService().find(id);
        if (jobEntity.getStatus() == JobStatus.CANCELING || jobEntity.getStatus() == JobStatus.CANCELED) {
            log.warn("Job {} status is {},can not be cancelled.", id, jobEntity.getStatus().name());
            return;
        }
        JobKey jobKey = QuartzKeyGenerator.generateJobKey(id);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException e) {
            throw new JobException(e);
        }


        configuration.getTaskFrameworkService().updateStatus(id, JobStatus.CANCELING);
        log.info("Update job {} status to {}", id, JobStatus.CANCELING.name());
        configuration.getJobDispatcher().stop(JobIdentity.of(id));
        configuration.getTaskFrameworkService().updateStatus(id, JobStatus.CANCELED);
        log.info("Update job {} status to {}", id, JobStatus.CANCELED.name());
    }

    @Override
    public void await(Long id, Integer timeout, TimeUnit timeUnit) throws InterruptedException {
        Await.await().timeUnit(timeUnit).timeout(timeout).period(10).periodTimeUnit(TimeUnit.SECONDS)
                .until(() -> configuration.getTaskFrameworkService().isJobFinished(id))
                .build().start();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return configuration.getEventPublisher();
    }
}
