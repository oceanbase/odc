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
package com.oceanbase.odc.core.sql.execute;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-04-25
 * @since 4.2.0
 */
@Slf4j
public class TestConnectionExtensionExecutor implements ConnectionExtensionExecutor {

    private final DialectType dialectType;

    public TestConnectionExtensionExecutor(DialectType dialectType) {
        this.dialectType = dialectType;
    }

    @Override
    public Function<Connection, String> getConnectionIdFunction() {
        return connection -> {
            try (Statement statement = connection.createStatement()) {
                return String.valueOf(statement.execute("select connection_id()"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    public BiConsumer<Connection, String> killQueryConsumer() {
        return (connection, connectionId) -> {
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("KILL QUERY " + connectionId);
                }
            } catch (Exception e) {
                log.warn("Failed to kill query, connectionId={}", connectionId, e);
            }
        };
    }

    @Override
    public DialectType getDialectType() {
        return this.dialectType;
    }
}
