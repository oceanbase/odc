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

import static com.oceanbase.odc.core.alarm.AlarmEventNames.SCHEDULING_FAILED;
import static com.oceanbase.odc.core.alarm.AlarmEventNames.SCHEDULING_IGNORE;

import java.util.Date;
import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.metadb.schedule.ScheduleRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.notification.Broker;
import com.oceanbase.odc.service.notification.NotificationProperties;
import com.oceanbase.odc.service.notification.helper.EventBuilder;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.alarm.ScheduleAlarmUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/23
 */
@Slf4j
@Component
public class OdcTriggerListener extends TriggerListenerSupport {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;

    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private Broker broker;
    @Autowired
    private EventBuilder eventBuilder;

    @Override
    public String getName() {
        return "ODC_TRIGGER_LISTENER";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        log.info("Job is fired,jobKey={}", trigger.getJobKey());
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        boolean skipExecution = SpringContextUtil.getBean(ScheduleService.class)
                .vetoJobExecution(trigger);
        if (skipExecution) {
            log.warn("The job will be skipped, job key:" + trigger.getJobKey());
            ScheduleAlarmUtils.misfire(Long.parseLong(trigger.getJobKey().getName()), new Date());
        }
        return skipExecution;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        log.warn("Job is misfired, job key:" + trigger.getJobKey());
        try {
            Optional<ScheduleTaskEntity> latestTaskEntity =
                    scheduleTaskRepository.getLatestScheduleTaskByJobNameAndJobGroup(trigger.getJobKey().getName(),
                            trigger.getJobKey().getGroup());
            if (!latestTaskEntity.isPresent() || latestTaskEntity.get().getStatus().isTerminated()) {
                log.warn("Previous task is terminated,this misfire is unexpected,jobKey={}", trigger.getJobKey());
                AlarmUtils.alarm(SCHEDULING_FAILED, "Job is misfired, job key:" + trigger.getJobKey());
            } else {
                AlarmUtils.alarm(SCHEDULING_IGNORE,
                        "The Job has reached its trigger time, but the previous task has not yet finished. This scheduling will be ignored, job key:"
                                + trigger.getJobKey());
            }
        } catch (Exception e) {
            log.warn("Get previous task status failed,jobKey={}", trigger.getJobKey());
            AlarmUtils.alarm(SCHEDULING_FAILED, "Job is misfired, job key:" + trigger.getJobKey());
        }
        if (!notificationProperties.isEnabled()) {
            return;
        }
        ScheduleAlarmUtils.misfire(Long.parseLong(trigger.getJobKey().getName()), new Date());
        try {
            JobKey jobKey = trigger.getJobKey();
            scheduleRepository.findById(Long.parseLong(jobKey.getName()))
                    .ifPresent(schedule -> {
                        Event event = eventBuilder.ofFailedSchedule(schedule);
                        broker.enqueueEvent(event);
                    });
        } catch (Exception e) {
            log.warn("Failed to enqueue event.", e);
        }
    }

}
