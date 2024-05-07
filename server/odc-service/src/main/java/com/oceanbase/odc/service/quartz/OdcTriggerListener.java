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

import java.util.Optional;

import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.oceanbase.odc.service.quartz.util.ScheduleTaskUtils;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;

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
    private ScheduleTaskRepository taskRepository;

    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private Broker broker;
    @Autowired
    private EventBuilder eventBuilder;
    @Value("${odc.task.allow-recovery:true}")
    private boolean allowRecovery;

    @Override
    public String getName() {
        return "ODC_TRIGGER_LISTENER";
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {

        if (context.isRecovering() && !allowRecovery) {
            log.info("Recovery is not allowed,jobKey={}", trigger.getJobKey());
            return true;
        }
        // If the Job is being re-executed because of a 'recovery' situation,bind it to an existing
        // execution record.
        if (context.isRecovering() && JobType.valueOf(context.getJobDetail().getKey().getGroup()).shouldRecover()) {
            Long targetTaskId = ScheduleTaskUtils.getTargetTaskId(context);
            Optional<ScheduleTaskEntity> targetTask = targetTaskId == null ? taskRepository.findLatestTaskByJobName(
                    context.getJobDetail().getKey().getName()) : taskRepository.findById(targetTaskId);

            if (!targetTask.isPresent() || targetTask.get().getStatus().isTerminated()) {
                log.info(
                        "The scheduled task has already been completed or cannot exist. No recovery is needed.Job key={}",
                        context.getJobDetail().getKey());
                return true;
            }
            context.getTrigger().getJobDataMap()
                    .putAll(ScheduleTaskUtils.buildTriggerDataMap(targetTask.get().getId()));
        }

        return SpringContextUtil.getBean(ScheduleService.class)
                .terminateIfDatabaseNotExisted(ScheduleTaskUtils.getScheduleId(context));
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        log.warn("Job is misfired, job key:" + trigger.getJobKey());
        AlarmUtils.alarm(SCHEDULING_FAILED, "Job is misfired, job key:" + trigger.getJobKey());
        if (!notificationProperties.isEnabled()) {
            return;
        }
        try {
            JobKey jobKey = trigger.getJobKey();
            scheduleRepository.findById(Long.parseLong(jobKey.getName()))
                    .ifPresent(schedule -> {
                        Event event = eventBuilder.ofFailedSchedule(schedule);
                        broker.enqueueEvent(event);
                        AlarmUtils.alarm(SCHEDULING_FAILED, event.toString());
                    });
        } catch (Exception e) {
            log.warn("Failed to enqueue event.", e);
        }
    }

}
