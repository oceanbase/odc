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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.notification.ChannelEntity;
import com.oceanbase.odc.metadb.notification.ChannelRepository;
import com.oceanbase.odc.metadb.notification.EventEntity;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.metadb.notification.NotificationChannelRelationEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyChannelRelationRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.helper.EventUtils;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;
import com.oceanbase.odc.service.notification.model.Notification;

public class ConverterTest extends ServiceTestEnv {
    public static final Long ORGANIZATION_ID = 1L;
    public static final Long USER_ID = 1L;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper mapper;

    @Autowired
    private Converter converter;

    @MockBean
    private NotificationPolicyRepository policyRepository;

    @MockBean
    private NotificationPolicyChannelRelationRepository policyChannelRepository;

    @MockBean
    private ChannelRepository channelRepository;



    @Before
    public void setUp() throws Exception {
        eventRepository.deleteAll();
    }

    @After
    public void tearDown() {
        eventRepository.deleteAll();
    }

    @Test
    public void testConvert_Success() {
        List<EventEntity> events = new ArrayList<>();
        int eventCount = 100;
        for (int i = 0; i < eventCount; i++) {
            events.add(mapper.toEntity(getEvent()));
        }
        eventRepository.saveAll(events);

        when(policyRepository.findByOrganizationIds(any()))
                .thenReturn(Collections.singletonList(getNotificationPolicy()));
        when(policyChannelRepository.findByNotificationPolicyIds(any()))
                .thenReturn(Collections.singletonList(getNotificationChannelRelationEntity()));
        when(channelRepository.findByIdIn(any()))
                .thenReturn(Arrays.asList(getChannelEntity()));

        List<Notification> notifications =
                converter.convert(events.stream().map(e -> mapper.fromEntity(e)).collect(Collectors.toList()));

        Assert.assertEquals(eventCount, notifications.size());
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

    private NotificationPolicyEntity getPolicyEntity() {
        NotificationPolicyEntity entity = new NotificationPolicyEntity();
        entity.setId(1L);
        entity.setTitleTemplate("test_title");
        entity.setContentTemplate("test_content");
        entity.setCreatorId(USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setMatchExpression("fake_expression");
        return entity;
    }

    private ChannelEntity getChannelEntity() {
        ChannelEntity entity = new ChannelEntity();
        entity.setId(1L);
        entity.setType(ChannelType.DingTalk);
        entity.setName("testChannel");
        entity.setCreatorId(USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        return entity;
    }

    private NotificationPolicyEntity getNotificationPolicy() {
        NotificationPolicyEntity policy = new NotificationPolicyEntity();
        policy.setId(1L);
        policy.setMatchExpression(JsonUtils.toJson(getLabels()));
        policy.setOrganizationId(ORGANIZATION_ID);
        return policy;
    }

    private NotificationChannelRelationEntity getNotificationChannelRelationEntity() {
        NotificationChannelRelationEntity entity = new NotificationChannelRelationEntity();
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setChannelId(1L);
        entity.setNotificationPolicyId(1L);
        return entity;
    }

}
