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
package com.oceanbase.odc.plugin.schema.obmysql;

import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import org.pf4j.Extension;

import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.parser.OBMySQLGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableStats;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/28
 * @since 4.2.0
 */
@Extension
public class OBMySQLTableExtension implements TableExtensionPoint {

    @Override
    public List<DBObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull DBObjectType tableType) {
        List<String> nameList;
        switch (tableType) {
            case TABLE:
                nameList = getSchemaAccessor(connection).showTables(schemaName);
                return generateDBObjectIdentityByTableType(schemaName, nameList, DBObjectType.TABLE);
            case EXTERNAL_TABLE:
                nameList = getSchemaAccessor(connection).showExternalTables(schemaName);
                return generateDBObjectIdentityByTableType(schemaName, nameList, DBObjectType.EXTERNAL_TABLE);
            default:
                throw new IllegalArgumentException("Unsupported table type: " + tableType);
        }
    }

    private List<DBObjectIdentity> generateDBObjectIdentityByTableType(String schemaName, List<String> nameList,
            DBObjectType tableType) {
        return nameList.stream().map(item -> {
            DBObjectIdentity identity = new DBObjectIdentity();
            identity.setType(tableType);
            identity.setSchemaName(schemaName);
            identity.setName(item);
            return identity;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> showNamesLike(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String tableNameLike) {
        return getSchemaAccessor(connection).showTablesLike(schemaName, tableNameLike);
    }

    @Override
    public DBTable getDetail(@NonNull Connection connection, @NonNull String schemaName, @NonNull String tableName) {
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        String ddl = schemaAccessor.getTableDDL(schemaName, tableName);
        OBMySQLGetDBTableByParser parser = new OBMySQLGetDBTableByParser(ddl);

        DBTable table = new DBTable();
        table.setSchemaName(schemaName);
        table.setOwner(schemaName);
        table.setName(tableName);
        table.setColumns(schemaAccessor.listTableColumns(schemaName, tableName));
        if (!schemaAccessor.isExternalTable(schemaName, tableName)) {
            table.setConstraints(schemaAccessor.listTableConstraints(schemaName, tableName));
            table.setIndexes(schemaAccessor.listTableIndexes(schemaName, tableName));
            table.setType(DBObjectType.TABLE);
        } else {
            table.setType(DBObjectType.EXTERNAL_TABLE);
        }
        table.setPartition(parser.getPartition());
        table.setDDL(ddl);
        table.setTableOptions(schemaAccessor.getTableOptions(schemaName, tableName));
        table.setStats(getTableStats(connection, schemaName, tableName));
        try {
            table.setColumnGroups(schemaAccessor.listTableColumnGroups(schemaName, tableName));
        } catch (Exception e) {
            // eat the exception
        }
        return table;
    }

    protected DBTableStats getTableStats(@NonNull Connection connection, @NonNull String schemaName,
            @NonNull String tableName) {
        DBStatsAccessor statsAccessor = getStatsAccessor(connection);
        DBTableStats tableStats = statsAccessor.getTableStats(schemaName, tableName);
        Long dataSizeInBytes = tableStats.getDataSizeInBytes();
        if (dataSizeInBytes == null || dataSizeInBytes < 0) {
            tableStats.setTableSize(null);
        } else {
            tableStats.setTableSize(BinarySizeUnit.B.of(dataSizeInBytes).toString());
        }
        return tableStats;
    }

    @Override
    public void drop(@NonNull Connection connection, @NonNull String schemaName, @NonNull String tableName) {
        getTableOperator(connection).drop(DBObjectType.TABLE, schemaName, tableName);
    }

    @Override
    public String generateCreateDDL(@NonNull Connection connection, @NonNull DBTable tableName) {
        return getTableEditor(connection).generateCreateObjectDDL(tableName);
    }

    @Override
    public String generateUpdateDDL(@NonNull Connection connection, @NonNull DBTable oldTable,
            @NonNull DBTable newTable) {
        return getTableEditor(connection).generateUpdateObjectDDL(oldTable, newTable);
    }

    @Override
    public boolean syncExternalTableFiles(Connection connection, String schemaName, String tableName) {
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        schemaAccessor.syncExternalTableFiles(schemaName, tableName);
        return true;
    }

    protected DBTableEditor getTableEditor(Connection connection) {
        return DBAccessorUtil.getTableEditor(connection);
    }

    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    protected DBStatsAccessor getStatsAccessor(Connection connection) {
        return DBAccessorUtil.getStatsAccessor(connection);
    }

    private DBObjectOperator getTableOperator(Connection connection) {
        return new MySQLObjectOperator(JdbcOperationsUtil.getJdbcOperations(connection));
    }

}
