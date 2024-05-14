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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditorFactory;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter.ComparisonScope;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.structurecompare.comparedbobject.DBTableStructureComparator;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
@NoArgsConstructor
@Slf4j
public class DefaultDBStructureComparator implements DBStructureComparator {
    private final List<DialectType> supportedDialectTypes =
            Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE);
    private final List<DBObjectType> supportedDBObjectTypes = Arrays.asList(DBObjectType.TABLE);
    private DBTableStructureComparator tableComparator;
    private ComparisonScope scope;
    private Integer totalObjectCount = null;
    private Integer completedObjectCount = 0;

    @Override
    public List<DBObjectComparisonResult> compare(@NonNull DBStructureComparisonConfig srcConfig,
            @NonNull DBStructureComparisonConfig tgtConfig) throws SQLException {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (srcConfig.getBlackListMap().isEmpty()) {
            this.scope = ComparisonScope.ALL;
        } else {
            this.scope = ComparisonScope.PART;
        }
        checkUnsupportedConfiguration(srcConfig, tgtConfig);

        String srcDbVersion = getDBVersion(srcConfig.getConnectType(), srcConfig.getDataSource());
        String tgtDbVersion = getDBVersion(tgtConfig.getConnectType(), tgtConfig.getDataSource());

        DBSchemaAccessor srcAccessor =
                getDBSchemaAccessor(srcConfig.getConnectType(), srcConfig.getDataSource(), srcDbVersion);
        DBSchemaAccessor tgtAccessor =
                getDBSchemaAccessor(tgtConfig.getConnectType(), tgtConfig.getDataSource(), tgtDbVersion);

        DBTableEditor tgtTableEditor = getDBTableEditor(tgtConfig.getConnectType(), tgtDbVersion);

        log.info(
                "DefaultDBStructureComparator start to build source and target schema tables, source schema name={}, target schema name={}",
                srcConfig.getSchemaName(), tgtConfig.getSchemaName());
        long startTimestamp = System.currentTimeMillis();
        Map<String, DBTable> srcTableName2Table = srcAccessor.getTables(srcConfig.getSchemaName(), null);
        Map<String, DBTable> tgtTableName2Table = tgtAccessor.getTables(tgtConfig.getSchemaName(), null);

        if (srcConfig.getConnectType().getDialectType().isMysql()) {
            srcTableName2Table.values().forEach(StringUtils::quoteColumnDefaultValuesForMySQL);
            tgtTableName2Table.values().forEach(StringUtils::quoteColumnDefaultValuesForMySQL);
        }
        log.info(
                "DefaultDBStructureComparator build source and target schema tables success, time consuming={} seconds",
                (System.currentTimeMillis() - startTimestamp) / 1000);

        tableComparator = new DBTableStructureComparator(tgtTableEditor,
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
            Set<String> tableNamesToBeCompared = srcConfig.getBlackListMap().get(DBObjectType.TABLE);
            this.totalObjectCount = tableNamesToBeCompared.size();
            for (String tableName : tableNamesToBeCompared) {
                if (srcTableName2Table.containsKey(tableName)) {
                    returnVal.add(tableComparator.compare(srcTableName2Table.get(tableName),
                            tgtTableName2Table.get(tableName)));
                    this.completedObjectCount++;
                } else {
                    DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.TABLE, tableName,
                            srcConfig.getSchemaName(), tgtConfig.getSchemaName());
                    result.setComparisonResult(ComparisonResult.MISSING_IN_SOURCE);
                    this.completedObjectCount++;
                    returnVal.add(result);
                }
            }
        }
        return returnVal;
    }

    private DBSchemaAccessor getDBSchemaAccessor(ConnectType connectType, DataSource dataSource, String dbVersion)
            throws SQLException {
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

    @Override
    public Double getProgress() {
        if (this.scope == ComparisonScope.ALL) {
            if (tableComparator != null) {
                return tableComparator.getProgress();
            } else {
                return 0.0D;
            }
        } else {
            if (totalObjectCount == null) {
                return 0.0D;
            } else if (totalObjectCount == 0) {
                return 100.0D;
            } else {
                double progress = completedObjectCount * 100D / totalObjectCount;
                return Math.min(progress, 100.0D);
            }
        }
    }
}
