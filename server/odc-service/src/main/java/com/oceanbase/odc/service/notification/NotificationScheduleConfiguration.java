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

package com.oceanbase.odc.service.notification;

import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;

@EnableScheduling
@Configuration
@ConditionalOnProperty(value = "odc.notification.enabled", havingValue = "true")
public class NotificationScheduleConfiguration implements SchedulingConfigurer {
    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private Broker broker;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(3));

        taskRegistrar.addTriggerTask(() -> broker.dequeueEvent(EventStatus.CREATED),
                getTrigger(() -> Duration.ofMillis(notificationProperties.getDequeueEventFixedDelayMillis())));
        taskRegistrar.addTriggerTask(() -> broker.dequeueNotification(MessageSendingStatus.CREATED),
                getTrigger(() -> Duration
                        .ofMillis(notificationProperties.getDequeueCreatedNotificationFixedDelayMillis())));
        taskRegistrar.addTriggerTask(() -> broker.dequeueNotification(MessageSendingStatus.SENT_FAILED),
                getTrigger(() -> Duration
                        .ofMillis(notificationProperties.getDequeueFailedNotificationFixedDelayMillis())));
    }

    private Trigger getTrigger(Supplier<Duration> durationSupplier) {

        return triggerContext -> {
            Calendar nextExecutionTime = Calendar.getInstance();
            Date lastActualExecutionTime = triggerContext.lastActualExecutionTime();
            nextExecutionTime.setTime(lastActualExecutionTime != null ? lastActualExecutionTime : new Date());
            nextExecutionTime.add(Calendar.MILLISECOND, (int) durationSupplier.get().toMillis());
            return nextExecutionTime.getTime();
        };

    }

}
