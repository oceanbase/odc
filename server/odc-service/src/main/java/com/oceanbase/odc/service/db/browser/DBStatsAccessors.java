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

import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.MySQLNoGreaterThan5740StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLNoLessThan400StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.OBMySQLStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.ODPOBMySQLStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleLessThan2270StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleNoLessThan2270StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OBOracleNoLessThan400StatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.oracle.OracleStatsAccessor;

/**
 * {@link DBStatsAccessors}
 *
 * @author yh263208
 * @date 2022-11-09 15:39
 * @since ODC-release_4.1.0
 */
public class DBStatsAccessors {

    public static DBStatsAccessor create(ConnectionSession connectionSession) {
        PreConditions.notNull(connectionSession, "connectionSession");

        ConnectType connectType = connectionSession.getConnectType();
        SyncJdbcExecutor syncJdbcExecutor =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.CONSOLE_DS_KEY);
        String consoleConnectionId = ConnectionSessionUtil.getConsoleConnectionId(connectionSession);
        PreConditions.notNull(connectType, "connectType");
        PreConditions.notNull(syncJdbcExecutor, "syncJdbcExecutor");
        String obVersion = ConnectionSessionUtil.getVersion(connectionSession);
        PreConditions.notNull(obVersion, "obVersion");
        PreConditions.notNull(consoleConnectionId, "consoleConnectionId");

        if (connectType == ConnectType.OB_MYSQL || connectType == ConnectType.CLOUD_OB_MYSQL) {
            if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "4.0.0")) {
                // OB 版本 >= 4.0.0
                return new OBMySQLNoLessThan400StatsAccessor(syncJdbcExecutor);
            } else {
                return new OBMySQLStatsAccessor(syncJdbcExecutor);
            }
        } else if (connectType == ConnectType.OB_ORACLE || connectType == ConnectType.CLOUD_OB_ORACLE) {
            if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "4.0.0")) {
                // OB 版本 >= 4.0.0
                return new OBOracleNoLessThan400StatsAccessor(syncJdbcExecutor);
            } else if (VersionUtils.isGreaterThanOrEqualsTo(obVersion, "2.2.70")) {
                // OB 版本为 [2.2.70, 4.0.0)
                return new OBOracleNoLessThan2270StatsAccessor(syncJdbcExecutor);
            } else {
                // OB 版本 < 2.2.70
                return new OBOracleLessThan2270StatsAccessor(syncJdbcExecutor);
            }
        } else if (connectType == ConnectType.MYSQL) {
            return new MySQLNoGreaterThan5740StatsAccessor(syncJdbcExecutor);
        } else if (connectType == ConnectType.ODP_SHARDING_OB_MYSQL) {
            return new ODPOBMySQLStatsAccessor(consoleConnectionId);
        } else if (connectType == ConnectType.ORACLE) {
            return new OracleStatsAccessor(syncJdbcExecutor);
        } else {
            throw new UnsupportedException(String.format("ConnectType '%s' not supported", connectType));
        }
    }

}
