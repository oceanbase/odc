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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/3/22 17:20
 * @Description: []
 */
@Slf4j
@Validated
@Component
public class LogicalTableFinder {
    public List<LogicalTable> find(@NotEmpty List<Database> databases) {
        Map<Long, ConnectionConfig> id2DataSources = new HashMap<>();
        Map<ConnectionConfig, List<Database>> dataSource2Databases = new HashMap<>();
        for (Database database : databases) {
            Long dataSourceId = database.getDataSource().getId();
            ConnectionConfig connectionConfig = id2DataSources.computeIfAbsent(dataSourceId,
                    k -> database.getDataSource());
            List<Database> groupedDatabases = dataSource2Databases.computeIfAbsent(connectionConfig,
                    k -> new ArrayList<>());
            groupedDatabases.add(database);
        }
        List<DataNode> dataNodes = new ArrayList<>();
        Set<ConnectionConfig> dataSources = dataSource2Databases.keySet();
        for (ConnectionConfig dataSource : dataSources) {
            List<Database> groupedDatabases = dataSource2Databases.get(dataSource);
            findTableNames(dataSource, groupedDatabases).entrySet().forEach(entry -> {
                String databaseName = entry.getKey();
                List<String> tableNames = entry.getValue();
                tableNames.forEach(tableName -> {
                    DataNode dataNode = new DataNode(dataSource, databaseName, tableName);
                    dataNodes.add(dataNode);
                });
            });
        }
        List<LogicalTable> logicalTables = LogicalTableUtils.identifyLogicalTables(dataNodes);
        // TODO: use DBStructureComparator to compare if the table ddls are the same; if not, remove the
        // DataNode

        return logicalTables;
    }

    private Map<String, List<String>> findTableNames(ConnectionConfig dataSource, List<Database> databases) {
        ConnectionSessionFactory connectionSessionFactory = new DefaultConnectSessionFactory(dataSource);
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSessionFactory.generateSession());
        // TODO: add a batch-list-table-names method in DBSchemaAccessor
        return null;
    }
}
