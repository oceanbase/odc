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
package com.oceanbase.odc.service.db.browser;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DBSchemaAccessors {

    public static DBSchemaAccessor create(ConnectionSession connectionSession) {
        return create(connectionSession, ConnectionSessionConstants.BACKEND_DS_KEY);
    }

    public static DBSchemaAccessor create(ConnectionSession connectionSession, String dataSourceName) {
        PreConditions.notNull(connectionSession, "connectionSession");

        ConnectType connectType = connectionSession.getConnectType();
        SyncJdbcExecutor syncJdbcExecutor =
                connectionSession.getSyncJdbcExecutor(dataSourceName);
        PreConditions.notNull(connectType, "connectType");
        PreConditions.notNull(syncJdbcExecutor, "syncJdbcExecutor");
        String dbVersion = ConnectionSessionUtil.getVersion(connectionSession);
        PreConditions.notNull(dbVersion, "obVersion");

        SyncJdbcExecutor sysSyncJdbcExecutor = null;
        String tenantName = null;
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "1.4.79")) {
            try {
                sysSyncJdbcExecutor =
                        connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.SYS_DS_KEY);
                tenantName = ConnectionSessionUtil.getTenantName(connectionSession);
            } catch (Exception e) {
                log.warn("Get SYS-DATASOURCE failed, may lack of sys tenant permission，message={}", e.getMessage());
            }
        }

        return create(syncJdbcExecutor, sysSyncJdbcExecutor, connectType, dbVersion, tenantName);
    }

    public static DBSchemaAccessor create(@NonNull JdbcOperations syncJdbcExecutor, JdbcOperations sysJdbcExecutor,
            @NonNull ConnectType connectType, @NonNull String dbVersion, String tenantName) {
        if (connectType == ConnectType.OB_MYSQL || connectType == ConnectType.CLOUD_OB_MYSQL) {
            return com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessors.createForOBMySQL(syncJdbcExecutor,
                    sysJdbcExecutor, dbVersion, tenantName);
        } else if (connectType == ConnectType.OB_ORACLE || connectType == ConnectType.CLOUD_OB_ORACLE) {
            return com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessors.createForOBOracle(syncJdbcExecutor,
                    dbVersion);
        } else if (connectType == ConnectType.ODP_SHARDING_OB_MYSQL) {
            return com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessors.createForODPOBMySQL(syncJdbcExecutor);
        } else if (connectType == ConnectType.MYSQL) {
            return com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessors.createForMySQL(syncJdbcExecutor, dbVersion);
        } else if (connectType == ConnectType.DORIS) {
            return com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessors.createForDoris(syncJdbcExecutor, dbVersion);
        } else if (connectType == ConnectType.ORACLE) {
            return com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessors.createForOracle(syncJdbcExecutor);
        } else {
            throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }

}

