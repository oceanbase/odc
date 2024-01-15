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

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.executor.TaskRuntimeException;
import com.oceanbase.odc.service.task.listener.DefaultJobCallerListener;
import com.oceanbase.odc.service.task.listener.DestroyExecutorListener;
import com.oceanbase.odc.service.task.schedule.daemon.CheckRunningJob;
import com.oceanbase.odc.service.task.schedule.daemon.DoCancelingJob;
import com.oceanbase.odc.service.task.schedule.daemon.StartPreparingJob;

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
        validConfiguration(configuration);
        JobConfigurationHolder.setJobConfiguration(configuration);

        log.info("Job image name is {}", configuration.getJobImageNameProvider().provide());
        getEventPublisher().addEventListener(new DestroyExecutorListener(configuration));
        getEventPublisher().addEventListener(new DefaultJobCallerListener(this));
        initDaemonJob();
    }


    @Override
    public Long scheduleJobNow(JobDefinition jd) {
        PreConditions.notNull(jd, "job definition");
        PreConditions.notNull(jd.getJobType(), "job type");
        PreConditions.notNull(jd.getJobClass(), "job class");
        return scheduleJob(jd);
    }

    private Long scheduleJob(JobDefinition jd) {
        JobEntity jobEntity = configuration.getTaskFrameworkService().save(jd);
        return jobEntity.getId();
    }

    @Override
    public void cancelJob(Long id) throws JobException {
        configuration.getTransactionManager().doInTransactionWithoutResult(() -> {
            return TryCanceling(id);
        });
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

    private Void TryCanceling(Long id) {
        JobEntity jobEntity = configuration.getTaskFrameworkService().findWithLock(id);
        if (!cancelable(jobEntity.getStatus())) {
            throw new TaskRuntimeException(
                    MessageFormat.format("Cancel job failed, current job {0} status is {1}, can't be cancel.",
                            jobEntity.getId(), jobEntity.getStatus()));
        }
        int count = configuration.getTaskFrameworkService().updateJobToCanceling(id, jobEntity.getStatus());
        if (count <= 0) {
            throw new TaskRuntimeException(MessageFormat.format("Cancel job failed, current job {0} status is {1}.",
                    jobEntity.getId(), jobEntity.getStatus()));
        } else {
            log.info("Update job {} status to {}", id, JobStatus.CANCELING.name());
        }
        return null;
    }

    private boolean cancelable(JobStatus status) {
        List<JobStatus> list = Lists.newArrayList(JobStatus.PREPARING, JobStatus.RUNNING, JobStatus.RETRYING);
        return list.contains(status);
    }

    private void initDaemonJob() {
        initCheckRunningJob();
        initStartPreparingJob();
        initDoCancelingJob();
    }

    private void initCheckRunningJob() {
        String key = "checkRunningJob";
        initCronJob(key, key + "Group",
                configuration.getTaskFrameworkProperties().getCheckRunningJobCronExpression(),
                CheckRunningJob.class);
    }

    private void initStartPreparingJob() {
        String key = "startPreparingJob";
        initCronJob(key, key + "Group",
                configuration.getTaskFrameworkProperties().getStartPreparingJobCronExpression(),
                StartPreparingJob.class);
    }

    private void initDoCancelingJob() {
        String key = "doCancelingJob";
        initCronJob(key, key + "Group",
                configuration.getTaskFrameworkProperties().getDoCancelingJobCronExpression(),
                DoCancelingJob.class);
    }

    private void initCronJob(String key, String group, String cronExpression, Class<? extends Job> jobClass) {
        TriggerConfig config = new TriggerConfig();
        config.setTriggerStrategy(TriggerStrategy.CRON);
        config.setCronExpression(cronExpression);
        try {
            TriggerKey triggerKey = TriggerKey.triggerKey(key, group);
            JobKey jobKey = JobKey.jobKey(key, group);
            Trigger trigger = TriggerBuilder.build(triggerKey, config);
            JobDetail detail = JobBuilder.newJob(jobClass)
                    .withIdentity(JobKey.jobKey(key, group))
                    .build();
            if (scheduler.checkExists(triggerKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(detail, trigger);
        } catch (JobException e) {
            log.warn("build trigger {} failed:", key, e);
        } catch (SchedulerException e) {
            log.warn("schedule job failed:", e);
        }
    }

    private void validConfiguration(JobConfiguration configuration) {
        PreConditions.notNull(configuration.getTaskFrameworkProperties(), "task-framework properties");
        PreConditions.notNull(configuration.getTaskFrameworkProperties().getCheckRunningJobCronExpression(),
                "checkRunningJobCronExpression");
        PreConditions.notNull(configuration.getTaskFrameworkProperties().getStartPreparingJobCronExpression(),
                "startPreparingJobCronExpression");
        PreConditions.notNull(configuration.getScheduler(), "quartz scheduler");
        PreConditions.notNull(configuration.getJobDispatcher(), "job dispatcher");
        PreConditions.notNull(configuration.getHostUrlProvider(), "host url provider");
        PreConditions.notNull(configuration.getTaskFrameworkService(), "task framework service");
        PreConditions.notNull(configuration.getJobImageNameProvider(), "job image name provider");
        PreConditions.notNull(configuration.getTransactionManager(), "transaction manager");
    }

}
