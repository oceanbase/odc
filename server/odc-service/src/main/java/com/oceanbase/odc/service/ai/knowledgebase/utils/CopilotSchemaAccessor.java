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
package com.oceanbase.odc.service.ai.knowledgebase.utils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.service.ai.Constants;
import com.oceanbase.odc.service.ai.knowledgebase.dbschema.SchemaIndex;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

public class CopilotSchemaAccessor {
    public static List<SchemaIndex> listTablesSchemaIndex(List<DBObjectIdentity> tables,
            DBSchemaAccessor schemaAccessor, DBTableEditor tableEditor, Database database) {
        if (CollectionUtils.isEmpty(tables)) {
            return Collections.emptyList();
        }
        String databaseName = database.getName();
        List<SchemaIndex> SchemaIndexes = new ArrayList<>();
        List<String> tableNames = tables.stream().map(DBObjectIdentity::getName).collect(Collectors.toList());
        Map<String, List<DBTableColumn>> tableName2Column = schemaAccessor.listTableColumns(databaseName, tableNames);
        Map<String, DBTableOptions> tableName2Options = schemaAccessor.listTableOptions(databaseName);
        Map<String, List<DBTableIndex>> table2Indexes = schemaAccessor.listTableIndexes(databaseName);

        for (DBObjectIdentity table : tables) {
            DBTableOptions tableOptions = tableName2Options.get(table.getName());
            List<DBTableColumn> dbTableColumns = tableName2Column.get(table.getName());
            DBTable dbTable = new DBTable();
            dbTable.setName(table.getName());
            dbTable.setColumns(dbTableColumns);
            dbTable.setIndexes(table2Indexes.getOrDefault(table.getName(), new ArrayList<>()));
            dbTable.setTableOptions(tableOptions);
            dbTable.setConstraints(new ArrayList<>());
            dbTable.setDDL(tableEditor.generateCreateObjectDDL(dbTable));

            SchemaIndex schemaIndex = SchemaIndex.ofDBTable(dbTable);
            SchemaIndexes.add(schemaIndex);
        }
        return SchemaIndexes;
    }

    public static List<SchemaIndex> listViewsSchemaIndex(List<DBObjectIdentity> views,
            DBSchemaAccessor schemaAccessor,
            Database database) {
        List<SchemaIndex> schemaIndices = new ArrayList<>();
        Map<String, List<DBTableColumn>> viewName2Column = schemaAccessor.listBasicViewColumns(database.getName());
        for (DBObjectIdentity view : views) {
            try {
                SchemaIndex schemaIndex = new SchemaIndex();
                List<DBTableColumn> columns = viewName2Column.get(view.getName());
                String definition = MessageFormat.format(Constants.VIEW_DEFINITION_TEMPLATE, view.getName(),
                        concatColumnMetas(columns));
                schemaIndex.setName(view.getName());
                schemaIndex.setComment("");
                schemaIndex
                        .setFields(columns.stream().map(SchemaIndex::ofDBTableColumn).collect(Collectors.toList()));
                schemaIndex.setDefinition(definition);
                schemaIndices.add(schemaIndex);
            } catch (Exception e) {
                // ignore error
            }
        }
        return schemaIndices;
    }

    public static List<String> listTableDDLWithOptions(Collection<String> tables, String database,
            DBSchemaAccessor accessor, boolean ignoreError) {
        List<String> ddls = new ArrayList<>();
        for (String table : tables) {
            try {
                String ddl = accessor.getTableDDL(database, table);
                DBTableOptions options = accessor.getTableOptions(database, table, ddl);
                ddls.add(options == null ? ddl : String.format("%s\n%s", ddl, options.getComment()));
            } catch (Exception e) {
                if (!ignoreError) {
                    throw e;
                }
            }
        }
        return ddls;
    }

    private static String concatColumnMetas(List<DBTableColumn> columns) {
        StringBuilder stringBuilder = new StringBuilder();
        for (DBTableColumn column : columns) {
            stringBuilder.append(column.getName()).append(' ').append(column.getTypeName())
                    .append(" COMMENT '").append(column.getComment()).append("',\n");
        }
        return stringBuilder.toString();
    }
}
