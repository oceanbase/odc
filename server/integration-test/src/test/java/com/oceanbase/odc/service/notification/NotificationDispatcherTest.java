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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.MessageEntity;
import com.oceanbase.odc.metadb.notification.MessageRepository;
import com.oceanbase.odc.service.notification.constant.ChannelPropertiesConstants;
import com.oceanbase.odc.service.notification.model.ChannelConfig;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.Notification;

public class NotificationDispatcherTest extends ServiceTestEnv {
    public static final Long ORGANIZATION_ID = 1L;
    public static final Long USER_ID = 1L;


    @Autowired
    private MessageRepository messageRepository;


    @Autowired
    private NotificationDispatcher dispatcher;



    @MockBean
    private ChannelRepository channelRepository;

    @Before
    public void setUp() throws Exception {
        messageRepository.deleteAll();
    }

    @After
    public void tearDown() {
        messageRepository.deleteAll();
    }


    @Test
    public void testDispatch_SendFailed() {
        MessageEntity entity = messageRepository.save(getMessage().toEntity());

        dispatcher.dispatch(getNotification(entity));

        Assert.assertEquals(MessageSendingStatus.SENT_FAILED, messageRepository.findAll().get(0).getStatus());
    }

    private Notification getNotification(MessageEntity message) {
        Notification notification = new Notification();
        notification.setMessage(Message.fromEntity(message));
        notification.setChannel(getChannel());
        return notification;
    }

    private ChannelConfig getChannel() {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setType(ChannelType.DingTalkGroupBot);
        channelConfig.setName("testChannel");
        channelConfig.setId(1L);
        Map<String, String> properties = new HashMap<>();
        properties.put(ChannelPropertiesConstants.SERVICE_URL, "[\"fake_url\"]");
        properties.put(ChannelPropertiesConstants.AT_ALL, "[false]");
        properties.put(ChannelPropertiesConstants.RECIPIENT_ATTRIBUTE_NAME, "[\"user_account_name\"]");
        channelConfig.setProperties(properties);
        return channelConfig;
    }

    private Message getMessage() {
        return Message.builder()
                .title("test title")
                .content("test content")
                .channelId(1L)
                .eventId(1L)
                .retryTimes(0)
                .maxRetryTimes(3)
                .status(MessageSendingStatus.CREATED)
                .toRecipients(Arrays.asList("1"))
                .ccRecipients(Arrays.asList("2"))
                .creatorId(USER_ID)
                .organizationId(ORGANIZATION_ID)
                .build();
    }
}
