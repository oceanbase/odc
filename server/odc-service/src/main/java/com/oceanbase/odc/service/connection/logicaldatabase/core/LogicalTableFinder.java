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
package com.oceanbase.odc.service.connection.logicaldatabase.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.google.common.annotations.VisibleForTesting;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionFactory;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import jakarta.validation.constraints.NotEmpty;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/3/22 17:20
 * @Description: []
 */
@Slf4j
@SkipAuthorize("internal usage")
public class LogicalTableFinder {
    private final List<Database> databases;
    private final Map<Long, ConnectionConfig> id2DataSource;
    private final Map<Long, List<Database>> dataSourceId2Databases;
    private final DBObjectRepository dbObjectRepository;

    public LogicalTableFinder(@NotEmpty List<Database> databases,
            @NonNull DBObjectRepository dbObjectRepository) {
        this.databases = databases;
        this.id2DataSource = new HashMap<>();
        this.dataSourceId2Databases =
                databases.stream().collect(Collectors.groupingBy(database -> database.getDataSource().getId()));
        databases.stream()
                .forEach(
                        database -> this.id2DataSource.put(database.getDataSource().getId(), database.getDataSource()));
        this.dbObjectRepository = dbObjectRepository;
    }

    @VisibleForTesting
    public List<DataNode> transferToDataNodes() {
        List<DataNode> dataNodes = new ArrayList<>();
        Set<Long> dataSourceIds = dataSourceId2Databases.keySet();
        for (Long dataSourceId : dataSourceIds) {
            List<Database> groupedDatabases = dataSourceId2Databases.get(dataSourceId);
            Map<String, Database> name2Database =
                    groupedDatabases.stream().collect(Collectors.toMap(Database::getName, database -> database));
            ConnectionConfig dataSource = id2DataSource.get(dataSourceId);
            getSchemaName2TableNames(groupedDatabases).entrySet().forEach(entry -> {
                String databaseName = entry.getKey();
                List<DBObjectEntity> tables = entry.getValue();
                tables.forEach(table -> {
                    Database database = name2Database.get(databaseName);
                    if (Objects.isNull(database)) {
                        throw new UnexpectedException("Database not found: " + databaseName);
                    }
                    DataNode dataNode =
                            new DataNode(dataSource, database.getId(), databaseName, table.getId(), table.getName());
                    dataNodes.add(dataNode);
                });
            });
        }
        return dataNodes;
    }

    public List<LogicalTable> find() {
        List<DataNode> dataNodes = transferToDataNodes();
        log.info("Data nodes: {}", dataNodes);

        List<LogicalTable> logicalTableCandidates = LogicalTableRecognitionUtils.recognizeLogicalTables(dataNodes);
        log.info("Logical table candidates: {}", logicalTableCandidates);

        Map<Long, List<DataNode>> dataSourceId2DataNodes = logicalTableCandidates.stream()
                .flatMap(logicalTable -> logicalTable.getActualDataNodes().stream()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(dataNode -> dataNode.getDataSourceConfig().getId()));

        Map<String, DBTable> tables = new HashMap<>();
        for (Map.Entry<Long, List<DataNode>> entry : dataSourceId2DataNodes.entrySet()) {
            ConnectionConfig dataSource = id2DataSource.get(entry.getKey());
            Map<String, List<String>> schemaName2TableNames = entry.getValue().stream()
                    .collect(Collectors.groupingBy(DataNode::getSchemaName,
                            Collectors.mapping(DataNode::getTableName, Collectors.toList())));
            schemaName2TableNames.entrySet().forEach(schemaName2TableNamesEntry -> {
                String schemaName = schemaName2TableNamesEntry.getKey();
                List<String> tableNames = schemaName2TableNamesEntry.getValue();
                tables.putAll(getTableName2Tables(dataSource, schemaName, tableNames).entrySet().stream()
                        .collect(Collectors.toMap(key -> dataSource.getId() + "." + schemaName + "." + key.getKey(),
                                Map.Entry::getValue)));
            });
        }

        for (LogicalTable logicalTable : logicalTableCandidates) {
            Map<String, List<DataNode>> sha1ToDataNodes = new HashMap<>();
            final List<DataNode>[] majorityDataNodes = new List[] {new ArrayList<>()};

            for (DataNode dataNode : logicalTable.getActualDataNodes()) {
                String dataNodeSignature = dataNode.getStructureSignature(tables
                        .getOrDefault(dataNode.getDataSourceConfig().getId() + "." + dataNode.getFullName(), null));
                sha1ToDataNodes.computeIfAbsent(dataNodeSignature, k -> new ArrayList<>()).add(dataNode);
                if (sha1ToDataNodes.get(dataNodeSignature).size() > majorityDataNodes[0].size()) {
                    majorityDataNodes[0] = sha1ToDataNodes.get(dataNodeSignature);
                }
            }
            majorityDataNodes[0].sort(Comparator.comparing(DataNode::getTableName));
            logicalTable.setActualDataNodes(majorityDataNodes[0]);
        }

        List<LogicalTable> finalLogicalTables =
                LogicalTableRecognitionUtils.recognizeLogicalTablesWithExpression(logicalTableCandidates.stream()
                        .map(LogicalTable::getActualDataNodes).flatMap(List::stream).collect(Collectors.toList()));

        return finalLogicalTables;
    }

    public Map<String, List<DBObjectEntity>> getSchemaName2TableNames(List<Database> groupedDatabases) {
        if (CollectionUtils.isEmpty(groupedDatabases)) {
            return Collections.emptyMap();
        }
        return groupedDatabases.stream().collect(Collectors.toMap(Database::getName, database -> {
            try {
                return dbObjectRepository.listPhysicalTablesThatNotBelongsToLogicalTables(database.getId());
            } catch (Exception e) {
                log.error("Failed to get table names from schema: {}", database.getName(), e);
                return Collections.emptyList();
            }
        }));
    }

    public static Map<String, DBTable> getTableName2Tables(ConnectionConfig dataSource, String schemaName,
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
