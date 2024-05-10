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
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBPhysicalDBEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBPhysicalDBRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTablePhysicalTableRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalTableRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/5/10 14:01
 * @Description: []
 */
public class LogicalTableExtractTask implements Runnable {
    private final Database logicalDatabase;
    private final DatabaseRepository databaseRepository;
    private final DatabaseService databaseService;
    private final LogicalDBPhysicalDBRepository dbRelationRepository;
    private final LogicalTablePhysicalTableRepository tableRelationRepository;
    private final LogicalTableRepository tableRepository;
    private JdbcLockRegistry jdbcLockRegistry;

    public LogicalTableExtractTask(@NonNull Database logicalDatabase, @NonNull DatabaseRepository databaseRepository,
            @NonNull LogicalDBPhysicalDBRepository dbRelationRepository, @NonNull DatabaseService databaseService,
            @NonNull LogicalTableRepository tableRepository,
            @NonNull LogicalTablePhysicalTableRepository tableRelationRepository,
            @NonNull JdbcLockRegistry jdbcLockRegistry) {
        this.logicalDatabase = logicalDatabase;
        this.databaseRepository = databaseRepository;
        this.dbRelationRepository = dbRelationRepository;
        this.databaseService = databaseService;
        this.tableRepository = tableRepository;
        this.tableRelationRepository = tableRelationRepository;
        this.jdbcLockRegistry = jdbcLockRegistry;
    }

    @Override
    public void run() {
        List<LogicalDBPhysicalDBEntity> relations =
                dbRelationRepository.findByLogicalDatabaseId(logicalDatabase.getId());
        List<Database> physicalDatabases = databaseService.listDatabasesByIds(
                relations.stream().map(LogicalDBPhysicalDBEntity::getPhysicalDatabaseId).collect(Collectors.toList()));
        List<LogicalTable> logicalTables = new LogicalTableFinder(physicalDatabases).find();
        if (CollectionUtils.isEmpty(logicalTables)) {
            return;
        }

        Lock lock = jdbcLockRegistry.obtain("logicaltable-extract-database-id-" + logicalDatabase.getId());

        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Can not acquire jdbc lock", e);
        }

        try {
            Set<String> existedTables = tableRepository.findByLogicalDatabaseId(logicalDatabase.getId()).stream()
                    .map(LogicalTableEntity::getName).collect(
                            Collectors.toSet());

            logicalTables.stream().filter(table -> !existedTables.contains(table.getName())).forEach(table -> {
                LogicalTableEntity tableEntity = new LogicalTableEntity();
                tableEntity.setLogicalDatabaseId(logicalDatabase.getId());
                tableEntity.setName(table.getName());
                tableEntity.setExpression(table.getFullNameExpression());
                tableEntity.setLastSyncTime(new Date());
                tableEntity.setOrganizationId(logicalDatabase.getOrganizationId());
                LogicalTableEntity savedTableEntity = tableRepository.save(tableEntity);

                List<DataNode> dataNodes = table.getActualDataNodes();
                List<LogicalTablePhysicalTableEntity> physicalTableEntities = new ArrayList<>();
                dataNodes.stream().forEach(dataNode -> {
                    LogicalTablePhysicalTableEntity physicalTableEntity = new LogicalTablePhysicalTableEntity();
                    physicalTableEntity.setLogicalTableId(savedTableEntity.getId());
                    physicalTableEntity.setOrganizationId(logicalDatabase.getOrganizationId());
                    physicalTableEntity.setPhysicalDatabaseId(dataNode.getDatabaseId());
                    physicalTableEntity.setPhysicalDatabaseName(dataNode.getSchemaName());
                    physicalTableEntity.setPhysicalTableName(dataNode.getTableName());
                    physicalTableEntity.setConsistent(true);
                    physicalTableEntities.add(physicalTableEntity);
                });
                tableRelationRepository.batchCreate(physicalTableEntities);
            });
        } finally {
            lock.unlock();
        }
    }
}
