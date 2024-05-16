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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.session.factory.DruidDataSourceFactory;
import com.oceanbase.odc.service.structurecompare.DefaultDBStructureComparator;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBStructureComparisonConfig;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2024/4/9 16:39
 * @Descripition:
 */
@Slf4j
public class DLMTableStructureSynchronizer {

    public static void sync(ConnectionConfig sourceConnectionConfig, ConnectionConfig targetConnectionConfig,
            String tableName, Set<DBObjectType> targetType) throws SQLException {
        HashSet<String> tableNames = new HashSet<>();
        tableNames.add(tableName);
        sync(sourceConnectionConfig, targetConnectionConfig, tableNames, targetType);
    }

    public static void sync(ConnectionConfig sourceConnectionConfig, ConnectionConfig targetConnectionConfig,
            Set<String> tableNames, Set<DBObjectType> targetType) throws SQLException {
        DBStructureComparisonConfig sourceConfig = initDBStructureComparisonConfig(
                sourceConnectionConfig, tableNames);
        DBStructureComparisonConfig targetConfig = initDBStructureComparisonConfig(
                targetConnectionConfig, tableNames);
        DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
        List<DBObjectComparisonResult> results = comparator.compare(sourceConfig, targetConfig).stream()
                .peek(o -> o.setSubDBObjectComparisonResult(o.getSubDBObjectComparisonResult().stream()
                        .filter(sb -> targetType.contains(sb.getDbObjectType())).collect(
                                Collectors.toList())))
                .collect(Collectors.toList());
        try (Connection conn = targetConfig.getDataSource().getConnection()) {
            for (DBObjectComparisonResult result : results) {
                String changeSqlScript =
                        result.toEntity(1L, targetConnectionConfig.getDialectType()).getChangeSqlScript();
                if (StringUtils.isNotEmpty(changeSqlScript)) {
                    log.info("Start to sync target table structure,sqls={}", changeSqlScript);
                    conn.prepareStatement(changeSqlScript).execute();
                    log.info("Sync table structure success.");
                } else {
                    log.info("Table structure comparison has finished,no action is necessary.");
                }
            }
        } catch (Exception e) {
            log.warn("Sync table structure failed!", e);
        }
        closeDataSource(sourceConfig.getDataSource());
        closeDataSource(targetConfig.getDataSource());
    }

    private static DBStructureComparisonConfig initDBStructureComparisonConfig(ConnectionConfig config,
            Set<String> tableNames) {
        DBStructureComparisonConfig returnValue = new DBStructureComparisonConfig();
        returnValue.setSchemaName(config.getDefaultSchema());
        returnValue.setConnectType(config.getType());
        returnValue.setDataSource(new DruidDataSourceFactory(config).getDataSource());
        returnValue.setToComparedObjectTypes(Collections.singleton(DBObjectType.TABLE));
        Map<DBObjectType, Set<String>> blackListMap = new HashMap<>();
        blackListMap.put(DBObjectType.TABLE, new HashSet<>(tableNames));
        returnValue.setBlackListMap(blackListMap);
        return returnValue;
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

}
