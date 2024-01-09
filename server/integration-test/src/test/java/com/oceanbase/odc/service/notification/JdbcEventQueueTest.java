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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.metadb.notification.EventEntity;
import com.oceanbase.odc.metadb.notification.EventRepository;
import com.oceanbase.odc.service.notification.helper.EventMapper;
import com.oceanbase.odc.service.notification.helper.EventUtils;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.notification.model.EventLabels;
import com.oceanbase.odc.service.notification.model.EventStatus;

public class JdbcEventQueueTest extends ServiceTestEnv {
    public static final Long ORGANIZATION_ID = 1L;
    public static final Long USER_ID = 1L;

    @Autowired
    private JdbcEventQueue queue;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMapper mapper;

    @Before
    public void setUp() throws Exception {
        eventRepository.deleteAll();
    }

    @After
    public void tearDown() {
        eventRepository.deleteAll();
    }

    @Test
    public void testOffer_Success() {
        queue.offer(getEvent());
        Assert.assertEquals(1, eventRepository.findAll().size());
    }

    @Test
    public void testPeek_Success() {
        queue.offer(getEvent());
        Event event = queue.peek(EventStatus.CREATED);
        Assert.assertEquals(EventStatus.CREATED, event.getStatus());
    }

    @Test
    public void testPeekN_Success() {
        queue.offer(getEvent());
        queue.offer(getEvent());
        List<Event> event = queue.peek(2, EventStatus.CREATED);
        Assert.assertEquals(2, event.size());
    }

    @Test
    public void testPeekN_MultiThread_Success() {
        List<EventEntity> events = new ArrayList<>();
        int eventCount = 100;
        for (int i = 0; i < eventCount; i++) {
            events.add(mapper.toEntity(getEvent()));
        }
        eventRepository.saveAll(events);

        List<Event> dequeued = new CopyOnWriteArrayList<>();
        int runnerCount = 10;
        int batchSize = 10;
        List<Thread> threads = new CopyOnWriteArrayList<>();
        for (int i = 0; i < runnerCount; i++) {
            Thread thread = new Thread(() -> {
                List<Event> event = queue.peek(10, EventStatus.CREATED);
                dequeued.addAll(event);
            });
            threads.add(thread);
            thread.start();
        }
        threads.stream().forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        Assert.assertTrue(dequeued.size() >= batchSize && dequeued.size() <= eventCount);
    }

    @Test
    public void testSize_Success() {
        queue.offer(getEvent());
        queue.offer(getEvent());
        Assert.assertEquals(2, queue.size());
    }


    private Event getEvent() {
        Event event = new Event();
        event.setStatus(EventStatus.CREATED);
        event.setOrganizationId(ORGANIZATION_ID);
        event.setTriggerTime(new Date());
        event.setCreatorId(USER_ID);
        event.setLabels(getLabels());
        event.setProjectId(1L);
        return event;
    }

    private EventLabels getLabels() {
        return EventUtils.buildEventLabels(TaskType.ASYNC, "failed", 1L);
    }
}
