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
package com.oceanbase.odc.plugin.schema.obmysql.utils;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLInformationExtension;
import com.oceanbase.tools.dbbrowser.DBBrowser;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessorFactory;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

/**
 * @author jingtian
 * @date 2023/6/30
 */
public class DBAccessorUtil {

    public static String getDbVersion(Connection connection) {
        return new OBMySQLInformationExtension().getDBVersion(connection);
    }

    public static DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBBrowser.schemaAccessor()
                .setJdbcOperations(JdbcOperationsUtil.getJdbcOperations(connection))
                .setDbVersion(getDbVersion(connection))
                .setType(DialectType.OB_MYSQL.name()).create();
    }

    public static DBSchemaAccessor getSchemaAccessor(Connection connection, String tenantName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(DBSchemaAccessorFactory.TENANT_NAME_KEY, tenantName);
        return DBBrowser.schemaAccessor()
                .setJdbcOperations(JdbcOperationsUtil.getJdbcOperations(connection))
                .setDbVersion(getDbVersion(connection))
                .setProperties(properties)
                .setType(DialectType.OB_MYSQL.name()).create();
    }

    public static DBStatsAccessor getStatsAccessor(Connection connection) {
        return DBBrowser.statsAccessor()
                .setDbVersion(getDbVersion(connection))
                .setJdbcOperations(JdbcOperationsUtil.getJdbcOperations(connection))
                .setType(DialectType.OB_MYSQL.name()).create();
    }

    public static DBTableEditor getTableEditor(Connection connection) {
        return DBBrowser.objectEditor().tableEditor()
                .setDbVersion(getDbVersion(connection))
                .setType(DialectType.OB_MYSQL.name()).create();
    }

}
