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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.pf4j.Extension;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
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
        schemaName = isQuotedWithIdentifier(schemaName) ? schemaName : StringUtils.quoteOracleIdentifier(schemaName);
        String alterSql = "ALTER SESSION SET CURRENT_SCHEMA=" + schemaName;
        JdbcOperationsUtil.getJdbcOperations(connection).update(alterSql);
    }

    public boolean isQuotedWithIdentifier(String text) {
        if (text != null && text.length() >= 2) {
            return text.startsWith("\"") && text.endsWith("\"");
        }
        return false;
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
            }
        }
        return connectionId;
    }

    @Override
    public String getVariable(Connection connection, String variableName) {
        String querySql = "SHOW SESSION VARIABLES LIKE '" + variableName + "'";
        try {
            return JdbcOperationsUtil.getJdbcOperations(connection).query(querySql, rs -> {
                if (rs.next()) {
                    return rs.getString(2);
                }
                throw new UnexpectedException("variable does not exist: " + variableName);
            });
        } catch (Exception e) {
            log.warn("Failed to get variable {}, message={}", variableName, e.getMessage());
        }
        return null;
    }
}
