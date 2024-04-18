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
package com.oceanbase.odc.plugin.schema.oboracle.utils;

import java.sql.Connection;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.connect.oboracle.OBOracleInformationExtension;
import com.oceanbase.odc.plugin.schema.oboracle.browser.DBStatsAccessors;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorGenerator;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

/**
 * @author jingtian
 * @date 2023/6/30
 */
public class DBAccessorUtil {
    public static String getDbVersion(Connection connection) {
        OBOracleInformationExtension informationExtension = new OBOracleInformationExtension();
        return informationExtension.getDBVersion(connection);
    }

    public static DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBSchemaAccessorGenerator.createForOBOracle(JdbcOperationsUtil.getJdbcOperations(connection),
                getDbVersion(connection));
    }

    public static DBStatsAccessor getStatsAccessor(Connection connection) {
        return DBStatsAccessors.create(JdbcOperationsUtil.getJdbcOperations(connection), getDbVersion(connection));
    }

}
