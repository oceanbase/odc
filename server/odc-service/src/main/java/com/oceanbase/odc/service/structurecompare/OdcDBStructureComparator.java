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
package com.oceanbase.odc.service.structurecompare;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.utils.Lists;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.plugin.SchemaPluginUtil;
import com.oceanbase.odc.service.structurecompare.comparedbobject.TableStructureComparator;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
@Slf4j
public class OdcDBStructureComparator implements DBStructureComparator {
    private final List<DialectType> supportedDialectTypes =
            Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE);
    private final List<DBObjectType> supportedDBObjectTypes = Arrays.asList(DBObjectType.TABLE);

    @Override
    public List<DBObjectComparisonResult> compare(@NonNull DBStructureComparisonConfig srcConfig,
            @NonNull DBStructureComparisonConfig tgtConfig)
            throws SQLException {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        checkConfig(srcConfig, tgtConfig);

        DBSchemaAccessor srcAccessor = SchemaPluginUtil.getSchemaBrowserExtension(srcConfig.getDialectType())
                .getDBSchemaAccessor(srcConfig.getDataSource().getConnection());
        DBSchemaAccessor tgtAccessor = SchemaPluginUtil.getSchemaBrowserExtension(tgtConfig.getDialectType())
                .getDBSchemaAccessor(tgtConfig.getDataSource().getConnection());
        DBTableEditor tgtTableEditor = SchemaPluginUtil.getSchemaBrowserExtension(tgtConfig.getDialectType())
                .getDBTableEditor(tgtConfig.getDataSource().getConnection());

        Map<String, DBTable> srcTables = buildSchemaTables(srcAccessor, srcConfig.getSchemaName());
        Map<String, DBTable> tgtTables = buildSchemaTables(tgtAccessor, tgtConfig.getSchemaName());
        TableStructureComparator tableComparator = new TableStructureComparator(tgtTableEditor,
                tgtConfig.getDialectType(),
                srcConfig.getSchemaName(), tgtConfig.getSchemaName());

        if (srcConfig.getBlackListMap().get(DBObjectType.TABLE) == null) {
            /**
             * Compare all the tables between source database and target database.
             */
            returnVal =
                    tableComparator.compare(new ArrayList<>(srcTables.values()), new ArrayList<>(tgtTables.values()));
        } else {
            /**
             * Compare specified tables between source database and target database.
             */
            for (String tableName : srcConfig.getBlackListMap().get(DBObjectType.TABLE)) {
                if (srcTables.containsKey(tableName)) {
                    returnVal.add(tableComparator.compare(srcTables.get(tableName), tgtTables.get(tableName)));
                } else {
                    DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.TABLE, tableName,
                            srcConfig.getSchemaName(), tgtConfig.getSchemaName());
                    result.setComparisonResult(ComparisonResult.MISSING_IN_SOURCE);
                    returnVal.add(result);
                }
            }
        }
        return returnVal;
    }

    private void checkConfig(DBStructureComparisonConfig srcConfig, DBStructureComparisonConfig tgtConfig) {
        if (!srcConfig.getDialectType().equals(tgtConfig.getDialectType())) {
            throw new IllegalArgumentException("The dialect type of source and target schema must be equal");
        }
        if (!supportedDialectTypes.contains(srcConfig.getDialectType())) {
            throw new UnsupportedOperationException(
                    "Unsupported dialect type for schema structure comparison: " + srcConfig.getDialectType());
        }
        srcConfig.getToComparedObjectTypes().stream().forEach(dbObjectType -> {
            if (!supportedDBObjectTypes.contains(dbObjectType)) {
                throw new UnsupportedOperationException(
                        "Unsupported database object type for schema structure comparison: " + dbObjectType);
            }
        });
    }

    private Map<String, DBTable> buildSchemaTables(DBSchemaAccessor accessor, String schemaName) {
        Map<String, DBTable> returnVal = new HashMap<>();
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
                log.warn("Failed to query table column metadata information, table name: {}, schema name: {}",
                        tableName, schemaName);
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
}
