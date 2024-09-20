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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.TableMappingRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.logicaldatabase.core.LogicalTableFinder;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.DataNode;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalTable;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.table.TableService;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2024/5/10 14:01
 * @Description: []
 */
@Slf4j
public class LogicalTableExtractTask implements Runnable {
    private final Database logicalDatabase;
    private final DatabaseService databaseService;
    private final DatabaseMappingRepository dbRelationRepository;
    private final TableMappingRepository tableRelationRepository;
    private final DBObjectRepository dbObjectRepository;
    private final ConnectionService connectionService;
    private final TableService tableService;
    private final User creator;
    private JdbcLockRegistry jdbcLockRegistry;

    public LogicalTableExtractTask(@NonNull Database logicalDatabase, @NonNull DatabaseRepository databaseRepository,
            @NonNull DatabaseMappingRepository dbRelationRepository, @NonNull DatabaseService databaseService,
            @NonNull DBObjectRepository dbObjectRepository,
            @NonNull TableMappingRepository tableRelationRepository, @NonNull ConnectionService connectionService,
            @NonNull JdbcLockRegistry jdbcLockRegistry, @NonNull User creator, @NonNull TableService tableService) {
        this.logicalDatabase = logicalDatabase;
        this.dbRelationRepository = dbRelationRepository;
        this.databaseService = databaseService;
        this.dbObjectRepository = dbObjectRepository;
        this.tableRelationRepository = tableRelationRepository;
        this.connectionService = connectionService;
        this.jdbcLockRegistry = jdbcLockRegistry;
        this.creator = creator;
        this.tableService = tableService;
    }

    @Override
    public void run() {
        SecurityContextUtils.setCurrentUser(creator);
        databaseService.updateObjectLastSyncTimeAndStatus(logicalDatabase.getId(), DBObjectSyncStatus.SYNCING);
        List<DatabaseMappingEntity> relations =
                dbRelationRepository.findByLogicalDatabaseId(logicalDatabase.getId());
        List<Database> physicalDatabases = databaseService.listDatabasesDetailsByIds(
                relations.stream().map(DatabaseMappingEntity::getPhysicalDatabaseId).collect(Collectors.toList()));
        Map<Long, ConnectionConfig> id2DataSources = connectionService.listForConnectionSkipPermissionCheck(
                physicalDatabases.stream().map(database -> database.getDataSource().getId()).collect(
                        Collectors.toList()))
                .stream().collect(Collectors.toMap(ConnectionConfig::getId, connection -> connection));
        physicalDatabases.stream()
                .forEach(database -> database.setDataSource(id2DataSources.get(database.getDataSource().getId())));

        // sync tables before try to extract logical tables
        syncDBTables(physicalDatabases);

        List<LogicalTable> logicalTables = new LogicalTableFinder(physicalDatabases, dbObjectRepository).find();
        if (CollectionUtils.isEmpty(logicalTables)) {
            databaseService.updateObjectLastSyncTimeAndStatus(logicalDatabase.getId(), DBObjectSyncStatus.SYNCED);
            return;
        }

        Lock lock = jdbcLockRegistry.obtain("logicaltable-extract-database-id-" + logicalDatabase.getId());

        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
            }
        } catch (InterruptedException e) {
            databaseService.updateObjectLastSyncTimeAndStatus(logicalDatabase.getId(), DBObjectSyncStatus.FAILED);
            throw new RuntimeException(e);
        }

        try {
            Set<String> existedTables = dbObjectRepository.findByDatabaseIdAndType(logicalDatabase.getId(),
                    DBObjectType.LOGICAL_TABLE).stream().map(DBObjectEntity::getName).collect(Collectors.toSet());

            logicalTables.stream().filter(table -> !existedTables.contains(table.getName())).forEach(table -> {
                DBObjectEntity tableEntity = new DBObjectEntity();
                tableEntity.setDatabaseId(logicalDatabase.getId());
                tableEntity.setType(DBObjectType.LOGICAL_TABLE);
                tableEntity.setName(table.getName());
                tableEntity.setOrganizationId(logicalDatabase.getOrganizationId());
                DBObjectEntity savedTableEntity = dbObjectRepository.save(tableEntity);

                List<DataNode> dataNodes = table.getActualDataNodes();
                List<TableMappingEntity> physicalTableEntities = new ArrayList<>();
                dataNodes.stream().forEach(dataNode -> {
                    TableMappingEntity physicalTableEntity = new TableMappingEntity();
                    physicalTableEntity.setLogicalTableId(savedTableEntity.getId());
                    physicalTableEntity.setOrganizationId(logicalDatabase.getOrganizationId());
                    physicalTableEntity.setPhysicalDatabaseId(dataNode.getDatabaseId());
                    physicalTableEntity.setPhysicalDatabaseName(dataNode.getSchemaName());
                    physicalTableEntity.setPhysicalTableId(dataNode.getTableId());
                    physicalTableEntity.setPhysicalTableName(dataNode.getTableName());
                    physicalTableEntity.setExpression(table.getFullNameExpression());
                    physicalTableEntity.setConsistent(true);
                    physicalTableEntities.add(physicalTableEntity);
                });
                tableRelationRepository.batchCreate(physicalTableEntities);
                databaseService.updateObjectLastSyncTimeAndStatus(logicalDatabase.getId(), DBObjectSyncStatus.SYNCED);
            });
        } catch (Exception ex) {
            log.warn("Failed to extract logical tables for database id={}", logicalDatabase.getId(), ex);
            databaseService.updateObjectLastSyncTimeAndStatus(logicalDatabase.getId(), DBObjectSyncStatus.FAILED);
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    private void syncDBTables(List<Database> physicalDatabases) {
        if (CollectionUtils.isEmpty(physicalDatabases)) {
            return;
        }
        try {
            physicalDatabases.stream().forEach(database -> {
                OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(database.getDataSource(), true);
                try (SingleConnectionDataSource ds = (SingleConnectionDataSource) factory.getDataSource();
                        Connection conn = ds.getConnection()) {
                    tableService.syncDBTables(conn, database, database.getDataSource().getDialectType());
                } catch (SQLException | InterruptedException e) {
                    log.warn("Failed to sync tables for database: {}", database.getName(), e);
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to sync tables for databases: {}", physicalDatabases.stream().map(Database::getName)
                    .collect(Collectors.joining(",")), ex);
        }
    }
}
