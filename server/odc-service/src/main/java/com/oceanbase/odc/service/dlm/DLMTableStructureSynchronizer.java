/*
 * Copyright (c) 2024 OceanBase.
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

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.structurecompare.StructureComparisonTaskResultEntity;
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
            String tableName) throws SQLException {
        HashSet<String> tableNames = new HashSet<>();
        tableNames.add(tableName);
        sync(sourceConnectionConfig, targetConnectionConfig, tableNames);
    }

    public static void sync(ConnectionConfig sourceConnectionConfig, ConnectionConfig targetConnectionConfig,
            Set<String> tableNames) throws SQLException {
        DBStructureComparisonConfig sourceConfig = initDBStructureComparisonConfig(
                sourceConnectionConfig, tableNames);
        DBStructureComparisonConfig targetConfig = initDBStructureComparisonConfig(
                targetConnectionConfig, tableNames);
        DefaultDBStructureComparator comparator = new DefaultDBStructureComparator();
        List<DBObjectComparisonResult> compare = comparator.compare(sourceConfig, targetConfig);
        StructureComparisonTaskResultEntity res = compare.get(0).toEntity(1L,
                DialectType.OB_MYSQL);
        log.info(res.toString());
        if (StringUtils.isNotEmpty(res.getChangeSqlScript())) {
            log.info("Start to sync target table structure,sqls={}", res.getChangeSqlScript());
            try (Connection conn = targetConfig.getDataSource().getConnection()) {
                conn.prepareStatement(res.getChangeSqlScript()).execute();
            } catch (Exception e) {
                log.warn("Sync table structure failed!", e);
            }
            log.info("Sync table structure success.");
        } else {
            log.info("Table structure comparison has finished,no action is necessary.");
        }
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

}
