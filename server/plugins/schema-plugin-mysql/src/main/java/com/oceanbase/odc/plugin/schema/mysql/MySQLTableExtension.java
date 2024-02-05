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
package com.oceanbase.odc.plugin.schema.mysql;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pf4j.Extension;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLNoGreaterThan5740IndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.schema.mysql.MySQLNoGreaterThan5740SchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.stats.mysql.MySQLNoGreaterThan5740StatsAccessor;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class MySQLTableExtension extends OBMySQLTableExtension {
    @Override
    public DBTable getDetail(@NonNull Connection connection, @NonNull String schemaName, @NonNull String tableName) {
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        DBTable table = new DBTable();
        table.setSchemaName(schemaName);
        table.setOwner(schemaName);
        table.setName(schemaAccessor.isLowerCaseTableName() ? tableName.toLowerCase() : tableName);
        table.setColumns(schemaAccessor.listTableColumns(schemaName, tableName));
        table.setConstraints(schemaAccessor.listTableConstraints(schemaName, tableName));
        table.setPartition(schemaAccessor.getPartition(schemaName, tableName));
        table.setIndexes(schemaAccessor.listTableIndexes(schemaName, tableName));
        table.setDDL(schemaAccessor.getTableDDL(schemaName, tableName));
        table.setTableOptions(schemaAccessor.getTableOptions(schemaName, tableName));
        table.setStats(getTableStats(connection, schemaName, tableName));
        return table;
    }

    @Override
    public Map<String, DBTable> listDetails(Connection connection, String schemaName) {
        Map<String, DBTable> returnVal = new HashMap<>();
        DBSchemaAccessor accessor = getSchemaAccessor(connection);
        List<String> tableNames = accessor.showTables(schemaName);
        if (tableNames.isEmpty()) {
            return returnVal;
        }
        Map<String, List<DBTableColumn>> tableName2Columns = accessor.listTableColumns(schemaName);
        Map<String, List<DBTableIndex>> tableName2Indexes = accessor.listTableIndexes(schemaName);
        Map<String, List<DBTableConstraint>> tableName2Constraints = accessor.listTableConstraints(schemaName);
        Map<String, DBTableOptions> tableName2Options = accessor.listTableOptions(schemaName);
        for (String tableName : tableNames) {
            if (!tableName2Columns.containsKey(tableName)) {
                continue;
            }
            DBTable table = new DBTable();
            table.setSchemaName(schemaName);
            table.setOwner(schemaName);
            table.setName(tableName);
            table.setColumns(tableName2Columns.getOrDefault(tableName, Lists.newArrayList()));
            table.setIndexes(tableName2Indexes.getOrDefault(tableName, Lists.newArrayList()));
            table.setConstraints(tableName2Constraints.getOrDefault(tableName, Lists.newArrayList()));
            table.setTableOptions(tableName2Options.getOrDefault(tableName, new DBTableOptions()));
            table.setPartition(accessor.getPartition(schemaName, tableName));
            table.setDDL(accessor.getTableDDL(schemaName, tableName));
            returnVal.put(tableName, table);
        }
        return returnVal;
    }

    @Override
    protected DBTableEditor getTableEditor(@NonNull Connection connection) {
        return new MySQLTableEditor(new MySQLNoGreaterThan5740IndexEditor(), new MySQLColumnEditor(),
                new MySQLConstraintEditor(),
                new MySQLDBTablePartitionEditor());
    }

    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return new MySQLNoGreaterThan5740SchemaAccessor(JdbcOperationsUtil.getJdbcOperations(connection));
    }

    @Override
    protected DBStatsAccessor getStatsAccessor(Connection connection) {
        return new MySQLNoGreaterThan5740StatsAccessor(JdbcOperationsUtil.getJdbcOperations(connection));
    }

}
