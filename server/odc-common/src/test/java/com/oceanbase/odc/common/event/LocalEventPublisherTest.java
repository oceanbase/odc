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
package com.oceanbase.odc.common.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import lombok.NonNull;

/**
 * Test cases for {@link LocalEventPublisher}
 *
 * @author yh263208
 * @date 2022-02-11 20:36
 * @since ODC_release_3.3.0
 */
public class LocalEventPublisherTest {

    @Test
    public void testAddAndRemoveEventListener() {
        LocalEventPublisher notificationCenter = new LocalEventPublisher();
        TestEvent1Listener listener = new TestEvent1Listener(new ArrayList<>());
        notificationCenter.addEventListener(listener);

        Assert.assertTrue(notificationCenter.removeEventListener(listener));
    }

    @Test
    public void testAddAndRemoveEventListener_1() {
        LocalEventPublisher notificationCenter = new LocalEventPublisher();
        TestEvent1Listener listener = new TestEvent1Listener(new ArrayList<>());
        notificationCenter.addEventListener(listener);

        Assert.assertEquals(listener, notificationCenter.removeEventListener(listener.getListenerId()));
    }

    @Test
    public void testPublishEventForOneListener() {
        LocalEventPublisher notificationCenter = new LocalEventPublisher();

        List<String> events = new ArrayList<>();
        TestEvent1Listener listener = new TestEvent1Listener(events);
        notificationCenter.addEventListener(listener);

        TestEvent1 target = new TestEvent1(this);
        notificationCenter.publishEvent(target);
        Assert.assertEquals(target.getEventName(), events.get(0));
    }

    @Test
    public void testPublishEventForOneListener_1() {
        LocalEventPublisher notificationCenter = new LocalEventPublisher();

        List<String> events = new ArrayList<>();
        TestEvent1Listener listener = new TestEvent1Listener(events);
        notificationCenter.addEventListener(listener);

        TestEvent1 target = new TestEvent1(this);
        notificationCenter.publishEvent(target);
        Assert.assertEquals(target.getEventName(), events.get(0));

        events.removeIf(s -> true);
        TestEvent2 target1 = new TestEvent2(this);
        notificationCenter.publishEvent(target1);
        Assert.assertTrue(events.isEmpty());
    }

    @Test
    public void testPublishEventForTwoListeners() {
        LocalEventPublisher notificationCenter = new LocalEventPublisher();

        List<String> events = new ArrayList<>();
        TestEvent3Listener listener = new TestEvent3Listener(events);
        notificationCenter.addEventListener(listener);

        List<String> events1 = new ArrayList<>();
        TestEvent2Listener listener1 = new TestEvent2Listener(events1);
        notificationCenter.addEventListener(listener1);

        TestEvent1 target = new TestEvent1(this);
        notificationCenter.publishEvent(target);
        Assert.assertEquals(target.getEventName(), events.get(0));

        TestEvent2 target1 = new TestEvent2(this);
        notificationCenter.publishEvent(target1);
        Assert.assertEquals(target1.getEventName(), events1.get(0));
    }

}


class TestEvent1 extends AbstractEvent {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public TestEvent1(Object source) {
        super(source, "TestEvent1");
    }
}


class TestEvent2 extends AbstractEvent {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public TestEvent2(Object source) {
        super(source, "TestEvent2");
    }
}


class TestEvent1Listener extends AbstractEventListener<TestEvent1> {

    private final List<String> eventList;

    public TestEvent1Listener(@NonNull List<String> eventList) {
        this.eventList = eventList;
    }

    @Override
    public void onEvent(TestEvent1 event) {
        eventList.add(event.getEventName());
    }

    public void onBaseEvent(TestEvent2 event) {
        eventList.add(event.getEventName());
    }
}


class TestEvent3Listener extends TestEvent1Listener {

    public TestEvent3Listener(@NonNull List<String> eventList) {
        super(eventList);
    }

    @Override
    public void onEvent(TestEvent1 event) {
        super.onEvent(event);
    }
}


class TestEvent2Listener extends AbstractEventListener<TestEvent2> {

    private final List<String> eventList;

    public TestEvent2Listener(@NonNull List<String> eventList) {
        this.eventList = eventList;
    }

    @Override
    public void onEvent(TestEvent2 event) {
        eventList.add(event.getEventName());
    }

}
