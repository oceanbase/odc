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

import java.sql.Connection;

import com.oceanbase.odc.core.datasource.event.GetConnectionFailedEvent;

import lombok.Data;

@Data
public class DatasourceMonitorEventContext {
    private final long timestamp;
    private final Action action;
    private final Connection connection;

    public DatasourceMonitorEventContext(Action action, Connection connection, long timestamp) {
        this.timestamp = timestamp;
        this.action = action;
        this.connection = connection;
    }

    public enum Action {
        GET_CONNECTION_FAILED;

        public Action of(String eventName) {
            switch (eventName) {
                case GetConnectionFailedEvent.EVENT_NAME:
                    return Action.GET_CONNECTION_FAILED;
                default:
                    throw new IllegalArgumentException("Unknown action: " + eventName);
            }
        }
    }
}
