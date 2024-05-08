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
import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.metadb.notification.MessageEntity;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;

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

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private JdbcNotificationQueue notificationQueue;

    @Autowired
    private ChannelMapper channelMapper;

    @Before
    public void setUp() throws Exception {
        eventRepository.deleteAll();
    }

    @After
    public void tearDown() throws Exception {
        eventRepository.deleteAll();
        messageRepository.deleteAll();
    }

    @Test
    public void testDequeueEvent_Success() {
        eventRepository.save(eventMapper.toEntity(getEvent()));

        when(eventQueue.peek(anyInt(), any(EventStatus.class))).thenReturn(Arrays.asList(getEvent()));
        when(eventFilter.filter(anyList())).thenReturn(Arrays.asList(getEvent()));
        when(converter.convert(anyList())).thenReturn(Arrays.asList(getMessage()));
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
        ChannelEntity channel = channelRepository.save(channelMapper.toEntity(getChannel()));
        MessageEntity entity = messageRepository.save(getMessage().toEntity());

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
        return event;
    }

    private Channel getChannel() {
        Channel channel = new Channel();
        channel.setType(ChannelType.DingTalk);
        channel.setName("testChannel");
        channel.setId(1L);
        channel.setCreatorId(USER_ID);
        channel.setOrganizationId(ORGANIZATION_ID);
        channel.setProjectId(1L);
        return channel;
    }

    private Message getMessage() {
        Event event = new Event();
        event.setId(1L);
        return Message.builder()
                .title("test title")
                .content("test content")
                .retryTimes(0)
                .maxRetryTimes(3)
                .status(MessageSendingStatus.CREATED)
                .creatorId(USER_ID)
                .organizationId(ORGANIZATION_ID)
                .projectId(1L)
                .channel(getChannel())
                .event(event)
                .build();
    }

}
