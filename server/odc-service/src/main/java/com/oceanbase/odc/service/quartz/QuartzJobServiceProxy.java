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

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.quartz.executor.QuartzJob;
import com.oceanbase.odc.service.schedule.model.ChangeQuartJobParam;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobParam;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;

/**
 * @author jingtian
 * @date 2024/12/25
 */
@Service("quartzJobServiceProxy")
@SkipAuthorize("odc internal usage")
public class QuartzJobServiceProxy {
    @Autowired
    @Qualifier("scheduleTaskJobService")
    private ScheduleTaskJobService scheduleTaskJobService;
    @Autowired
    @Qualifier("quartzJobService")
    private QuartzJobService quartzJobService;

    public void createJob(CreateQuartzJobParam req) throws SchedulerException {
        getQuartzJobServiceImpl(req.getJobClass()).createJob(req);
    }

    public void createJob(CreateQuartzJobParam req, JobDataMap triggerDataMap) throws SchedulerException {
        getQuartzJobServiceImpl(req.getJobClass()).createJob(req, triggerDataMap);
    }

    public void changeJob(ChangeQuartJobParam req) {
        getQuartzJobServiceImpl(req.getJobClass()).changeJob(req);
    }

    public void pauseJob(JobKey jobKey) throws SchedulerException {
        getQuartzJobServiceImpl(jobKey.getGroup()).pauseJob(jobKey);
    }

    public void resumeJob(JobKey jobKey) throws SchedulerException {
        getQuartzJobServiceImpl(jobKey.getGroup()).resumeJob(jobKey);
    }

    public void deleteJob(JobKey jobKey) throws SchedulerException {
        getQuartzJobServiceImpl(jobKey.getGroup()).deleteJob(jobKey);
    }

    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        return getQuartzJobServiceImpl(jobKey.getGroup()).checkExists(jobKey);
    }

    public Trigger getTrigger(TriggerKey key) throws SchedulerException {
        return getQuartzJobServiceImpl(key.getGroup()).getTrigger(key);
    }

    public void rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
        getQuartzJobServiceImpl(triggerKey.getGroup()).rescheduleJob(triggerKey, newTrigger);
    }

    public void triggerJob(JobKey key) throws SchedulerException {
        getQuartzJobServiceImpl(key.getGroup()).triggerJob(key);
    }

    public void interruptJob(JobKey key) throws UnableToInterruptJobException {
        getQuartzJobServiceImpl(key.getGroup()).interruptJob(key);
    }

    public void triggerJob(JobKey key, JobDataMap triggerDataMap) throws SchedulerException {
        getQuartzJobServiceImpl(key.getGroup()).triggerJob(key, triggerDataMap);
    }

    public List<? extends Trigger> getJobTriggers(JobKey jobKey) throws SchedulerException {
        return getQuartzJobServiceImpl(jobKey.getGroup()).getJobTriggers(jobKey);
    }

    private AbstractQuartzJobService getQuartzJobServiceImpl(String group) {
        for (ScheduleTaskType type : ScheduleTaskType.values()) {
            if (type.name().equalsIgnoreCase(group)) {
                return scheduleTaskJobService;
            }
        }
        return quartzJobService;
    }

    private AbstractQuartzJobService getQuartzJobServiceImpl(Class<? extends Job> jobClass) {
        return QuartzJob.class.isAssignableFrom(jobClass) ? scheduleTaskJobService : quartzJobService;
    }
}
