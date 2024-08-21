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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/5/15 19:34
 * @Description: []
 */
public class LogicalTableCheckConsistencyTask implements Runnable {
    private final Long tableId;
    private final TableMappingRepository relationRepository;
    private final DatabaseService databaseService;
    private final User creator;
    private final ConnectionService connectionService;

    public LogicalTableCheckConsistencyTask(@NonNull Long tableId,
            @NonNull TableMappingRepository relationRepository, @NonNull DatabaseService databaseService,
            @NonNull ConnectionService connectionService,
            @NonNull User creator) {
        this.tableId = tableId;
        this.relationRepository = relationRepository;
        this.databaseService = databaseService;
        this.connectionService = connectionService;
        this.creator = creator;
    }

    @Override
    public void run() {
        SecurityContextUtils.setCurrentUser(creator);
        List<TableMappingEntity> mappings = relationRepository.findByLogicalTableId(this.tableId);

        Set<Long> databaseIds = mappings.stream().map(TableMappingEntity::getPhysicalDatabaseId)
                .collect(Collectors.toSet());
        Map<Long, Database> id2Databases = databaseService.listDatabasesDetailsByIds(databaseIds).stream()
                .collect(Collectors.toMap(Database::getId, database -> database));
        Map<Long, ConnectionConfig> id2DataSources = connectionService.listForConnectionSkipPermissionCheck(
                id2Databases.values().stream().map(database -> database.getDataSource().getId()).collect(
                        Collectors.toList()))
                .stream().collect(Collectors.toMap(ConnectionConfig::getId, connection -> connection));
        id2Databases.values().stream()
                .forEach(database -> database.setDataSource(id2DataSources.get(database.getDataSource().getId())));
        Map<String, List<TableMappingEntity>> signature2Tables = new HashMap<>();
        mappings.stream().collect(Collectors.groupingBy(TableMappingEntity::getPhysicalDatabaseId))
                .forEach((databaseId, physicalTables) -> {
                    Database database = id2Databases.get(databaseId);
                    if (Objects.isNull(database)) {
                        throw new UnexpectedException("Database not found, databaseId=" + databaseId);
                    }
                    Map<String, DBTable> tableName2Tables =
                            LogicalTableFinder.getTableName2Tables(database.getDataSource(), database.getName(),
                                    physicalTables.stream().map(TableMappingEntity::getPhysicalTableName)
                                            .collect(Collectors.toList()));
                    physicalTables.forEach(physicalTable -> {
                        DBTable table = tableName2Tables.get(physicalTable.getPhysicalTableName());
                        if (Objects.isNull(table)) {
                            return;
                        }
                        signature2Tables
                                .computeIfAbsent(DataNode.getStructureSignature(table), key -> new ArrayList<>())
                                .add(physicalTable);
                    });
                });

        Optional<Entry<String, List<TableMappingEntity>>> largestEntryOptional =
                signature2Tables.entrySet().stream()
                        .max(Comparator.comparingInt(entry -> entry.getValue().size()));

        if (largestEntryOptional.isPresent()) {
            Entry<String, List<TableMappingEntity>> largestEntry = largestEntryOptional.get();
            signature2Tables.values().stream()
                    .flatMap(List::stream)
                    .forEach(table -> table.setConsistent(false));

            largestEntry.getValue().forEach(table -> table.setConsistent(true));
            relationRepository.saveAll(mappings);
        }
    }
}
