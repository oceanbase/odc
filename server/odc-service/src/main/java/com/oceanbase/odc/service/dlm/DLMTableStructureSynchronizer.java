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
package com.oceanbase.odc.service.dlm;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.odc.common.util.JdbcOperationsUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.db.browser.DBTableEditors;
import com.oceanbase.odc.service.plugin.ConnectionPluginUtil;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.structurecompare.comparedbobject.DBTableStructureComparator;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.editor.DBTableEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.dbbrowser.util.VersionUtils;
import com.oceanbase.tools.migrator.common.configure.DataSourceInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/4/9 16:39
 * @Descripition:
 */
@Slf4j
public class DLMTableStructureSynchronizer {

    public static void sync(DataSourceInfo sourceInfo, DataSourceInfo targetInfo, String srcTableName,
            String tgtTableName, Set<DBObjectType> targetType) throws Exception {
        sync(DataSourceInfoMapper.toConnectionConfig(sourceInfo), DataSourceInfoMapper.toConnectionConfig(targetInfo),
                srcTableName, tgtTableName, targetType);
    }

    public static void sync(ConnectionConfig srcConfig, ConnectionConfig tgtConfig,
            String srcTableName, String tgtTableName, Set<DBObjectType> targetType) throws Exception {
        DataSource sourceDs = new DruidDataSourceFactory(srcConfig).getDataSource();
        DataSource targetDs = new DruidDataSourceFactory(tgtConfig).getDataSource();
        if (srcConfig.getDialectType() != tgtConfig.getDialectType()) {
            log.warn("Different types of databases do not support structural synchronization.");
            return;
        }
        try {
            String tgtDbVersion = getDBVersion(tgtConfig.getType(), targetDs);
            String srcDbVersion = getDBVersion(srcConfig.getType(), sourceDs);
            if (!isSupportedSyncTableStructure(srcConfig.getDialectType(), srcDbVersion, tgtConfig.getDialectType(),
                    tgtDbVersion)) {
                log.warn("Synchronization of table structure is unsupported,sourceDbType={},targetDbType={}",
                        srcConfig.getDialectType(),
                        tgtConfig.getDialectType());
                return;
            }
            DBTableEditor tgtTableEditor = getDBTableEditor(tgtConfig.getType(), tgtDbVersion);
            DBSchemaAccessor srcAccessor = getDBSchemaAccessor(srcConfig.getType(), sourceDs, srcDbVersion);
            DBSchemaAccessor tgtAccessor = getDBSchemaAccessor(tgtConfig.getType(), targetDs, tgtDbVersion);
            DBTable srcTable = srcAccessor.getTables(srcConfig.getDefaultSchema(),
                    Collections.singletonList(srcTableName)).get(srcTableName);
            DBTable tgtTable = tgtAccessor.getTables(tgtConfig.getDefaultSchema(),
                    Collections.singletonList(tgtTableName)).get(tgtTableName);
            DBTableStructureComparator comparator = new DBTableStructureComparator(tgtTableEditor,
                    tgtConfig.getType().getDialectType(), srcConfig.getDefaultSchema(), tgtConfig.getDefaultSchema());
            List<String> changeSqlScript = new LinkedList<>();
            if (tgtTable == null) {
                srcTable.setSchemaName(tgtConfig.getDefaultSchema());
                srcTable.setName(tgtTableName);
                changeSqlScript.add(tgtTableEditor.generateCreateObjectDDL(srcTable));
            } else {
                DBObjectComparisonResult result = comparator.compare(srcTable, tgtTable);
                if (result.getComparisonResult() == ComparisonResult.INCONSISTENT) {
                    changeSqlScript = result.getSubDBObjectComparisonResult().stream()
                            .filter(o -> targetType.contains(o.getDbObjectType())
                                    && (o.getComparisonResult() == ComparisonResult.ONLY_IN_SOURCE
                                            || (o.getDbObjectType() == DBObjectType.PARTITION
                                                    && o.getComparisonResult() == ComparisonResult.INCONSISTENT)))
                            .map(DBObjectComparisonResult::getChangeScript).collect(Collectors.toList());
                }
            }
            if (!changeSqlScript.isEmpty()) {
                log.info("Start to sync target table structure,sqls={}", changeSqlScript);
                try (Connection conn = targetDs.getConnection(); Statement statement = conn.createStatement()) {
                    for (String sql : changeSqlScript) {
                        statement.addBatch(sql);
                    }
                    statement.executeBatch();
                }
            }
            log.info("Sync table structure success,executed sql count={}", changeSqlScript.size());
        } finally {
            closeDataSource(sourceDs);
            closeDataSource(targetDs);
        }
    }


    public static boolean isSupportedSyncTableStructure(DialectType srcType, String srcVersion, DialectType tgtType,
            String tgtVersion) {
        // only supports MySQL or OBMySQL
        if (!srcType.isMysql() || !tgtType.isMysql()) {
            return false;
        }
        if (srcType != tgtType) {
            return false;
        }
        // unsupported MySQL versions below 5.7.0
        if (srcType == DialectType.MYSQL && isMySQLVersionLessThan570(srcVersion)) {
            return false;
        }
        if (tgtType == DialectType.MYSQL && isMySQLVersionLessThan570(tgtVersion)) {
            return false;
        }
        return true;
    }



    private static String getDBVersion(ConnectType connectType, DataSource dataSource) throws SQLException {
        return ConnectionPluginUtil.getInformationExtension(connectType.getDialectType())
                .getDBVersion(dataSource.getConnection());
    }

    private static DBTableEditor getDBTableEditor(ConnectType connectType, String dbVersion) {
        return DBTableEditors.create(connectType, dbVersion);
    }

    private static DBSchemaAccessor getDBSchemaAccessor(ConnectType connectType, DataSource dataSource,
            String dbVersion)
            throws SQLException {
        return DBSchemaAccessors.create(JdbcOperationsUtil.getJdbcOperations(dataSource.getConnection()), null,
                connectType, dbVersion, null);
    }

    private static void closeDataSource(DataSource dataSource) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                log.warn("Structure comparison failed to close dataSource!", e);
            }
        }
    }

    private static boolean isMySQLVersionLessThan570(String version) {
        return VersionUtils.isLessThan(version, "5.7.0");
    }

}
