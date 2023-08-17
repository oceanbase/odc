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
import java.util.Map;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;
import org.springframework.util.ConcurrentReferenceHashMap;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AbstractEventPublisher}
 *
 * @author yh263208
 * @date 2022-02-11 18:02
 * @since ODC_release_3.3.0
 */
@Slf4j
@SuppressWarnings("all")
public abstract class AbstractEventPublisher implements EventPublisher {

    private static final Map<Class<?>, ResolvableType> eventTypeCache = new ConcurrentReferenceHashMap<>();

    @Override
    public <T extends AbstractEvent> void publishEvent(@NonNull T event) {
        Collection<AbstractEventListener<? extends AbstractEvent>> listeners = getAllEventListeners();
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        listeners.stream().filter(listener -> {
            if (listener == null) {
                return false;
            }
            ResolvableType resolvableType = resolveDeclaredEventType(listener);
            return resolvableType.isAssignableFrom(event.getClass());
        }).map(listener -> (AbstractEventListener<T>) listener).forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception exception) {
                log.warn("Event response method call failed, event={}", event, exception);
            }
        });
    }

    private static ResolvableType resolveDeclaredEventType(AbstractEventListener<? extends AbstractEvent> listener) {
        ResolvableType declaredEventType = resolveDeclaredEventType(listener.getClass());
        if (declaredEventType == null || declaredEventType.isAssignableFrom(AbstractEvent.class)) {
            Class<?> targetClass = AopUtils.getTargetClass(listener);
            if (targetClass != listener.getClass()) {
                declaredEventType = resolveDeclaredEventType(targetClass);
            }
        }
        return declaredEventType;
    }

    static ResolvableType resolveDeclaredEventType(Class<?> listenerType) {
        ResolvableType eventType = eventTypeCache.get(listenerType);
        if (eventType == null) {
            eventType = ResolvableType.forClass(listenerType).as(AbstractEventListener.class).getGeneric();
            eventTypeCache.put(listenerType, eventType);
        }
        return (eventType != ResolvableType.NONE ? eventType : null);
    }

    /**
     * Get all Event Listeners
     *
     * @return listener list
     */
    protected abstract Collection<AbstractEventListener<? extends AbstractEvent>> getAllEventListeners();

}
