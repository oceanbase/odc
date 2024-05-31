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
package com.oceanbase.odc.plugin.schema.mysql.utils;

import java.sql.Connection;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.connect.mysql.MySQLInformationExtension;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorGenerator;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessorGenerator;

/**
 * @author jingtian
 * @date 2024/2/26
 * @since ODC_release_4.2.4
 */
public class DBAccessorUtil {
    public static String getDbVersion(Connection connection) {
        MySQLInformationExtension informationExtension = new MySQLInformationExtension();
        return informationExtension.getDBVersion(connection);
    }

    public static DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBSchemaAccessorGenerator.createForMySQL(JdbcOperationsUtil.getJdbcOperations(connection),
                getDbVersion(connection));
    }

    public static DBStatsAccessor getStatsAccessor(Connection connection) {
        return DBStatsAccessorGenerator.createForMySQL(JdbcOperationsUtil.getJdbcOperations(connection),
                getDbVersion(connection));
    }
}
