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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/22 15:50
 * @Description: []
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "odc.notification.enabled", havingValue = "true")
public class NotificationSchedules {
    @Autowired
    private Broker broker;

    @Autowired
    private NotificationProperties notificationProperties;

    @Scheduled(fixedDelayString = "${odc.notification.dequeue-event-fixed-delay-millis:30000}")
    public void convertEventToNotification() {
        broker.dequeueEvent(EventStatus.CREATED);
    }

    @Scheduled(fixedDelayString = "${odc.notification.dequeue-created-notification-fixed-delay-millis:30000}")
    public void sendNotification() {
        broker.dequeueNotification(MessageSendingStatus.CREATED);
    }

    @Scheduled(fixedDelayString = "${odc.notification.dequeue-failed-notification-fixed-delay-millis:30000}")
    public void resendNotification() {
        broker.dequeueNotification(MessageSendingStatus.SENT_FAILED);
    }


}
