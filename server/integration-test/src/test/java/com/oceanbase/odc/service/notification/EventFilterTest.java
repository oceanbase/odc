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
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
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
import com.oceanbase.odc.metadb.notification.EventEntity;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.metadb.notification.NotificationPolicyRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.helper.EventUtils;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;

public class EventFilterTest extends ServiceTestEnv {

    public static final Long ORGANIZATION_ID = 1L;
    public static final Long USER_ID = 1L;

    @Autowired
    private EventFilter filter;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper mapper;

    @MockBean
    private NotificationPolicyRepository policyRepository;

    @Before
    public void setUp() throws Exception {
        eventRepository.deleteAll();
    }

    @After
    public void tearDown() throws Exception {
        eventRepository.deleteAll();
    }

    @Test
    public void testFilter_Success() {
        List<EventEntity> events = new ArrayList<>();
        int eventCount = 100;
        for (int i = 0; i < eventCount; i++) {
            events.add((mapper.toEntity(getEvent())));
        }
        List<EventEntity> entities = eventRepository.saveAll(events);
        doReturn(Collections.singletonList(getNotificationPolicy())).when(policyRepository)
                .findByOrganizationIds(any());
        List<Event> filtered =
                filter.filter(entities.stream().map(entity -> mapper.fromEntity(entity)).collect(Collectors.toList()));
        Assert.assertEquals(eventCount, filtered.size());
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

    private NotificationPolicyEntity getNotificationPolicy() {
        NotificationPolicyEntity policy = new NotificationPolicyEntity();
        policy.setMatchExpression(JsonUtils.toJson(getLabels()));
        policy.setOrganizationId(ORGANIZATION_ID);
        return policy;
    }

    private EventLabels getLabels() {
        return EventUtils.buildEventLabels(TaskType.ASYNC, "failed", 1L);
    }
}
