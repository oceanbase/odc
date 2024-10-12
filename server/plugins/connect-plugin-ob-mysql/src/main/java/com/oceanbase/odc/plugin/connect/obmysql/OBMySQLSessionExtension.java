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
package com.oceanbase.odc.plugin.connect.obmysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.pf4j.Extension;

import com.oceanbase.jdbc.OceanBaseConnection;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.ReflectionUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource.CloseIgnoreInvocationHandler;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.plugin.connect.model.DBClientInfo;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-03-23
 * @since 4.2.0
 */
@Extension
@Slf4j
public class OBMySQLSessionExtension implements SessionExtensionPoint {

    private final OBMySQLInformationExtension obMySQLInformationExtension = new OBMySQLInformationExtension();

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
        connection.setCatalog(schemaName);
    }

    @Override
    public String getCurrentSchema(Connection connection) {
        String querySql = "SELECT DATABASE()";
        return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
    }

    @Override
    public String getConnectionId(Connection connection) {
        String querySql = "select connection_id()";
        String connectionId = null;
        try {
            return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
        } catch (Exception e) {
            if (connection instanceof OceanBaseConnection) {
                connectionId = ((OceanBaseConnection) connection).getServerThreadId() + "";
            } else {
                OceanBaseConnection actual =
                        ReflectionUtils.getProxiedFieldValue(connection, CloseIgnoreInvocationHandler.class, "target");
                connectionId = actual == null ? "" : actual.getServerThreadId() + "";
            }
        }
        return connectionId;
    }

    @Override
    public String getVariable(Connection connection, String variableName) {
        String querySql = "show session variables like '" + variableName + "'";
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

    @Override
    public String getAlterVariableStatement(String variableScope, String variableName, String variableValue) {
        return String.format("set %s %s=%s", variableScope, variableName, variableValue);
    }

    @Override
    public boolean setClientInfo(Connection connection, DBClientInfo clientInfo) {
        String dbVersion = obMySQLInformationExtension.getDBVersion(connection);
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return false;
        }
        String SET_CLIENT_INFO_SQL =
                "call dbms_application_info.set_module(module_name => ?, action_name => ? );call dbms_application_info.set_client_info(?); ";
        try (PreparedStatement pstmt = connection.prepareStatement(SET_CLIENT_INFO_SQL)) {
            pstmt.setString(1, clientInfo.getModule());
            pstmt.setString(2, clientInfo.getAction());
            pstmt.setString(3, clientInfo.getContext());
            pstmt.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
