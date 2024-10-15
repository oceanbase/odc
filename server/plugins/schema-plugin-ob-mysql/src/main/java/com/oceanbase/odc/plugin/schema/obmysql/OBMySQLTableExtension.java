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

    /**
     * 根据给定的连接、模式名和表名获取表的详细信息
     *
     * @param connection 数据库连接
     * @param schemaName 模式名
     * @param tableName  表名
     * @return 包含表详细信息的DBTable对象
     */
    @Override
    public DBTable getDetail(@NonNull Connection connection, @NonNull String schemaName, @NonNull String tableName) {
        // 获取模式访问器
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        // 获取表的DDL语句
        String ddl = schemaAccessor.getTableDDL(schemaName, tableName);
        // 通过DDL语句解析表的分区信息
        OBMySQLGetDBTableByParser parser = new OBMySQLGetDBTableByParser(ddl);

        // 创建DBTable对象并设置基本信息
        DBTable table = new DBTable();
        table.setSchemaName(schemaName);
        table.setOwner(schemaName);
        table.setName(tableName);
        // 获取表的列信息并设置到DBTable对象中
        table.setColumns(schemaAccessor.listTableColumns(schemaName, tableName));
        // 获取表的约束信息并设置到DBTable对象中
        table.setConstraints(schemaAccessor.listTableConstraints(schemaName, tableName));
        // 设置表的分区信息
        table.setPartition(parser.getPartition());
        // 获取表的索引信息并设置到DBTable对象中
        table.setIndexes(schemaAccessor.listTableIndexes(schemaName, tableName));
        // 设置表的DDL语句
        table.setDDL(ddl);
        // 获取表的选项信息并设置到DBTable对象中
        table.setTableOptions(schemaAccessor.getTableOptions(schemaName, tableName));
        // 获取表的统计信息并设置到DBTable对象中
        table.setStats(getTableStats(connection, schemaName, tableName));
        try {
            // 获取表的列组信息并设置到DBTable对象中
            table.setColumnGroups(schemaAccessor.listTableColumnGroups(schemaName, tableName));
        } catch (Exception e) {
            // eat the exception
            // 忽略异常
        }
        return table;
        // 忽略异常
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
