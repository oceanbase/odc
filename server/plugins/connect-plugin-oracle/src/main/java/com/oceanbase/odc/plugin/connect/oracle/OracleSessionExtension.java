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

package com.oceanbase.odc.plugin.connect.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import org.pf4j.Extension;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.plugin.connect.model.DBClientInfo;
import com.oceanbase.odc.plugin.connect.oboracle.OBOracleSessionExtension;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2023/11/8
 * @since ODC_release_4.2.4
 */
@Slf4j
@Extension
public class OracleSessionExtension extends OBOracleSessionExtension {
    @Override
    public void killQuery(Connection connection, String connectionId) {
        JdbcOperationsUtil.getJdbcOperations(connection).execute("ALTER SYSTEM KILL SESSION '" + connectionId + "'");
    }

    @Override
    public String getConnectionId(Connection connection) {
        String querySql =
                "SELECT SID, SERIAL# FROM V$SESSION WHERE SID = SYS_CONTEXT('USERENV', 'SID') and AUDSID=SYS_CONTEXT('USERENV', 'SESSIONID')";
        String connectionId = null;
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(querySql);
            if (rs.next()) {
                connectionId = rs.getString("SID") + "," + rs.getString("SERIAL#");
            }
            PreConditions.notNull(connectionId, "SID and SERIAL# can not be null");
            return connectionId;
        } catch (Exception e) {
            log.info(
                    "Failed to get connection id from oracle, may not have permission to query V$SESSION, will use SYS_CONTEXT('USERENV', 'SESSIONID') as connection id, error message={}",
                    e.getMessage());
            querySql = "select SYS_CONTEXT('USERENV', 'SESSIONID') from dual";
            return JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
        }
    }

    @Override
    public String getVariable(@NonNull Connection connection, @NonNull String variableName) {
        String querySql = "SELECT VALUE FROM V$PARAMETER WHERE NAME = '" + variableName.toLowerCase() + "'";
        String value = null;
        try {
            value = JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
        } catch (Exception e) {
            log.warn("Failed to get variable {}, message={}", variableName, e.getMessage());
        }
        /**
         * nls parameters maybe null in V$PARAMETER, we need to query from V$NLS_PARAMETERS
         */
        if (Objects.isNull(value) && variableName.toLowerCase().startsWith("nls_")) {
            querySql = "SELECT VALUE FROM V$NLS_PARAMETERS WHERE PARAMETER = '" + variableName.toUpperCase() + "'";
            try {
                value = JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
            } catch (Exception e) {
                log.warn("Failed to get variable {}, message={}", variableName, e.getMessage());
                querySql = "SELECT VALUE FROM V_$NLS_PARAMETERS WHERE PARAMETER = '" + variableName.toUpperCase() + "'";
                value = JdbcOperationsUtil.getJdbcOperations(connection).queryForObject(querySql, String.class);
            }
        }
        return value;
    }

    @Override
    public String getAlterVariableStatement(String variableScope, String variableName, String variableValue) {
        if ("system".equals(variableScope) || "global".equals(variableScope)) {
            throw new UnsupportedOperationException("modifying the global or system variable of oracle is unsupported");
        }
        return String.format("alter %s set %s=%s", variableScope, variableName, variableValue);
    }

    @Override
    public boolean setClientInfo(Connection connection, DBClientInfo clientInfo) {
        String SET_MODULE_TEMPLATE =
                "BEGIN DBMS_APPLICATION_INFO.SET_MODULE(module_name => ?, action_name => ?); END;";
        try (PreparedStatement pstmt = connection.prepareStatement(SET_MODULE_TEMPLATE)) {
            pstmt.setString(1, clientInfo.getModule());
            pstmt.setString(2, clientInfo.getAction());
            pstmt.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


}
