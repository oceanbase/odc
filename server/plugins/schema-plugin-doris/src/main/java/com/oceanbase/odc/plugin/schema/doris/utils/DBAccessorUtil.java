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
package com.oceanbase.odc.plugin.schema.doris.utils;

import java.sql.Connection;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.plugin.connect.doris.DorisInformationExtension;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.doris.DorisSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.DorisStatsAccessor;

/**
 * @author gaoda.xy
 * @date 2024/3/20 16:42
 */
public class DBAccessorUtil {

    public static String getDbVersion(Connection connection) {
        DorisInformationExtension extension = new DorisInformationExtension();
        return extension.getDBVersion(connection);
    }

    public static DBSchemaAccessor getSchemaAccessor(Connection connection) {
        String dbVersion = getDbVersion(connection);
        JdbcOperations jdbcOperations = JdbcOperationsUtil.getJdbcOperations(connection);
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "5.7.0")) {
            return new DorisSchemaAccessor(jdbcOperations);
        } else {
            throw new UnsupportedException(String.format("Doris version '%s' not supported", dbVersion));
        }
    }

    public static DBStatsAccessor getStatsAccessor(Connection connection) {
        String dbVersion = getDbVersion(connection);
        JdbcOperations jdbcOperations = JdbcOperationsUtil.getJdbcOperations(connection);
        if (VersionUtils.isGreaterThanOrEqualsTo(dbVersion, "5.7.0")) {
            return new DorisStatsAccessor(jdbcOperations);
        } else {
            throw new UnsupportedException(String.format("Doris version '%s' not supported", dbVersion));
        }
    }

}
