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
package com.oceanbase.odc.service.quartz;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.quartz.executor.QuartzJob;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.quartz.util.QuartzCronExpressionUtils;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobReq;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

/**
 * @Author：tinker
 * @Date: 2022/11/14 18:28
 * @Descripition:
 */

@Service
@SkipAuthorize("odc internal usage")
public class QuartzJobService {

    @Autowired
    private Scheduler scheduler;

    public void createJob(CreateQuartzJobReq req) throws SchedulerException {
        createJob(req, null);
    }

    public void createJob(CreateQuartzJobReq req, JobDataMap triggerDataMap) throws SchedulerException {

        JobKey jobKey = QuartzKeyGenerator.generateJobKey(req.getScheduleId(), req.getType());

        Class<? extends Job> clazz = QuartzJob.class;

        if (req.getTriggerConfig() != null) {
            JobDataMap forTrigger = triggerDataMap == null ? new JobDataMap(new HashMap<>(1)) : triggerDataMap;
            TriggerKey triggerKey = QuartzKeyGenerator.generateTriggerKey(req.getScheduleId(), req.getType());
            Trigger trigger = buildTrigger(triggerKey, req.getTriggerConfig(), req.getMisfireStrategy(), forTrigger);
            JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(jobKey)
                    .usingJobData(req.getJobDataMap()).build();
            scheduler.scheduleJob(jobDetail, trigger);
        } else {
            JobDetail jobDetail = JobBuilder.newJob(clazz).withIdentity(jobKey)
                    .usingJobData(req.getJobDataMap()).storeDurably(true).build();
            scheduler.addJob(jobDetail, false);
        }
    }

    private Trigger buildTrigger(TriggerKey key, TriggerConfig config,
            MisfireStrategy misfireStrategy, JobDataMap triggerDataMap) throws SchedulerException {
        switch (config.getTriggerStrategy()) {
            case START_NOW:
                return TriggerBuilder
                        .newTrigger().withIdentity(key).withSchedule(SimpleScheduleBuilder
                                .simpleSchedule().withRepeatCount(0).withMisfireHandlingInstructionFireNow())
                        .startNow().usingJobData(triggerDataMap).build();
            case START_AT:
                return TriggerBuilder.newTrigger().withIdentity(key)
                        .withSchedule(SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0)
                                .withMisfireHandlingInstructionFireNow())
                        .startAt(config.getStartAt()).usingJobData(triggerDataMap).build();
            case CRON:
            case DAY:
            case MONTH:
            case WEEK: {

                String cron;

                try {
                    cron = QuartzCronExpressionUtils
                            .adaptCronExpression(config.getCronExpression());
                } catch (ParseException e) {
                    throw new SchedulerException("Invalid cron expression,create quartz job failed.");
                }

                CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron);

                switch (misfireStrategy) {
                    case MISFIRE_INSTRUCTION_IGNORE_MISFIRE:
                        cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                        break;
                    case MISFIRE_INSTRUCTION_FIRE_ONCE_NOW:
                        cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
                        break;
                    default:
                        cronScheduleBuilder = cronScheduleBuilder.withMisfireHandlingInstructionDoNothing();
                        break;
                }

                return TriggerBuilder.newTrigger().withIdentity(key)
                        .withSchedule(cronScheduleBuilder).usingJobData(triggerDataMap).build();

            }
            default:
                throw new UnsupportedException();
        }
    }

    public void pauseJob(JobKey jobKey) throws SchedulerException {
        scheduler.pauseJob(jobKey);
    }

    public void resumeJob(JobKey jobKey) throws SchedulerException {
        scheduler.resumeJob(jobKey);
    }

    public void deleteJob(JobKey jobKey) throws SchedulerException {
        scheduler.deleteJob(jobKey);
    }

    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        return scheduler.checkExists(jobKey);
    }

    public Trigger getTrigger(TriggerKey key) throws SchedulerException {
        return scheduler.getTrigger(key);
    }

    public void updateTriggerDataMap(TriggerKey triggerKey, JobDataMap triggerDataMap) throws SchedulerException {
        Trigger trigger = getTrigger(triggerKey);
        if (trigger != null) {
            trigger.getJobDataMap().putAll(triggerDataMap);
            scheduler.rescheduleJob(triggerKey, trigger);
        }
    }

    public void triggerJob(JobKey key) throws SchedulerException {
        scheduler.triggerJob(key);
    }

    public void interruptJob(JobKey key) throws UnableToInterruptJobException {
        scheduler.interrupt(key);
    }

    public void triggerJob(JobKey key, JobDataMap triggerDataMap) throws SchedulerException {
        scheduler.triggerJob(key, triggerDataMap);
    }

    public List<? extends Trigger> getJobTriggers(JobKey jobKey) throws SchedulerException {
        return scheduler.getTriggersOfJob(jobKey);
    }
}
