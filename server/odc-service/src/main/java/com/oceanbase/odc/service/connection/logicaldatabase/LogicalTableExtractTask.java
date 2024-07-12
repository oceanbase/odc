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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/5/10 14:01
 * @Description: []
 */
public class LogicalTableExtractTask implements Runnable {
    private final Database logicalDatabase;
    private final DatabaseService databaseService;
    private final DatabaseMappingRepository dbRelationRepository;
    private final TableMappingRepository tableRelationRepository;
    private final DBObjectRepository dbObjectRepository;
    private JdbcLockRegistry jdbcLockRegistry;

    public LogicalTableExtractTask(@NonNull Database logicalDatabase, @NonNull DatabaseRepository databaseRepository,
            @NonNull DatabaseMappingRepository dbRelationRepository, @NonNull DatabaseService databaseService,
            @NonNull DBObjectRepository dbObjectRepository,
            @NonNull TableMappingRepository tableRelationRepository,
            @NonNull JdbcLockRegistry jdbcLockRegistry) {
        this.logicalDatabase = logicalDatabase;
        this.dbRelationRepository = dbRelationRepository;
        this.databaseService = databaseService;
        this.dbObjectRepository = dbObjectRepository;
        this.tableRelationRepository = tableRelationRepository;
        this.jdbcLockRegistry = jdbcLockRegistry;
    }

    @Override
    public void run() {
        List<DatabaseMappingEntity> relations =
                dbRelationRepository.findByLogicalDatabaseId(logicalDatabase.getId());
        List<Database> physicalDatabases = databaseService.listDatabasesDetailsByIds(
                relations.stream().map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(Collectors.toList()));
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
            throw new RuntimeException(e);
        }
    }
}
