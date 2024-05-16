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
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.plugin.schema.api.TableExtensionPoint;
import com.oceanbase.odc.plugin.schema.obmysql.parser.OBMySQLGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.MySQLObjectOperator;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan2277PartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400ConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLLessThan400TableEditor;
import com.oceanbase.tools.dbbrowser.editor.mysql.OBMySQLTableEditor;
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
    public List<DBObjectIdentity> list(@NonNull Connection connection, @NonNull String schemaName) {
        return getSchemaAccessor(connection).showTables(schemaName).stream().map(item -> {
            DBObjectIdentity identity = new DBObjectIdentity();
            identity.setType(DBObjectType.TABLE);
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
        table.setName(schemaAccessor.isLowerCaseTableName() ? tableName.toLowerCase() : tableName);
        table.setColumns(schemaAccessor.listTableColumns(schemaName, tableName));
        table.setConstraints(schemaAccessor.listTableConstraints(schemaName, tableName));
        table.setPartition(parser.getPartition());
        table.setIndexes(schemaAccessor.listTableIndexes(schemaName, tableName));
        table.setDDL(ddl);
        table.setTableOptions(schemaAccessor.getTableOptions(schemaName, tableName));
        table.setStats(getTableStats(connection, schemaName, tableName));
        table.setColumnGroups(schemaAccessor.listTableColumnGroups(schemaName, tableName));
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

    protected DBTableEditor getTableEditor(Connection connection) {
        String dbVersion = DBAccessorUtil.getDbVersion(connection);
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400TableEditor(new OBMySQLIndexEditor(), new MySQLColumnEditor(),
                    getDBTableConstraintEditor(connection, dbVersion),
                    getDBTablePartitionEditor(connection, dbVersion));
        }
        return new OBMySQLTableEditor(new OBMySQLIndexEditor(), new MySQLColumnEditor(),
                getDBTableConstraintEditor(connection, dbVersion),
                getDBTablePartitionEditor(connection, dbVersion));
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

    protected DBTablePartitionEditor getDBTablePartitionEditor(Connection connection, String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "2.2.77")) {
            return new OBMySQLLessThan2277PartitionEditor();
        } else if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400DBTablePartitionEditor();
        } else {
            return new OBMySQLDBTablePartitionEditor();
        }
    }

    protected DBTableConstraintEditor getDBTableConstraintEditor(Connection connection, String dbVersion) {
        if (VersionUtils.isLessThan(dbVersion, "4.0.0")) {
            return new OBMySQLLessThan400ConstraintEditor();
        }
        return new MySQLConstraintEditor();
    }

}
