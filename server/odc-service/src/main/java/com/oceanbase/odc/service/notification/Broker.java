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
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.ExceptionUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.metadb.notification.MessageSendingHistoryEntity;
import com.oceanbase.odc.metadb.notification.MessageSendingHistoryRepository;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;

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
    private NotificationDispatcher dispatcher;

    @Autowired
    private NotificationQueue notificationQueue;

    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MessageSendingHistoryRepository sendingHistoryRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    public void dequeueEvent(EventStatus eventStatus) {
        try {
            // 从事件队列中拉取事件
            List<Event> events = eventQueue.peek(notificationProperties.getEventDequeueBatchSize(), eventStatus);
            // 过滤掉不需要发送通知的事件
            List<Event> filtered = eventFilter.filter(events);
            // 将事件转换为通知
            List<Message> messages = converter.convert(filtered);
            // 通知进入通知队列，等待异步发送
            notificationQueue.offer(messages);
        } catch (Exception e) {
            log.error("Failed to dequeue events.", e);
        }
    }

    public void enqueueEvent(Event event) {
        Optional<OrganizationEntity> optional = organizationRepository.findById(event.getOrganizationId());
        if (optional.isPresent() && optional.get().getType() == OrganizationType.INDIVIDUAL) {
            return;
        }
        eventQueue.offer(event);
    }

    public void dequeueNotification(MessageSendingStatus status) {
        List<Message> messages =
                notificationQueue.peek(notificationProperties.getNotificationDequeueBatchSize(), status);
        for (Message message : messages) {
            try {
                dispatcher.dispatch(message);
            } catch (Exception e) {
                messageRepository.updateStatusAndRetryTimesAndErrorMessageById(message.getId(),
                        MessageSendingStatus.SENT_FAILED, ExceptionUtils.getRootCauseMessage(e));
                sendingHistoryRepository.save(new MessageSendingHistoryEntity(message.getId(),
                        MessageSendingStatus.SENT_FAILED, ExceptionUtils.getRootCauseMessage(e)));
                log.warn("Send notification failed.", e);
            }
        }
    }
}
