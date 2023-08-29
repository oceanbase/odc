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
import java.util.concurrent.atomic.AtomicReference;

import org.pf4j.Extension;

import com.oceanbase.odc.plugin.connect.obmysql.util.JdbcOperationsUtil;
import com.oceanbase.odc.plugin.schema.obmysql.OBMySQLTableExtension;
import com.oceanbase.odc.plugin.schema.oboracle.parser.OBOracleGetDBTableByParser;
import com.oceanbase.odc.plugin.schema.oboracle.utils.DBAccessorUtil;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OBOracleIndexEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleColumnEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleConstraintEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleDBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.editor.oracle.OracleTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.stats.DBStatsAccessor;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

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
        DBSchemaAccessor schemaAccessor = getSchemaAccessor(connection);
        DBStatsAccessor statsAccessor = getStatsAccessor(connection);
        // Time-consuming queries methods of DBSchemaAccessor are replaced by GetDBTableByParser
        OBOracleGetDBTableByParser parser = new OBOracleGetDBTableByParser(connection, schemaName, tableName);

        DBTable table = new DBTable();
        table.setSchemaName(schemaName);
        table.setOwner(schemaName);
        table.setName(schemaAccessor.isLowerCaseTableName() ? tableName.toLowerCase() : tableName);
        table.setColumns(schemaAccessor.listTableColumns(schemaName, tableName));
        table.setConstraints(parser.listConstraints());
        table.setPartition(parser.getPartition());
        table.setIndexes(parser.listIndexes());
        table.setDDL(getTableDDL(connection, schemaName, tableName, parser));
        table.setTableOptions(schemaAccessor.getTableOptions(schemaName, tableName));
        table.setStats(statsAccessor.getTableStats(schemaName, tableName));
        return table;
    }

    private String getTableDDL(Connection connection, String schemaName, String tableName,
            OBOracleGetDBTableByParser parser) {
        String getTableDDlSql =
                "SELECT dbms_metadata.get_ddl('TABLE', '" + tableName + "', '" + schemaName + "') as DDL from dual";
        AtomicReference<String> ddlRef = new AtomicReference<>();
        JdbcOperationsUtil.getJdbcOperations(connection).query(getTableDDlSql, t -> {
            // Create table ddl like this: CREATE [GLOBAL TEMPORARY|SHARDED|DUPLICATED] TABLE T...
            String ddl = t.getString(1);
            if (Objects.nonNull(ddl)) {
                // fix: Replace " TABLE " to " TABLE schemaName."
                ddlRef.set(StringUtils.replace(ddl, " TABLE ",
                        " TABLE " + StringUtils.quoteOracleIdentifier(schemaName) + ".", 1));
            }
        });
        StringBuilder ddl = new StringBuilder(ddlRef.get());
        ddl.append(";\n");
        Map<String, String> variables = new HashMap<>();
        DBTableOptions tableOptions = getSchemaAccessor(connection).getTableOptions(schemaName, tableName);
        variables.put("schemaName", StringUtils.quoteOracleIdentifier(schemaName));
        variables.put("tableName",
                StringUtils.quoteOracleIdentifier(tableName));
        if (StringUtils.isNotEmpty(tableOptions.getComment())) {
            variables.put("comment", StringUtils.quoteOracleValue(tableOptions.getComment()));
            String tableCommentDdl = StringUtils.replaceVariables(ORACLE_TABLE_COMMENT_DDL_TEMPLATE, variables);
            ddl.append(tableCommentDdl).append(";\n");
        }
        List<DBTableColumn> columns = getSchemaAccessor(connection).listTableColumns(schemaName, tableName);
        for (DBTableColumn column : columns) {
            if (StringUtils.isNotEmpty(column.getComment())) {
                variables.put("columnName", StringUtils.quoteOracleIdentifier(column.getName()));
                variables.put("comment", StringUtils.quoteOracleValue(column.getComment()));
                String columnCommentDdl = StringUtils.replaceVariables(ORACLE_COLUMN_COMMENT_DDL_TEMPLATE, variables);
                ddl.append(columnCommentDdl).append(";\n");
            }
        }
        List<DBTableIndex> indexes = parser.listIndexes();
        for (DBTableIndex index : indexes) {
            /**
             * 如果有唯一索引，则在表的 DDL 里已经包含了对应的唯一约束 这里就不需要再去获取索引的 DDL 了，否则会重复
             */
            if (index.getType() == DBIndexType.UNIQUE || index.getPrimary()) {
                continue;
            }
            String getIndexDDLSql = "SELECT dbms_metadata.get_ddl('INDEX', '" + index.getName() + "', '" + schemaName
                    + "') as DDL from dual";
            JdbcOperationsUtil.getJdbcOperations(connection).query(getIndexDDLSql, (rs, num) -> {
                String indexDdl = rs.getString("DDL");
                if (StringUtils.isNotBlank(indexDdl)) {
                    ddl.append("\n").append(rs.getString("DDL"));
                }
                return ddl;
            });
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
        return new OracleTableEditor(new OBOracleIndexEditor(), new OracleColumnEditor(),
                new OracleConstraintEditor(), new OracleDBTablePartitionEditor());
    }
}
