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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.metadb.notification.ChannelPropertyEntity;
import com.oceanbase.odc.metadb.notification.ChannelPropertyRepository;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.notification.model.BaseChannelConfig;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.DingTalkChannelConfig;
import com.oceanbase.odc.service.notification.model.QueryChannelParams;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/10
 */
public class NotificationServiceTest extends AuthorityTestEnv {
    private static final Long CHANNEL_ID = 1L;
    private static final String CHANNEL_NAME = "test_channel";
    private static final Long PROJECT_ID = 1L;

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ChannelRepository channelRepository;
    @Autowired
    private ChannelPropertyRepository channelPropertyRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(ADMIN_USER_ID);
    }

    @After
    public void tearDown() {
        channelPropertyRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    public void test_CreateChannel_withChannelConfig() {
        Channel saved = notificationService.createChannel(PROJECT_ID, getChannel());
        Assert.assertNotNull(saved);
        List<ChannelPropertyEntity> properties = channelPropertyRepository.findAllByChannelId(saved.getId());
        Assert.assertEquals(1, properties.size());
    }

    @Test
    public void test_DetailChannel_withChannelConfig() {
        Channel saved = notificationService.createChannel(PROJECT_ID, getChannel());
        Channel channel = notificationService.detailChannel(PROJECT_ID, saved.getId());
        Assert.assertEquals("test", ((DingTalkChannelConfig) channel.getChannelConfig()).getWebhook());
    }

    @Test
    public void tset_ListChannels() {
        Channel channel = getChannel();
        notificationService.createChannel(PROJECT_ID, channel);
        channel.setName("test1");
        notificationService.createChannel(PROJECT_ID, channel);

        Page<Channel> channels = notificationService.listChannels(PROJECT_ID, QueryChannelParams.builder().build(),
                Pageable.unpaged());
        Assert.assertEquals(2, channels.getSize());
    }

    @Test
    public void test_UpdateChannel_withConfig() {
        Channel channel = getChannel();
        channel.getChannelConfig().setLanguage("zh-CN");
        Channel saved = notificationService.createChannel(PROJECT_ID, channel);
        BaseChannelConfig config = new DingTalkChannelConfig();
        config.setLanguage("en");
        saved.setChannelConfig(config);
        Channel updated = notificationService.updateChannel(PROJECT_ID, saved);

        Channel updatedWithConfig = notificationService.detailChannel(PROJECT_ID, updated.getId());
        Assert.assertEquals("en", updatedWithConfig.getChannelConfig().getLanguage());
    }

    @Test
    public void test_DeleteChannel() {
        Channel saved = notificationService.createChannel(PROJECT_ID, getChannel());
        notificationService.deleteChannel(PROJECT_ID, saved.getId());

        Assert.assertFalse(channelRepository.findById(saved.getId()).isPresent());
        List<ChannelPropertyEntity> properties = channelPropertyRepository.findAllByChannelId(saved.getId());
        Assert.assertEquals(0, properties.size());
    }

    @Test
    public void test_ExistsChannel() {
        Channel saved = notificationService.createChannel(PROJECT_ID, getChannel());
        Assert.assertTrue(notificationService.existsChannel(PROJECT_ID, saved.getName()));
    }

    private Channel getChannel() {
        Channel channel = new Channel();
        channel.setProjectId(PROJECT_ID);
        channel.setName(CHANNEL_NAME);
        channel.setType(ChannelType.DingTalk);
        channel.setOrganizationId(ORGANIZATION_ID);
        channel.setCreatorId(ADMIN_USER_ID);
        DingTalkChannelConfig channelConfig = new DingTalkChannelConfig();
        channelConfig.setWebhook("test");
        channel.setChannelConfig(channelConfig);
        return channel;
    }

}
