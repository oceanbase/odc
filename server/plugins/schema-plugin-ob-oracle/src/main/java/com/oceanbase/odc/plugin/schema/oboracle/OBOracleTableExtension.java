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
package com.oceanbase.odc.plugin.schema.oboracle;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.pf4j.Extension;

import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.plugin.schema.oboracle.parser.OBOracleGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.util.StringUtils;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2023/6/29
 * @since 4.2.0
 */
@Extension
public class OBOracleTableExtension extends OBMySQLTableExtension {

    private static final String ORACLE_TABLE_COMMENT_DDL_TEMPLATE =
            "COMMENT ON TABLE ${schemaName}.${tableName} IS ${comment}";
    private static final String ORACLE_COLUMN_COMMENT_DDL_TEMPLATE =
            "COMMENT ON COLUMN ${schemaName}.${tableName}.${columnName} IS ${comment}";

    @Override
    public DBTable getDetail(@NonNull Connection connection, @NonNull String schemaName, @NonNull String tableName) {
        DBSchemaAccessor accessor = getSchemaAccessor(connection);
        List<DBTableColumn> columns = accessor.listTableColumns(schemaName, tableName);
        // Time-consuming queries methods of DBSchemaAccessor are replaced by GetDBTableByParser
        OBOracleGetDBTableByParser parser = new OBOracleGetDBTableByParser(connection, schemaName, tableName);
        DBTable table = new DBTable();
        table.setSchemaName(schemaName);
        table.setOwner(schemaName);
        table.setName(tableName);
        table.setColumns(columns);
        /**
         * If the constraint name cannot be obtained through ddl of the table, then the constraint
         * information will still be obtained through DBSchemaAccessor
         */
        List<DBTableConstraint> constraints = parser.listConstraints();
        table.setConstraints(constraints.stream().anyMatch(c -> Objects.isNull(c.getName()))
                ? accessor.listTableConstraints(schemaName, tableName)
                : constraints);
        table.setPartition(parser.getPartition());
        table.setIndexes(parser.listIndexes());
        DBTableOptions tableOptions = accessor.getTableOptions(schemaName, tableName);
        table.setTableOptions(tableOptions);
        table.setDDL(getTableDDL(connection, schemaName, tableName, parser, columns, tableOptions));
        table.setStats(getTableStats(connection, schemaName, tableName));
        table.setColumnGroups(accessor.listTableColumnGroups(schemaName, tableName));
        return table;
    }

    private String getTableDDL(Connection connection, String schemaName, String tableName,
            OBOracleGetDBTableByParser parser, List<DBTableColumn> columns, DBTableOptions tableOptions) {
        CreateTable createTableStmt = parser.getCreateTableStmt();
        Validate.notNull(createTableStmt, "CreateTable statement can not be null");
        StringBuilder ddl = new StringBuilder();
        if (StringUtils.isBlank(createTableStmt.getSchema())) {
            ddl.append(createTableStmt.getText().replaceFirst("TABLE " + StringUtils.quoteOracleIdentifier(tableName),
                    "TABLE " + StringUtils.quoteOracleIdentifier(schemaName) + "."
                            + StringUtils.quoteOracleIdentifier(tableName)));
        } else {
            ddl.append(createTableStmt.getText());
        }
        ddl.append(";\n");
        Map<String, String> variables = new HashMap<>();
        variables.put("schemaName", StringUtils.quoteOracleIdentifier(schemaName));
        variables.put("tableName",
                StringUtils.quoteOracleIdentifier(tableName));
        if (StringUtils.isNotEmpty(tableOptions.getComment())) {
            variables.put("comment", StringUtils.quoteOracleValue(tableOptions.getComment()));
            String tableCommentDdl = StringUtils.replaceVariables(ORACLE_TABLE_COMMENT_DDL_TEMPLATE, variables);
            ddl.append(tableCommentDdl).append(";\n");
        }
        for (DBTableColumn column : columns) {
            if (StringUtils.isNotEmpty(column.getComment())) {
                variables.put("columnName", StringUtils.quoteOracleIdentifier(column.getName()));
                variables.put("comment", StringUtils.quoteOracleValue(column.getComment()));
                String columnCommentDdl = StringUtils.replaceVariables(ORACLE_COLUMN_COMMENT_DDL_TEMPLATE, variables);
                ddl.append(columnCommentDdl).append(";\n");
            }
        }
        List<DBTableIndex> indexes = parser.listIndexes();
        List<String> constraintNames = parser.listConstraints().stream().map(DBTableConstraint::getName).filter(
                Objects::nonNull).collect(Collectors.toList());
        List<DBTableConstraint> constraintNameIsNull =
                parser.listConstraints().stream().filter(cons -> Objects.isNull(cons.getName())).collect(
                        Collectors.toList());
        for (DBTableIndex index : indexes) {
            /**
             * If it is a unique index and the corresponding unique constraint is already included in the DDL of
             * the table, there is no need to obtain the DDL of the index, otherwise it will be repeated.
             */
            if (index.getPrimary() || constraintNames.contains(index.getName())
                    || constraintNameIsNull.stream().anyMatch(con -> DBConstraintType.UNIQUE_KEY == con.getType()
                            && con.getColumnNames().equals(index.getColumnNames()))) {
                continue;
            }
            String indexDdl = parser.getIndexName2Ddl().get(index.getName());
            if (StringUtils.isNotBlank(indexDdl)) {
                ddl.append("\n").append(indexDdl);
            }
        }
        return ddl.toString();
    }

    @Override
    protected DBSchemaAccessor getSchemaAccessor(Connection connection) {
        return DBAccessorUtil.getSchemaAccessor(connection);
    }

    @Override
    protected DBStatsAccessor getStatsAccessor(Connection connection) {
        return DBAccessorUtil.getStatsAccessor(connection);
    }

    @Override
    protected DBTableEditor getTableEditor(Connection connection) {
        return DBAccessorUtil.getTableEditor(connection);
    }

}
