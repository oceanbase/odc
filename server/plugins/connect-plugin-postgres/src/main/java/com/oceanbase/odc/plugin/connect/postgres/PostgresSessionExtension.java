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
package com.oceanbase.odc.plugin.connect.postgres;

import java.sql.Connection;
import java.sql.SQLException;

import org.pf4j.Extension;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.ReflectionUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource.CloseIgnoreInvocationHandler;
import com.oceanbase.odc.plugin.connect.mysql.MySQLSessionExtension;

import lombok.NonNull;

@Extension
public class PostgresSessionExtension extends MySQLSessionExtension {

    @Override
    public String getConnectionId(@NonNull Connection connection) {
        String querySql = "SELECT pg_backend_pid();";
        String connectionId = null;
        try {
            return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
        } catch (Exception e) {
            OceanBaseConnection actual =
                    ReflectionUtils.getProxiedFieldValue(connection, CloseIgnoreInvocationHandler.class, "target");
            connectionId = actual == null ? "" : actual.getServerThreadId() + "";
        }
        return connectionId;
    }

    @Override
    public void killQuery(@NonNull Connection connection, @NonNull String connectionId) {

    }

    @Override
    public void switchSchema(Connection connection, String schemaName) throws SQLException {

    }

    @Override
    public String getCurrentSchema(Connection connection) {
        return null;
    }

    @Override
    public String getVariable(Connection connection, String variableName) {
        return null;
    }
}
