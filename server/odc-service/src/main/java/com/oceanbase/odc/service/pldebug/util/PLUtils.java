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
package com.oceanbase.odc.service.pldebug.util;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.pldebug.model.PLDebugODPSpecifiedRoute;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @author yaobin
 * @date 2023-02-18
 * @since 4.2.0
 */
public abstract class PLUtils {

    public static boolean isSys(ConnectionSession connectionSession) {

        return isSys((ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession));
    }

    public static boolean isSys(ConnectionConfig connectionConfig) {

        return "SYS".equalsIgnoreCase(connectionConfig.getUsername());
    }

    public static String getSpecifiedRoute(PLDebugODPSpecifiedRoute pLDebugODPSpecifiedRoute) {
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        if (pLDebugODPSpecifiedRoute != null && pLDebugODPSpecifiedRoute.getObserverHost() != null
                && pLDebugODPSpecifiedRoute.getObserverPort() != null) {
            sqlBuilder.append("/* TARGET_DB_SERVER = '");
            sqlBuilder.append(pLDebugODPSpecifiedRoute.getObserverHost());
            sqlBuilder.append(":");
            sqlBuilder.append(pLDebugODPSpecifiedRoute.getObserverPort());
            sqlBuilder.append("' */");
        }
        return sqlBuilder.toString();
    }
}
