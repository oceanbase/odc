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
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.tools.dbbrowser.model.DBTable;

/**
 * @Author: Lebie
 * @Date: 2024/5/15 19:34
 * @Description: []
 */
public class LogicalTableCheckConsistencyTask implements Runnable {
    private final LogicalTableEntity table;
    private final LogicalTablePhysicalTableRepository relationRepository;
    private final DatabaseService databaseService;
    private final DatabaseRepository databaseRepository;

    public LogicalTableCheckConsistencyTask(LogicalTableEntity table,
            LogicalTablePhysicalTableRepository relationRepository, DatabaseService databaseService,
            DatabaseRepository databaseRepository) {
        this.table = table;
        this.relationRepository = relationRepository;
        this.databaseService = databaseService;
        this.databaseRepository = databaseRepository;
    }

    @Override
    public void run() {
        List<LogicalTablePhysicalTableEntity> relations =
                relationRepository.findByLogicalTableIdIn(Collections.singleton(table.getId()));

        Set<Long> databaseIds = relations.stream().map(LogicalTablePhysicalTableEntity::getPhysicalDatabaseId)
                .collect(Collectors.toSet());
        Map<Long, Database> id2Databases = databaseService.listDatabasesDetailsByIds(databaseIds).stream()
                .collect(Collectors.toMap(Database::getId, database -> database));

        Map<String, List<LogicalTablePhysicalTableEntity>> signature2Tables = new HashMap<>();
        relations.stream().collect(Collectors.groupingBy(LogicalTablePhysicalTableEntity::getPhysicalDatabaseId))
                .forEach((databaseId, physicalTables) -> {
                    Database database = id2Databases.get(databaseId);
                    if (Objects.isNull(database)) {
                        throw new UnexpectedException("Database not found, databaseId=" + databaseId);
                    }
                    Map<String, DBTable> tableName2Tables =
                            LogicalTableFinder.getTableName2Tables(database.getDataSource(), database.getName(),
                                    physicalTables.stream().map(LogicalTablePhysicalTableEntity::getPhysicalTableName)
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

        Optional<Entry<String, List<LogicalTablePhysicalTableEntity>>> largestEntryOptional =
                signature2Tables.entrySet().stream()
                        .max(Comparator.comparingInt(entry -> entry.getValue().size()));

        if (largestEntryOptional.isPresent()) {
            Entry<String, List<LogicalTablePhysicalTableEntity>> largestEntry = largestEntryOptional.get();
            signature2Tables.values().stream()
                    .flatMap(List::stream)
                    .forEach(table -> table.setConsistent(false));

            largestEntry.getValue().forEach(table -> table.setConsistent(true));
            relationRepository.saveAll(relations);
        }
    }
}
