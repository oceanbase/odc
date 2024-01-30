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
package com.oceanbase.odc.plugin.connect.oboracle;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.pf4j.Extension;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource.CloseIgnoreInvocationHandler;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-03-23
 * @since 4.2.0
 */
@Extension
@Slf4j
public class OBOracleSessionExtension implements SessionExtensionPoint {

    @Override
    public void killQuery(Connection connection, String connectionId) {
        JdbcOperationsUtil.getJdbcOperations(connection).execute("KILL QUERY " + connectionId);
    }

    @Override
    public void switchSchema(Connection connection, String schemaName) throws SQLException {
        String currentSchema = getCurrentSchema(connection);
        if (Objects.equals(currentSchema, schemaName)) {
            return;
        }
        String alterSql = "ALTER SESSION SET CURRENT_SCHEMA=" + StringUtils.quoteOracleIdentifier(schemaName);
        JdbcOperationsUtil.getJdbcOperations(connection).update(alterSql);
    }

    @Override
    public String getCurrentSchema(Connection connection) {
        String querySql = "SELECT SYS_CONTEXT('userenv', 'CURRENT_SCHEMA') FROM DUAL";
        return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
    }

    @Override
    public String getConnectionId(Connection connection) {
        String querySql = "select userenv('sessionid') from dual";
        String connectionId = null;
        try {
            return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
        } catch (Exception e) {
            if (connection instanceof OceanBaseConnection) {
                connectionId = ((OceanBaseConnection) connection).getServerThreadId() + "";
            } else if (Proxy.isProxyClass(connection.getClass())) {
                Connection actual = ((CloseIgnoreInvocationHandler) Proxy.getInvocationHandler(connection)).getTarget();
                connectionId = ((OceanBaseConnection) actual).getServerThreadId() + "";
            }
        }
        return connectionId;
    }
}
