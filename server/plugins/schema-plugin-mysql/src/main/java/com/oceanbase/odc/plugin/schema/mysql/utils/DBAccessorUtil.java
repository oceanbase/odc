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
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.mysql.MySQLInformationExtension;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

/**
 * @author jingtian
 * @date 2024/2/26
 * @since ODC_release_4.2.4
 */
public class DBAccessorUtil {

    public static String getDbVersion(Connection connection) {
        return new MySQLInformationExtension().getDBVersion(connection);
    }

    public static DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBBrowser.schemaAccessor()
                .setDbVersion(getDbVersion(connection))
                .setJdbcOperations(JdbcOperationsUtil.getJdbcOperations(connection))
                .setType(DialectType.MYSQL.getDBBrowserDialectTypeName()).create();
    }

    public static DBStatsAccessor getStatsAccessor(Connection connection) {
        return DBBrowser.statsAccessor()
                .setJdbcOperations(JdbcOperationsUtil.getJdbcOperations(connection))
                .setDbVersion(getDbVersion(connection))
                .setType(DialectType.MYSQL.getDBBrowserDialectTypeName()).create();
    }

    public static DBTableEditor getTableEditor(Connection connection) {
        return DBBrowser.objectEditor().tableEditor()
                .setDbVersion(DBAccessorUtil.getDbVersion(connection))
                .setType(DialectType.MYSQL.getDBBrowserDialectTypeName()).create();
    }

}
