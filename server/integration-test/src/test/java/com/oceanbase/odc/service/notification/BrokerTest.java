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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.metadb.notification.MessageEntity;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.helper.EventUtils;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.Notification;

public class BrokerTest extends ServiceTestEnv {
    public static final Long ORGANIZATION_ID = 1L;
    public static final Long USER_ID = 1L;

    @Autowired
    private Broker broker;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper eventMapper;

    @MockBean
    private EventQueue eventQueue;

    @MockBean
    private EventFilter eventFilter;

    @MockBean
    private Converter converter;

    @MockBean
    private ChannelRepository channelRepository;

    @MockBean
    private JdbcNotificationQueue notificationQueue;

    @Before
    public void setUp() throws Exception {
        eventRepository.deleteAll();
    }

    @After
    public void tearDown() throws Exception {
        eventRepository.deleteAll();
    }

    @Test
    public void testDequeueEvent_Success() {
        eventRepository.save(eventMapper.toEntity(getEvent()));

        when(eventQueue.peek(anyInt(), any(EventStatus.class))).thenReturn(Arrays.asList(getEvent()));
        when(eventFilter.filter(anyList())).thenReturn(Arrays.asList(getEvent()));
        when(converter.convert(anyList())).thenReturn(Arrays.asList(getNotification()));
        broker.dequeueEvent(EventStatus.CREATED);
        List<MessageEntity> messages = messageRepository.findAll();
        Assert.assertEquals(1, messages.size());
    }

    @Test
    public void testEnqueueEvent_Success() {
        eventRepository.save(eventMapper.toEntity(getEvent()));

        broker.enqueueEvent(getEvent());
        Assert.assertEquals(1, eventRepository.findAll().size());
    }

    @Test
    public void testDequeueNotification_Success() {
        MessageEntity entity = messageRepository.save(getMessage().toEntity());

        when(notificationQueue.peek(anyInt(), any())).thenReturn(Arrays.asList(getNotification(entity)));
        broker.dequeueNotification(MessageSendingStatus.CREATED);

        Assert.assertEquals(MessageSendingStatus.SENT_FAILED, messageRepository.findAll().get(0).getStatus());
    }

    private Event getEvent() {
        Event event = new Event();
        event.setStatus(EventStatus.CREATED);
        event.setOrganizationId(ORGANIZATION_ID);
        event.setTriggerTime(new Date());
        event.setCreatorId(USER_ID);
        event.setProjectId(1L);
        event.setLabels(getLabels());
        return event;
    }

    private EventLabels getLabels() {
        return EventUtils.buildEventLabels(TaskType.ASYNC, "failed", 1L);
    }

    private Notification getNotification(MessageEntity message) {
        Notification notification = new Notification();
        notification.setMessage(Message.fromEntity(message));
        notification.setChannel(getChannel());
        return notification;
    }

    private Channel getChannel() {
        Channel channel = new Channel();
        channel.setType(ChannelType.DingTalk);
        channel.setName("testChannel");
        channel.setId(1L);
        return channel;
    }

    private Message getMessage() {
        Channel channel = new Channel();
        channel.setId(1L);
        return Message.builder()
                .title("test title")
                .content("test content")
                .retryTimes(0)
                .maxRetryTimes(3)
                .status(MessageSendingStatus.CREATED)
                .creatorId(USER_ID)
                .organizationId(ORGANIZATION_ID)
                .projectId(1L)
                .channel(channel)
                .build();
    }

    private Notification getNotification() {
        Notification notification = new Notification();
        notification.setMessage(getMessage());
        notification.setChannel(getChannel());
        return notification;
    }
}
