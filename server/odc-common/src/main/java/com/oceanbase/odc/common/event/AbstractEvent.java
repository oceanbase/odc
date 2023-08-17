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

import java.io.Serializable;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;

/**
 * Basic events, used to encapsulate contextual information when an event occurs
 *
 * @author yh263208
 * @date 2022-02-11 17:32
 * @since ODC_release_3.3.0
 */
public abstract class AbstractEvent extends EventObject {
    private static final long serialVersionUID = 7099051208183571937L;
    private final long timestamp;
    private final String eventName;
    private final Map<String, Serializable> attributes = new HashMap<>();

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public AbstractEvent(Object source, @NonNull String eventName) {
        super(source);
        this.timestamp = System.currentTimeMillis();
        this.eventName = eventName;
    }

    public final long getTimestamp() {
        return this.timestamp;
    }

    public final String getEventName() {
        return this.eventName;
    }

    public Collection<String> getAttributeKeys() {
        return this.attributes.keySet();
    }

    public Serializable getAttribute(@NonNull String key) {
        return this.attributes.get(key);
    }

    public void setAttribute(@NonNull String key, Serializable value) {
        if (value == null) {
            this.removeAttribute(key);
        } else {
            this.attributes.put(key, value);
        }
    }

    public Serializable removeAttribute(@NonNull String key) {
        return this.attributes.remove(key);
    }

}
