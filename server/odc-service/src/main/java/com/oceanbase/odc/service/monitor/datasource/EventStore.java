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
package com.oceanbase.odc.service.monitor.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.oceanbase.odc.core.datasource.event.GetConnectionFailedEvent;

final class EventStore {

    private final BlockingQueue<GetConnectionFailedEvent> eventQueue;

    public EventStore(int size) {
        eventQueue = new LinkedBlockingQueue<>(size);
    }

    public boolean offer(GetConnectionFailedEvent event) {
        return eventQueue.offer(event);
    }

    public List<GetConnectionFailedEvent> eventDrainTo(int size) {
        List<GetConnectionFailedEvent> events = new ArrayList<>();
        eventQueue.drainTo(events, size);
        return events;
    }
}
