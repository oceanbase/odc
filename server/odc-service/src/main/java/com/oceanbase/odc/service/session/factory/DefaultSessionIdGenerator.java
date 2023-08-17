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
package com.oceanbase.odc.service.session.factory;

import java.util.concurrent.atomic.AtomicLong;

import com.oceanbase.odc.core.session.ConnectionSessionIdGenerator;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

public class DefaultSessionIdGenerator implements ConnectionSessionIdGenerator {

    private static final AtomicLong SESSION_ID_COUNTER = new AtomicLong(10000);
    private final ConnectionConfig connectionConfig;

    public DefaultSessionIdGenerator(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    @Override
    public String generateId() {
        Verify.notNull(connectionConfig.getId(), "ConnectionId");
        return connectionConfig.getId() + "-" + SESSION_ID_COUNTER.incrementAndGet();
    }

}
