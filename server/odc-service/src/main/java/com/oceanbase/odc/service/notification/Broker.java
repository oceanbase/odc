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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.Notification;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:45
 * @Description: []
 */
@Service
@SkipAuthorize("currently not in use")
@Slf4j
public class Broker {
    @Autowired
    private EventQueue eventQueue;

    @Autowired
    private EventFilter eventFilter;

    @Autowired
    private Converter converter;

    @Autowired
    private NotificationDispatcher notificationDispatcher;

    @Autowired
    private NotificationQueue notificationQueue;

    @Autowired
    private NotificationProperties notificationProperties;

    @Transactional(rollbackFor = Exception.class)
    public void dequeueEvent(EventStatus eventStatus) {
        // 从事件队列中拉取事件
        List<Event> events = eventQueue.peek(notificationProperties.getEventDequeueBatchSize(), eventStatus);
        // 过滤掉不需要发送通知的事件
        List<Event> filtered = eventFilter.filter(events);
        // 将事件转换为通知
        List<Notification> notifications = converter.convert(filtered);
        // 通知进入通知队列，等待异步发送
        notificationQueue.offer(notifications);
    }

    @Transactional(rollbackFor = Exception.class)
    public void enqueueEvent(Event event) {
        eventQueue.offer(event);
    }


    public void dequeueNotification(MessageSendingStatus status) {
        List<Notification> notifications =
                notificationQueue.peek(notificationProperties.getNotificationDequeueBatchSize(), status);
        for (Notification notification : notifications) {
            try {
                notificationDispatcher.dispatch(notification);
            } catch (Exception e) {
                log.warn("Send notification failed.", e);
            }
        }
    }
}
