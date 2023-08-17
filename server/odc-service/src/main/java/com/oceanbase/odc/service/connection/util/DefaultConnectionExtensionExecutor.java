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
package com.oceanbase.odc.service.connection.util;

import java.sql.Connection;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.ConnectionExtensionExecutor;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-04-25
 * @since 4.2.0
 */
public class DefaultConnectionExtensionExecutor implements ConnectionExtensionExecutor {

    private final DialectType dialectType;
    private final SessionExtensionPoint sessionExtension;

    public DefaultConnectionExtensionExecutor(@NonNull DialectType dialectType) {
        this.dialectType = dialectType;
        this.sessionExtension = ConnectionPluginUtil.getSessionExtension(dialectType);
    }

    @Override
    public Function<Connection, String> getConnectionIdFunction() {
        return sessionExtension::getConnectionId;
    }

    @Override
    public BiConsumer<Connection, String> killQueryConsumer() {
        return sessionExtension::killQuery;
    }

    @Override
    public DialectType getDialectType() {
        return this.dialectType;
    }
}
