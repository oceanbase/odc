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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.ListUtils;
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
import com.oceanbase.odc.metadb.notification.NotificationPolicyChannelRelationRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.ChannelMapper;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.Notification;

public class JdbcNotificationQueueTest extends ServiceTestEnv {
    public static final Long ORGANIZATION_ID = 1L;
    public static final Long USER_ID = 1L;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper mapper;

    @Autowired
    private JdbcNotificationQueue queue;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelMapper channelMapper;

    @MockBean
    private NotificationPolicyRepository policyRepository;

    @MockBean
    private NotificationPolicyChannelRelationRepository policyChannelRepository;

    @MockBean
    private ChannelRepository channelRepository;

    @Before
    public void setUp() throws Exception {
        eventRepository.deleteAll();
        messageRepository.deleteAll();
    }

    @After
    public void tearDown() {
        eventRepository.deleteAll();
        messageRepository.deleteAll();
    }


    @Test
    public void testOffer_NotificationIsEmpty_Success() {
        Assert.assertTrue(queue.offer(ListUtils.EMPTY_LIST));
    }

    @Test
    public void testOffer_NotificationNotEmpty_Success() {
        Assert.assertTrue(queue.offer(Arrays.asList(getNotification())));
        List<MessageEntity> messages = messageRepository.findAll();

        Assert.assertEquals(1, messages.size());
    }

    @Test
    public void testPeek_HaveChannel_MessageCreated() {
        messageRepository.save(getMessageEntity());

        when(channelRepository.findById(anyLong()))
                .thenReturn(Optional.of(getChannelEntity()));
        List<Notification> notifications = queue.peek(1, MessageSendingStatus.CREATED);

        Assert.assertEquals(MessageSendingStatus.CREATED, notifications.get(0).getMessage().getStatus());
    }

    @Test
    public void testPeek_NoChannel_MessageThrown() {
        messageRepository.save(getMessageEntity());

        when(channelRepository.findById(anyLong()))
                .thenReturn(Optional.empty());
        List<Notification> notifications = queue.peek(1, MessageSendingStatus.CREATED);

        Assert.assertEquals(0, notifications.size());
    }

    private Notification getNotification() {
        Notification notification = new Notification();
        notification.setMessage(getMessage());
        notification.setChannel(getChannel());
        return notification;
    }

    private Channel getChannel() {
        Channel channel = new Channel();
        channel.setType(ChannelType.DingTalk);
        channel.setName("testChannel");
        channel.setId(1L);
        channel.setProjectId(1L);
        return channel;
    }

    private ChannelEntity getChannelEntity() {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(1L);
        entity.setCreatorId(USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setProjectId(1L);
        entity.setType(ChannelType.DingTalk);
        entity.setName("test");
        return entity;
    }

    private Message getMessage() {
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
                .build();
    }

    private MessageEntity getMessageEntity() {
        Message message = getMessage();
        MessageEntity entity = message.toEntity();
        entity.setId(1L);
        return entity;
    }
}
