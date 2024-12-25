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

import java.util.List;

import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;

import com.oceanbase.odc.service.schedule.model.ChangeQuartJobParam;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobParam;

/**
 * @author jingtian
 * @date 2024/12/25
 */
public interface QuartzJobService {
    void createJob(CreateQuartzJobParam req) throws SchedulerException;

    void createJob(CreateQuartzJobParam req, JobDataMap triggerDataMap) throws SchedulerException;

    void changeJob(ChangeQuartJobParam req);

    void pauseJob(JobKey jobKey) throws SchedulerException;

    void resumeJob(JobKey jobKey) throws SchedulerException;

    void deleteJob(JobKey jobKey) throws SchedulerException;

    boolean checkExists(JobKey jobKey) throws SchedulerException;

    Trigger getTrigger(TriggerKey key) throws SchedulerException;

    void rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException;

    void triggerJob(JobKey key) throws SchedulerException;

    void interruptJob(JobKey key) throws UnableToInterruptJobException;

    void triggerJob(JobKey key, JobDataMap triggerDataMap) throws SchedulerException;

    List<? extends Trigger> getJobTriggers(JobKey jobKey) throws SchedulerException;

}
