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
package com.oceanbase.odc.service.monitor;

import org.springframework.context.ApplicationEvent;

import com.oceanbase.odc.service.monitor.task.TaskMonitorEventHandler;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class MonitorEvent<T> extends ApplicationEvent {

    private final MonitorModule module;

    private final T context;

    /**
     * Constructs a prototypical Event.
     *
     * @param context The object on which the Event initially occurred.
     * @param module
     * @throws IllegalArgumentException if source is null.
     */
    public MonitorEvent(@NonNull MonitorModule module, T context) {
        super(context);
        this.module = module;
        this.context = context;
    }

    @Getter
    public enum MonitorModule {
        TASK_MODULE(TaskMonitorEventHandler.class);

        private final Class<? extends MonitorEventHandler<?>> handler;

        MonitorModule(Class<? extends MonitorEventHandler<?>> handler) {
            this.handler = handler;
        }

    }
}
