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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.oceanbase.odc.metadb.notification.NotificationPolicyChannelRelationRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.notification.helper.PolicyMapper;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.DingTalkChannelConfig;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;
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
    @Autowired
    private NotificationPolicyChannelRelationRepository relationRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private NotificationPolicyRepository policyRepository;

    @Before
    public void setUp() {
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(ADMIN_USER_ID);
    }

    @After
    public void tearDown() {
        channelPropertyRepository.deleteAll();
        channelRepository.deleteAll();
        relationRepository.deleteAll();
        policyRepository.deleteAll();
    }

    @Test
    public void test_CreateChannel_withChannelConfig() {
        Channel saved = notificationService.createChannel(PROJECT_ID, getChannel());
        Assert.assertNotNull(saved);
        List<ChannelPropertyEntity> properties = channelPropertyRepository.findAllByChannelId(saved.getId());
        Assert.assertEquals(2, properties.size());
    }

    @Test
    public void test_DetailChannel_withChannelConfig() {
        Channel saved = notificationService.createChannel(PROJECT_ID, getChannel());
        Channel channel = notificationService.detailChannel(PROJECT_ID, saved.getId());
        Assert.assertEquals("https://oapi.dingtalk.com/robot",
                ((DingTalkChannelConfig) channel.getChannelConfig()).getWebhook());
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
        DingTalkChannelConfig config = new DingTalkChannelConfig();
        config.setLanguage("en");
        config.setWebhook("https://oapi.dingtalk.com/robot");
        saved.setChannelConfig(config);
        Channel updated = notificationService.updateChannel(PROJECT_ID, saved);

        Channel updatedWithConfig = notificationService.detailChannel(PROJECT_ID, updated.getId());
        Assert.assertEquals("en", updatedWithConfig.getChannelConfig().getLanguage());
    }

    @Test
    public void test_UpdateChannel_withSign() {
        Channel channel = getChannel();
        ((DingTalkChannelConfig) channel.getChannelConfig()).setSign("password");
        Channel saved = notificationService.createChannel(PROJECT_ID, channel);
        DingTalkChannelConfig config = new DingTalkChannelConfig();
        config.setWebhook("https://oapi.dingtalk.com/robot");
        saved.setChannelConfig(config);
        Channel updated = notificationService.updateChannel(PROJECT_ID, saved);

        Channel updatedWithConfig = notificationService.detailChannel(PROJECT_ID, updated.getId());
        Assert.assertEquals("password", ((DingTalkChannelConfig) updatedWithConfig.getChannelConfig()).getSign());
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

    @Test
    public void test_ListPolicies_NoActualData() {
        List<NotificationPolicy> policies = notificationService.listPolicies(PROJECT_ID);
        Assert.assertEquals("${com.oceanbase.odc.event.ALL_EVENTS.name}", policies.get(0).getEventName());
        Assert.assertTrue(policies.get(0).isEnabled());
    }

    @Test
    public void test_ListPolicies_WithActualData() {
        NotificationPolicyEntity policy = getPolicy();
        policy.setEnabled(false);
        policyRepository.save(policy);
        List<NotificationPolicy> policies = notificationService.listPolicies(PROJECT_ID);
        Assert.assertFalse(policies.get(0).isEnabled());
    }

    @Test
    public void test_BatchUpdatePolicies() {
        Channel channel = notificationService.createChannel(PROJECT_ID, getChannel());
        NotificationPolicy saved = PolicyMapper.fromEntity(policyRepository.save(getPolicy()));
        saved.setEnabled(false);
        NotificationPolicy metaPolicy = PolicyMapper.fromEntity(getPolicy());
        metaPolicy.setPolicyMetadataId(2L);
        metaPolicy.setChannels(Collections.singletonList(channel));
        List<NotificationPolicy> toBeUpdated = Arrays.asList(saved, metaPolicy);
        notificationService.batchUpdatePolicies(PROJECT_ID, toBeUpdated);

        Map<Long, List<NotificationPolicy>> policies = notificationService.listPolicies(PROJECT_ID)
                .stream().collect(Collectors.groupingBy(NotificationPolicy::getPolicyMetadataId));

        Assert.assertFalse(policies.get(1L).get(0).isEnabled());
        Assert.assertTrue(policies.containsKey(2L));
        Assert.assertEquals(policies.get(2L).get(0).getChannels().get(0).getId(), channel.getId());
    }

    private Channel getChannel() {
        Channel channel = new Channel();
        channel.setProjectId(PROJECT_ID);
        channel.setName(CHANNEL_NAME);
        channel.setType(ChannelType.DingTalk);
        channel.setOrganizationId(ORGANIZATION_ID);
        channel.setCreatorId(ADMIN_USER_ID);
        DingTalkChannelConfig channelConfig = new DingTalkChannelConfig();
        channelConfig.setWebhook("https://oapi.dingtalk.com/robot");
        channel.setChannelConfig(channelConfig);
        return channel;
    }

    private NotificationPolicyEntity getPolicy() {
        NotificationPolicyEntity policy = new NotificationPolicyEntity();
        policy.setCreatorId(ADMIN_USER_ID);
        policy.setOrganizationId(ORGANIZATION_ID);
        policy.setProjectId(PROJECT_ID);
        policy.setPolicyMetadataId(1L);
        policy.setMatchExpression("true");
        return policy;
    }

}
