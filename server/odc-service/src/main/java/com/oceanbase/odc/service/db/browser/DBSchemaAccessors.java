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
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoGreaterThan5740SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween220And225XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2260And2276SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLBetween2277And3XSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLNoGreaterThan1479SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.OBMySQLSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.ODPOBMySQLSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleLessThan2270SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleLessThan400SchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OBOracleSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.oracle.OracleSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.ALLDataDictTableNames;

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
        String obVersion = ConnectionSessionUtil.getVersion(connectionSession);
        PreConditions.notNull(obVersion, "obVersion");

        SyncJdbcExecutor sysSyncJdbcExecutor = null;
        String tenantName = null;
        if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "1.4.79")) {
            try {
                sysSyncJdbcExecutor =
                        connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.SYS_DS_KEY);
                tenantName = ConnectionSessionUtil.getTenantName(connectionSession);
            } catch (Exception e) {
                log.warn("Get SYS-DATASOURCE failed, may lack of sys tenant permission，message={}", e.getMessage());
            }
        }

        return create(syncJdbcExecutor, sysSyncJdbcExecutor, connectType, obVersion, tenantName);
    }

    public static DBSchemaAccessor create(@NonNull JdbcOperations syncJdbcExecutor, JdbcOperations sysJdbcExecutor,
            @NonNull ConnectType connectType, @NonNull String obVersion, String tenantName) {
        if (connectType == ConnectType.OB_MYSQL || connectType == ConnectType.CLOUD_OB_MYSQL) {
            if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "4.0.0")) {
                // OB 版本 >= 4.0.0
                return new OBMySQLSchemaAccessor(syncJdbcExecutor);
            } else if (VersionUtils.isGreaterThan(obVersion, "2.2.76")) {
                // OB 版本为 [2.2.77, 4.0.0)
                return new OBMySQLBetween2277And3XSchemaAccessor(syncJdbcExecutor);
            } else if (VersionUtils.isGreaterThan(obVersion, "2.2.60")) {
                // OB 版本为 [2.2.60, 2.2.77)
                return new OBMySQLBetween2260And2276SchemaAccessor(syncJdbcExecutor);
            } else if (VersionUtils.isGreaterThan(obVersion, "1.4.79")) {
                // OB 版本为 (1.4.79, 2.2.60)
                return new OBMySQLBetween220And225XSchemaAccessor(syncJdbcExecutor);
            } else {
                // OB 版本 <= 1.4.79
                return new OBMySQLNoGreaterThan1479SchemaAccessor(syncJdbcExecutor, sysJdbcExecutor, tenantName);
            }
        } else if (connectType == ConnectType.OB_ORACLE || connectType == ConnectType.CLOUD_OB_ORACLE) {
            if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "4.0.0")) {
                // OB 版本 >= 4.0.0
                return new OBOracleSchemaAccessor(syncJdbcExecutor, new ALLDataDictTableNames());
            } else if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "2.2.7")) {
                // OB 版本为 [2.2.7, 4.0.0)
                return new OBOracleLessThan400SchemaAccessor(syncJdbcExecutor, new ALLDataDictTableNames());
            } else {
                // OB 版本 < 2.2.7
                return new OBOracleLessThan2270SchemaAccessor(syncJdbcExecutor, new ALLDataDictTableNames());
            }
        } else if (connectType == ConnectType.ODP_SHARDING_OB_MYSQL) {
            return new ODPOBMySQLSchemaAccessor(syncJdbcExecutor);
        } else if (connectType == ConnectType.MYSQL) {
            return new MySQLNoGreaterThan5740SchemaAccessor(syncJdbcExecutor);
        } else if (connectType == ConnectType.ORACLE) {
            return new OracleSchemaAccessor(syncJdbcExecutor, new ALLDataDictTableNames());
        } else {
            throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }

}

