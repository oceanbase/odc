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
package com.oceanbase.odc.service.db.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncProperties;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/4/15 10:18
 */
@Slf4j
@Component
public class DBSchemaSyncTaskManager {

    @Autowired
    @Qualifier("syncDBSchemaTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @Autowired
    private DBSchemaSyncService dbSchemaSyncService;

    @Autowired
    @Lazy
    private DatabaseService databaseService;

    @Autowired
    private DBSchemaSyncProperties syncProperties;

    public void submitTaskByDatabases(@NonNull Collection<Database> databases) {
        if (CollectionUtils.isEmpty(databases)) {
            return;
        }
        Set<Long> databaseIds = databases.stream().map(Database::getId).collect(Collectors.toSet());
        databaseService.updateObjectSyncStatus(databaseIds, DBObjectSyncStatus.PENDING);
        databases.forEach(database -> {
            try {
                executor.submit(generateTask(database));
            } catch (Exception e) {
                databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.FAILED);
                log.warn("Failed to submit sync database schema task for database id={}", database.getId(), e);
            }
        });
    }

    public void submitTaskByDataSource(@NonNull ConnectionConfig dataSource) {
        List<Database> databases = databaseService.listExistDatabasesByConnectionId(dataSource.getId());
        databases.removeIf(e -> (syncProperties.isBlockExclusionsWhenSyncDbSchemas()
                && syncProperties.getExcludeSchemas(dataSource.getDialectType()).contains(e.getName()))
                || e.getObjectSyncStatus() == DBObjectSyncStatus.PENDING);
        submitTaskByDatabases(databases);
    }

    private Callable<Void> generateTask(@NonNull Database database) {
        return () -> {
            try {
                databaseService.updateObjectSyncStatus(Collections.singleton(database.getId()),
                        DBObjectSyncStatus.SYNCING);
                if (dbSchemaSyncService.sync(database)) {
                    databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.SYNCED);
                } else {
                    databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.FAILED);
                }
            } catch (ConflictException e) {
                // Ignore conflict exception because it means the database is being synchronized by another thread
            } catch (Exception e) {
                databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.FAILED);
                log.warn("Failed to synchronize schema for database id={}", database.getId(), e);
            }
            return null;
        };
    }

}
