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

import javax.sql.DataSource;

import org.apache.commons.compress.utils.Lists;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.obmysql.parser.OBMySQLGetDBTableByParser;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditorFactory;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.structurecompare.comparedbobject.DBTableStructureComparator;
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
public class DefaultDBStructureComparator implements DBStructureComparator {
    private final List<DialectType> supportedDialectTypes =
            Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE);
    private final List<DBObjectType> supportedDBObjectTypes = Arrays.asList(DBObjectType.TABLE);

    @Override
    public List<DBObjectComparisonResult> compare(@NonNull DBStructureComparisonConfig srcConfig,
            @NonNull DBStructureComparisonConfig tgtConfig) throws SQLException {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        checkUnsupportedConfiguration(srcConfig, tgtConfig);

        String srcDbVersion = getDBVersion(srcConfig.getConnectType(), srcConfig.getDataSource());
        String tgtDbVersion = getDBVersion(tgtConfig.getConnectType(), tgtConfig.getDataSource());

        DBSchemaAccessor srcAccessor =
                getDBSchemaAccessor(srcConfig.getConnectType(), srcConfig.getDataSource(), srcDbVersion);
        DBSchemaAccessor tgtAccessor =
                getDBSchemaAccessor(tgtConfig.getConnectType(), tgtConfig.getDataSource(), tgtDbVersion);

        DBTableEditor tgtTableEditor = getDBTableEditor(tgtConfig.getConnectType(), tgtDbVersion);

        Map<String, DBTable> srcTableName2Table = buildSchemaTables(srcAccessor, srcConfig.getSchemaName(),
                srcConfig.getConnectType().getDialectType(), srcDbVersion);
        Map<String, DBTable> tgtTableName2Table = buildSchemaTables(tgtAccessor, tgtConfig.getSchemaName(),
                tgtConfig.getConnectType().getDialectType(), tgtDbVersion);

        DBTableStructureComparator tableComparator = new DBTableStructureComparator(tgtTableEditor,
                tgtConfig.getConnectType().getDialectType(),
                srcConfig.getSchemaName(), tgtConfig.getSchemaName());

        if (srcConfig.getBlackListMap().get(DBObjectType.TABLE) == null) {
            /**
             * Compare all the tables between source database and target database.
             */
            returnVal =
                    tableComparator.compare(new ArrayList<>(srcTableName2Table.values()),
                            new ArrayList<>(tgtTableName2Table.values()));
        } else {
            /**
             * Compare specified tables between source database and target database.
             */
            for (String tableName : srcConfig.getBlackListMap().get(DBObjectType.TABLE)) {
                if (srcTableName2Table.containsKey(tableName)) {
                    returnVal.add(tableComparator.compare(srcTableName2Table.get(tableName),
                            tgtTableName2Table.get(tableName)));
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

    private DBSchemaAccessor getDBSchemaAccessor(ConnectType connectType, DataSource dataSource, String dbVersion)
            throws SQLException {
        /**
         * sysJdbcOperations and tenantName are required for OBMySQLNoGreaterThan1479SchemaAccessor to get
         * table partition, this method will be replaced by OBMySQLGetDBTableByParser, so we just set it
         * null here.
         */
        return DBSchemaAccessors.create(JdbcOperationsUtil.getJdbcOperations(dataSource.getConnection()), null,
                connectType, dbVersion, null);
    }

    private DBTableEditor getDBTableEditor(ConnectType connectType, String dbVersion) {
        return new DBTableEditorFactory(connectType, dbVersion).create();
    }

    private String getDBVersion(ConnectType connectType, DataSource dataSource) throws SQLException {
        return ConnectionPluginUtil.getInformationExtension(connectType.getDialectType())
                .getDBVersion(dataSource.getConnection());
    }

    private void checkUnsupportedConfiguration(DBStructureComparisonConfig srcConfig,
            DBStructureComparisonConfig tgtConfig) {
        if (!srcConfig.getConnectType().getDialectType().equals(tgtConfig.getConnectType().getDialectType())) {
            throw new IllegalArgumentException("The dialect type of source and target schema must be equal");
        }
        if (!supportedDialectTypes.contains(srcConfig.getConnectType().getDialectType())) {
            throw new UnsupportedOperationException(
                    "Unsupported dialect type for schema structure comparison: "
                            + srcConfig.getConnectType().getDialectType());
        }
        srcConfig.getToComparedObjectTypes().stream().forEach(dbObjectType -> {
            if (!supportedDBObjectTypes.contains(dbObjectType)) {
                throw new UnsupportedOperationException(
                        "Unsupported database object type for schema structure comparison: " + dbObjectType);
            }
        });
    }

    private Map<String, DBTable> buildSchemaTables(DBSchemaAccessor accessor, String schemaName,
            DialectType dialectType, String dbVersion) {
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
            if (DialectType.OB_MYSQL.equals(dialectType) && VersionUtils.isLessThanOrEqualsTo(dbVersion, "1.4.79")) {
                // Remove dependence on sys account
                String ddl = accessor.getTableDDL(schemaName, tableName);
                OBMySQLGetDBTableByParser parser = new OBMySQLGetDBTableByParser(ddl);
                table.setPartition(parser.getPartition());
            } else {
                table.setPartition(accessor.getPartition(schemaName, tableName));
            }
            table.setDDL(accessor.getTableDDL(schemaName, tableName));
            returnVal.put(tableName, table);
        }
        return returnVal;
    }
}
