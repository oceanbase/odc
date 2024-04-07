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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.model.LogicalTable;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectIdentity;
import com.oceanbase.tools.dbbrowser.model.DBTable;
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
            getSchemaName2TableNames(dataSource, groupedDatabases).entrySet().forEach(entry -> {
                String databaseName = entry.getKey();
                List<String> tableNames = entry.getValue();
                tableNames.forEach(tableName -> {
                    DataNode dataNode = new DataNode(dataSource, databaseName, tableName, null);
                    dataNodes.add(dataNode);
                });
            });
        }
        List<LogicalTable> logicalTables = LogicalTableUtils.identifyLogicalTables(dataNodes);
        for (LogicalTable logicalTable : logicalTables) {
            Map<String, List<DataNode>> sha1ToDataNodes = new HashMap<>();
            final List<DataNode>[] majorityDataNodes = new List[] {new ArrayList<>()};
            for (Map.Entry<ConnectionConfig, List<DataNode>> entry : logicalTable.groupByDataSource().entrySet()) {
                ConnectionConfig dataSource = entry.getKey();
                Map<String, List<String>> schemaName2TableNames = entry.getValue().stream()
                        .collect(Collectors.groupingBy(DataNode::getSchemaName,
                                Collectors.mapping(DataNode::getTableName, Collectors.toList())));
                schemaName2TableNames.entrySet().forEach(schemaName2TableNamesEntry -> {
                    String schemaName = schemaName2TableNamesEntry.getKey();
                    List<String> tableNames = schemaName2TableNamesEntry.getValue();
                    Map<String, DBTable> tables = getTableName2Tables(dataSource, schemaName, tableNames);
                    for (DataNode dataNode : entry.getValue()) {
                        dataNode.setTable(tables.getOrDefault(dataNode.getTableName(), null));
                        String dataNodeSignature = dataNode.getStructureSignature();
                        sha1ToDataNodes.computeIfAbsent(dataNodeSignature, k -> new ArrayList<>()).add(dataNode);
                        if (sha1ToDataNodes.get(dataNodeSignature).size() > majorityDataNodes[0].size()) {
                            majorityDataNodes[0] = sha1ToDataNodes.get(dataNodeSignature);
                        }
                    }
                });
            }
            logicalTable.setActualDataNodes(majorityDataNodes[0]);
        }

        List<LogicalTable> finalLogicalTables = LogicalTableUtils.generatePatternExpressions(logicalTables.stream()
                .map(LogicalTable::getActualDataNodes).flatMap(List::stream).collect(Collectors.toList()));

        return finalLogicalTables;
    }

    private Map<String, List<String>> getSchemaName2TableNames(ConnectionConfig dataSource,
            List<Database> groupedDatabases) {
        ConnectionSessionFactory connectionSessionFactory = new DefaultConnectSessionFactory(dataSource);
        ConnectionSession connectionSession = connectionSessionFactory.generateSession();
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        try {
            return groupedDatabases.stream().collect(Collectors.toMap(Database::getName, database -> {
                try {
                    return schemaAccessor.listTables(database.getName(), null).stream()
                            .map(DBObjectIdentity::getName).collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("Failed to get table names from schema: {}", database.getName(), e);
                    return Collections.emptyList();
                }
            }));
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception ex) {
                // eat exception
            }
        }
    }

    private Map<String, DBTable> getTableName2Tables(ConnectionConfig dataSource, String schemaName,
            List<String> tableNames) {
        ConnectionSessionFactory connectionSessionFactory = new DefaultConnectSessionFactory(dataSource);
        ConnectionSession connectionSession = connectionSessionFactory.generateSession();
        DBSchemaAccessor schemaAccessor = DBSchemaAccessors.create(connectionSession);
        try {
            return schemaAccessor.getTables(schemaName, tableNames);
        } catch (Exception e) {
            log.error("Failed to get tables from schema: {}", schemaName, e);
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception ex) {
                // eat exception
            }
        }
        return Collections.emptyMap();
    }
}
