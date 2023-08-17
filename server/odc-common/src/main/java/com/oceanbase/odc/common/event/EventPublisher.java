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

import lombok.NonNull;

/**
 * The notification center is used to register listeners for certain events. Push the event to the
 * specified listener by the notification center when the event occurs
 *
 * @author yh263208
 * @date 2022-02-11 16:56
 * @since ODC_release_3.3.0
 */
public interface EventPublisher {
    /**
     * Add an event listener
     *
     * @param listener listener instance
     */
    <T extends AbstractEvent> void addEventListener(@NonNull AbstractEventListener<T> listener);

    /**
     * Remove an listener
     *
     * @param listener listener instance
     */
    <T extends AbstractEvent> boolean removeEventListener(@NonNull AbstractEventListener<T> listener);

    /**
     * Remove an listener instance by id
     *
     * @param listenerId listener id
     */
    <T extends AbstractEvent> AbstractEventListener<T> removeEventListener(@NonNull String listenerId);

    /**
     * Remove an listener instance by id
     **/
    void removeAllListeners();

    /**
     * Publish an event
     *
     * @param event event instance
     */
    <T extends AbstractEvent> void publishEvent(@NonNull T event);

}
