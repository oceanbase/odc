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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.NonNull;

/**
 * Local notification center for notification of events on the machine
 *
 * @author yh263208
 * @date 2022-02-11 20:29
 * @since ODC_release_3.3.0
 */
@Service
@SuppressWarnings("all")
public class LocalEventPublisher extends AbstractEventPublisher {

    private Map<String, AbstractEventListener<? extends AbstractEvent>> id2Listener = new HashMap<>();
    private Object retrievalMutex = new Object();

    @Override
    protected Collection<AbstractEventListener<? extends AbstractEvent>> getAllEventListeners() {
        synchronized (this.retrievalMutex) {
            return new LinkedList<>(id2Listener.values());
        }
    }

    @Override
    public <T extends AbstractEvent> void addEventListener(@NonNull AbstractEventListener<T> listener) {
        synchronized (this.retrievalMutex) {
            this.id2Listener.putIfAbsent(listener.getListenerId(), listener);
        }
    }

    @Override
    public <T extends AbstractEvent> boolean removeEventListener(@NonNull AbstractEventListener<T> listener) {
        synchronized (this.retrievalMutex) {
            return removeEventListener(listener.getListenerId()) != null;
        }
    }

    @Override
    public <T extends AbstractEvent> AbstractEventListener<T> removeEventListener(@NonNull String listenerId) {
        synchronized (this.retrievalMutex) {
            if (!id2Listener.containsKey(listenerId)) {
                return null;
            }
            return (AbstractEventListener<T>) id2Listener.remove(listenerId);
        }
    }

    @Override
    public void removeAllListeners() {
        synchronized (this.retrievalMutex) {
            id2Listener = new HashMap<>();
        }
    }

}
