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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.concurrent.Await;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.alarm.AlarmEventNames;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.monitor.task.job.JobMonitorListener;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.config.TaskFrameworkProperties;
import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.enums.TaskMonitorMode;
import com.oceanbase.odc.service.task.enums.TaskRunMode;
import com.oceanbase.odc.service.task.exception.JobException;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;
import com.oceanbase.odc.service.task.listener.DefaultJobCallerListener;
import com.oceanbase.odc.service.task.schedule.daemon.CheckRunningJob;
import com.oceanbase.odc.service.task.schedule.daemon.DestroyExecutorJob;
import com.oceanbase.odc.service.task.schedule.daemon.DoCancelingJob;
import com.oceanbase.odc.service.task.schedule.daemon.ManagerResourceJob;
import com.oceanbase.odc.service.task.schedule.daemon.PullTaskResultJob;
import com.oceanbase.odc.service.task.schedule.daemon.StartPreparingJob;
import com.oceanbase.odc.service.task.schedule.daemon.v2.DoFinishJobV2;
import com.oceanbase.odc.service.task.schedule.daemon.v2.DoStopJobV2;
import com.oceanbase.odc.service.task.schedule.daemon.v2.ManagerResourceJobV2;
import com.oceanbase.odc.service.task.schedule.daemon.v2.PullTaskResultJobV2;
import com.oceanbase.odc.service.task.schedule.daemon.v2.StartPreparingJobV2;
import com.oceanbase.odc.service.task.service.JobRunnable;
import com.oceanbase.odc.service.task.util.JobUtils;
import com.oceanbase.odc.service.task.util.TaskSupervisorUtil;

import cn.hutool.core.util.StrUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
@Slf4j
public class StdJobScheduler implements JobScheduler {
    private static final String START_PREPARING_JOB_V2_KEY_NAME = "StartPreparingJobV2";
    private static final String PULL_TASK_RESULT_JOB_V2_KEY_NAME = "PullTaskResultJobV2";
    private static final String DO_STOP_JOB_V2_KEY_NAME = "DoStopJobV2";
    private static final String DO_FINISH_JOB_V2_KEY_NAME = "DoFinishJobV2";
    private static final String MANAGE_RESOURCE_JOB_V2_KEY_NAME = "ManagerResourceJobV2";


    private final Scheduler scheduler;
    private final JobConfiguration configuration;
    private final TaskFrameworkProperties taskFrameworkProperties;

    public StdJobScheduler(JobConfiguration configuration) {
        this.configuration = configuration;
        this.scheduler = configuration.getDaemonScheduler();
        this.taskFrameworkProperties = configuration.getTaskFrameworkProperties();
        validConfiguration(configuration);
        JobConfigurationHolder.setJobConfiguration(configuration);

        getEventPublisher().addEventListener(new DefaultJobCallerListener(this));
        getEventPublisher().addEventListener(new JobMonitorListener());
        if (taskFrameworkProperties.isEnableTaskSupervisorAgent()) {
            initDaemonJobV2();
        } else {
            disableSchedulerV2();
            initDaemonJob();
        }
        log.info("Start StdJobScheduler succeed.");
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
        doInTransaction(() -> tryCanceling(id));
    }

    @Override
    public void modifyJobParameters(Long jobId, Map<String, String> jobParameters) throws JobException {
        doInTransaction(() -> doModifyJobParameters(jobId, jobParameters));
    }

    private void doInTransaction(JobRunnable r) throws JobException {
        try {
            configuration.getTransactionManager().doInTransactionWithoutResult(() -> {
                try {
                    r.run();
                } catch (JobException e) {
                    throw new TaskRuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new JobException("Do in transaction occur error:", e);
        }

    }

    private void doModifyJobParameters(Long jobId, @NonNull Map<String, String> jobParameters) throws JobException {
        JobEntity jobEntity = configuration.getTaskFrameworkService().findWithPessimisticLock(jobId);
        if (jobEntity == null) {
            throw new JobException("Job is not found by id, jobId={0}.", jobId);
        }
        if (!jobEntity.getStatus().isExecuting()) {
            throw new JobException("Job is not executing, jobId={0}, jobStatus={1}.", jobId, jobEntity.getStatus());
        }
        // partial update
        Map<String, String> oldJobParameters = JsonUtils.fromJson(jobEntity.getJobParametersJson(),
                new TypeReference<Map<String, String>>() {});
        oldJobParameters.putAll(jobParameters);
        String newJobParametersJson = JobUtils.toJson(oldJobParameters);
        configuration.getTaskFrameworkService().updateJobParameters(jobId, newJobParametersJson);

        if (jobEntity.getStatus() == JobStatus.PREPARING) {
            return;
        }
        // send new parameters to executor
        if (jobEntity.getStatus() == JobStatus.RUNNING) {
            configuration.getJobDispatcher().modify(JobIdentity.of(jobId), newJobParametersJson);
        }
    }

    @Override
    public void await(Long jobId, Integer timeout, TimeUnit timeUnit) throws InterruptedException {
        Await.await().timeUnit(timeUnit).timeout(timeout).period(10).periodTimeUnit(TimeUnit.SECONDS)
                .until(() -> configuration.getTaskFrameworkService().isJobFinished(jobId))
                .build().start();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return configuration.getEventPublisher();
    }

    private void tryCanceling(Long jobId) throws JobException {
        JobEntity jobEntity = configuration.getTaskFrameworkService().findWithPessimisticLock(jobId);
        if (!jobEntity.getStatus().isExecuting()) {
            throw new JobException("Cancel job failed, current job {0} status is {1}, can't be cancel.",
                    jobEntity.getId(), jobEntity.getStatus());
        }
        log.info("Prepare cancel task, jobId={}.", jobEntity.getId());
        try {
            configuration.getJobDispatcher().stop(JobIdentity.of(jobEntity.getId()));
        } catch (JobException e) {
            log.warn("Stop job occur error: ", e);
            Map<String, String> eventMessage = AlarmUtils.createAlarmMapBuilder()
                    .item(AlarmUtils.ORGANIZATION_NAME, Optional.ofNullable(jobEntity.getOrganizationId()).map(
                            Object::toString).orElse(StrUtil.EMPTY))
                    .item(AlarmUtils.TASK_JOB_ID_NAME, String.valueOf(jobId))
                    .item(AlarmUtils.MESSAGE_NAME,
                            MessageFormat.format("Cancel job failed, jobId={0}, message={1}", jobEntity.getId(),
                                    e.getMessage()))
                    .build();
            AlarmUtils.alarm(AlarmEventNames.TASK_CANCELED_FAILED, eventMessage);
            throw new TaskRuntimeException(e);
        }
        int count = configuration.getTaskFrameworkService().updateJobToCanceling(jobId, jobEntity.getStatus());
        if (count <= 0) {
            throw new JobException("Cancel job failed, current job {0} status is {1}.",
                    jobEntity.getId(), jobEntity.getStatus());
        } else {
            log.info("Update job {} status to {}", jobId, JobStatus.CANCELING.name());
        }
    }

    private void initDaemonJob() {
        initCheckRunningJob();
        initPullTaskResultJob();
        initStartPreparingJob();
        initDoCancelingJob();
        initDestroyExecutorJob();
        initManageResource();
    }

    private void initDaemonJobV2() {
        initStartPreparingJobV2();
        initPullResultJobV2();
        initDoStopJobV2();
        initDoFinishJobV2();
        initManagerResourceJobV2();
    }

    private void disableSchedulerV2() {
        Scheduler taskSupervisorScheduler = configuration.getTaskSupervisorScheduler();
        cancelCronJob(START_PREPARING_JOB_V2_KEY_NAME, taskSupervisorScheduler);
        cancelCronJob(PULL_TASK_RESULT_JOB_V2_KEY_NAME, taskSupervisorScheduler);
        cancelCronJob(DO_STOP_JOB_V2_KEY_NAME, taskSupervisorScheduler);
        cancelCronJob(DO_FINISH_JOB_V2_KEY_NAME, taskSupervisorScheduler);
        cancelCronJob(MANAGE_RESOURCE_JOB_V2_KEY_NAME, taskSupervisorScheduler);
    }

    // v2 daemon job
    private void initStartPreparingJobV2() {
        log.info("start with supervisor preparing job");
        initCronJob(START_PREPARING_JOB_V2_KEY_NAME,
                configuration.getTaskFrameworkProperties().getStartPreparingJobV2CronExpression(),
                StartPreparingJobV2.class, configuration.getTaskSupervisorScheduler());
    }

    private void initPullResultJobV2() {
        log.info("start with supervisor pull task result job");
        initCronJob(PULL_TASK_RESULT_JOB_V2_KEY_NAME,
                configuration.getTaskFrameworkProperties().getPullTaskResultJobV2CronExpression(),
                PullTaskResultJobV2.class, configuration.getTaskSupervisorScheduler());
    }

    private void initDoStopJobV2() {
        log.info("start with supervisor do stop job");
        initCronJob(DO_STOP_JOB_V2_KEY_NAME,
                configuration.getTaskFrameworkProperties().getDoStopJobCronV2Expression(),
                DoStopJobV2.class, configuration.getTaskSupervisorScheduler());
    }

    private void initDoFinishJobV2() {
        log.info("start with supervisor do finish job");
        initCronJob(DO_FINISH_JOB_V2_KEY_NAME,
                configuration.getTaskFrameworkProperties().getDoFinishJobV2CronExpression(),
                DoFinishJobV2.class, configuration.getTaskSupervisorScheduler());
    }

    private void initManagerResourceJobV2() {
        log.info("start with supervisor manage resource job");
        initCronJob(MANAGE_RESOURCE_JOB_V2_KEY_NAME,
                configuration.getTaskFrameworkProperties().getManageResourceJobV2CronExpression(),
                ManagerResourceJobV2.class, configuration.getTaskSupervisorScheduler());
    }

    // old daemon job
    private void initCheckRunningJob() {
        String key = "checkRunningJob";
        initCronJob(key,
                configuration.getTaskFrameworkProperties().getCheckRunningJobCronExpression(),
                CheckRunningJob.class, scheduler);
    }

    private void initPullTaskResultJob() {
        TaskMonitorMode monitorMode = taskFrameworkProperties.getMonitorMode();
        if (monitorMode == TaskMonitorMode.PUSH) {
            return;
        }
        String key = "pullTaskResultJob";
        initCronJob(key,
                configuration.getTaskFrameworkProperties().getPullTaskResultJobCronExpression(),
                PullTaskResultJob.class, scheduler);
    }

    private void initStartPreparingJob() {
        log.info("start with normal preparing job");
        String key = "startPreparingJob";
        initCronJob(key,
                configuration.getTaskFrameworkProperties().getStartPreparingJobCronExpression(),
                StartPreparingJob.class, scheduler);
    }

    private void initDoCancelingJob() {
        String key = "doCancelingJob";
        initCronJob(key,
                configuration.getTaskFrameworkProperties().getDoCancelingJobCronExpression(),
                DoCancelingJob.class, scheduler);
    }

    private void initDestroyExecutorJob() {
        String key = "destroyExecutorJob";
        initCronJob(key,
                configuration.getTaskFrameworkProperties().getDestroyExecutorJobCronExpression(),
                DestroyExecutorJob.class, scheduler);
    }

    private void initManageResource() {
        if (TaskSupervisorUtil.isTaskSupervisorEnabled(taskFrameworkProperties)
                || taskFrameworkProperties.getRunMode() == TaskRunMode.K8S) {
            String key = "managerResourceJob";
            initCronJob(key,
                    configuration.getTaskFrameworkProperties().getDestroyExecutorJobCronExpression(),
                    ManagerResourceJob.class, scheduler);
        }
    }

    private void initCronJob(String key, String cronExpression, Class<? extends Job> jobClass, Scheduler scheduler) {
        TriggerConfig config = new TriggerConfig();
        config.setTriggerStrategy(TriggerStrategy.CRON);
        config.setCronExpression(cronExpression);
        try {
            String group = JobConstants.ODC_JOB_MONITORING;
            TriggerKey triggerKey = TriggerKey.triggerKey(key, group);

            Trigger trigger = TriggerBuilder.build(triggerKey, config);
            JobDetail detail = JobBuilder.newJob(jobClass)
                    .withIdentity(JobKey.jobKey(key, group))
                    .build();
            scheduleCronJob(triggerKey, trigger, detail, scheduler);
        } catch (JobException e) {
            log.warn("build trigger {} failed:", key, e);
        } catch (SchedulerException e) {
            log.warn("schedule job failed:", e);
        }
    }

    private void cancelCronJob(String key, Scheduler scheduler) {
        String group = JobConstants.ODC_JOB_MONITORING;
        JobKey jobKey = JobKey.jobKey(key, group);
        try {
            scheduler.deleteJob(jobKey);
            log.info("delete job key = {} succeed", jobKey);
        } catch (SchedulerException e) {
            log.warn("delete job key = {} failed, reason = {}", jobKey, e.getMessage());
        }
    }

    private void scheduleCronJob(TriggerKey triggerKey, Trigger trigger, JobDetail detail, Scheduler scheduler)
            throws SchedulerException {
        if (scheduler.checkExists(triggerKey)) {
            if (scheduler.getTrigger(triggerKey) instanceof CronTrigger && trigger instanceof CronTrigger) {
                if (!Objects.equals(((CronTrigger) scheduler.getTrigger(triggerKey)).getCronExpression(),
                        ((CronTrigger) trigger).getCronExpression())) {
                    scheduler.rescheduleJob(triggerKey, trigger);
                }
            }
        } else {
            scheduler.scheduleJob(detail, trigger);
        }
    }

    private void validConfiguration(JobConfiguration configuration) {
        PreConditions.notNull(configuration.getTaskFrameworkProperties(), "task-framework properties");
        PreConditions.notNull(configuration.getTaskFrameworkProperties().getCheckRunningJobCronExpression(),
                "checkRunningJobCronExpression");
        PreConditions.notNull(configuration.getTaskFrameworkProperties().getStartPreparingJobCronExpression(),
                "startPreparingJobCronExpression");
        if (!taskFrameworkProperties.isEnableTaskSupervisorAgent()) {
            PreConditions.notNull(configuration.getDaemonScheduler(), "quartz scheduler");
        } else {
            PreConditions.notNull(configuration.getTaskSupervisorScheduler(), "quartz task scheduler");
        }
        PreConditions.notNull(configuration.getJobDispatcher(), "job dispatcher");
        PreConditions.notNull(configuration.getHostUrlProvider(), "host url provider");
        PreConditions.notNull(configuration.getTaskFrameworkService(), "task framework service");
        PreConditions.notNull(configuration.getJobImageNameProvider(), "job image name provider");
        PreConditions.notNull(configuration.getTransactionManager(), "transaction manager");
    }

}
