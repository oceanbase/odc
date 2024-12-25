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
public class QuartzJobServiceProxy implements QuartzJobService {
    @Autowired
    @Qualifier("scheduleTaskJobServiceImpl")
    private ScheduleTaskJobServiceImpl scheduleTaskJobService;
    @Autowired
    @Qualifier("quartzJobServiceImpl")
    private QuartzJobServiceImpl quartzJobService;

    @Override
    public void createJob(CreateQuartzJobParam req) throws SchedulerException {
        getQuartzJobServiceImpl(req.getJobClass()).createJob(req);
    }

    @Override
    public void createJob(CreateQuartzJobParam req, JobDataMap triggerDataMap) throws SchedulerException {
        getQuartzJobServiceImpl(req.getJobClass()).createJob(req, triggerDataMap);
    }

    @Override
    public void changeJob(ChangeQuartJobParam req) {
        getQuartzJobServiceImpl(req.getJobClass()).changeJob(req);
    }

    @Override
    public void pauseJob(JobKey jobKey) throws SchedulerException {
        getQuartzJobServiceImpl(jobKey.getGroup()).pauseJob(jobKey);
    }

    @Override
    public void resumeJob(JobKey jobKey) throws SchedulerException {
        getQuartzJobServiceImpl(jobKey.getGroup()).resumeJob(jobKey);
    }

    @Override
    public void deleteJob(JobKey jobKey) throws SchedulerException {
        getQuartzJobServiceImpl(jobKey.getGroup()).deleteJob(jobKey);
    }

    @Override
    public boolean checkExists(JobKey jobKey) throws SchedulerException {
        return getQuartzJobServiceImpl(jobKey.getGroup()).checkExists(jobKey);
    }

    @Override
    public Trigger getTrigger(TriggerKey key) throws SchedulerException {
        return getQuartzJobServiceImpl(key.getGroup()).getTrigger(key);
    }

    @Override
    public void rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) throws SchedulerException {
        getQuartzJobServiceImpl(triggerKey.getGroup()).rescheduleJob(triggerKey, newTrigger);
    }

    @Override
    public void triggerJob(JobKey key) throws SchedulerException {
        getQuartzJobServiceImpl(key.getGroup()).triggerJob(key);
    }

    @Override
    public void interruptJob(JobKey key) throws UnableToInterruptJobException {
        getQuartzJobServiceImpl(key.getGroup()).interruptJob(key);
    }

    @Override
    public void triggerJob(JobKey key, JobDataMap triggerDataMap) throws SchedulerException {
        getQuartzJobServiceImpl(key.getGroup()).triggerJob(key, triggerDataMap);
    }

    @Override
    public List<? extends Trigger> getJobTriggers(JobKey jobKey) throws SchedulerException {
        return getQuartzJobServiceImpl(jobKey.getGroup()).getJobTriggers(jobKey);
    }

    private QuartzJobService getQuartzJobServiceImpl(String group) {
        for (ScheduleTaskType type : ScheduleTaskType.values()) {
            if (type.name().equalsIgnoreCase(group)) {
                return scheduleTaskJobService;
            }
        }
        return quartzJobService;
    }

    private QuartzJobService getQuartzJobServiceImpl(Class<? extends Job> jobClass) {
        return QuartzJob.class.isAssignableFrom(jobClass) ? scheduleTaskJobService : quartzJobService;
    }
}
