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

import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessorFactory;

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
        String dbVersion = ConnectionSessionUtil.getVersion(connectionSession);
        PreConditions.notNull(dbVersion, "obVersion");
        PreConditions.notNull(consoleConnectionId, "consoleConnectionId");

        Map<String, Object> properties = new HashMap<>();
        properties.put(DBStatsAccessorFactory.CONNECTION_ID_KEY, consoleConnectionId);
        return DBBrowser.statsAccessor()
                .setProperties(properties)
                .setJdbcOperations(syncJdbcExecutor)
                .setDbVersion(dbVersion)
                .setType(connectType.getDialectType().getDBBrowserDialectTypeName())
                .create();
    }

}
