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

    @Autowired
    private GlobalSearchProperties globalSearchProperties;

    /**
     * 根据数据库集合提交任务
     *
     * @param databases 数据库集合
     */
    public void submitTaskByDatabases(@NonNull Collection<Database> databases) {
        // 如果数据库集合为空或全局搜索未启用，则直接返回
        if (CollectionUtils.isEmpty(databases) || !globalSearchProperties.isEnableGlobalSearch()) {
            return;
        }
        // 获取数据库ID集合
        Set<Long> databaseIds = databases.stream().map(Database::getId).collect(Collectors.toSet());
        // 更新数据库对象同步状态为待处理
        databaseService.updateObjectSyncStatus(databaseIds, DBObjectSyncStatus.PENDING);
        // 遍历数据库集合，提交同步数据库对象的任务
        databases.forEach(database -> {
            try {
                executor.submit(generateTask(database));
            } catch (Exception e) {
                // 如果提交任务失败，则更新数据库对象的最后同步时间和状态为失败，并记录警告日志
                databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.FAILED);
                log.warn("Failed to submit sync database schema task for database id={}", database.getId(), e);
            }
        });
    }

    /**
     * 根据数据源提交任务
     *
     * @param dataSource 数据源配置
     */
    public void submitTaskByDataSource(@NonNull ConnectionConfig dataSource) {
        // 获取数据源对应的所有数据库
        List<Database> databases = databaseService.listExistDatabasesByConnectionId(dataSource.getId());
        // 根据同步属性和数据库方言类型，判断是否需要排除某些数据库，并移除对应的数据库
        databases.removeIf(e -> (syncProperties.isBlockExclusionsWhenSyncDbSchemas()
                && syncProperties.getExcludeSchemas(dataSource.getDialectType()).contains(e.getName()))
                || e.getObjectSyncStatus() == DBObjectSyncStatus.PENDING);
        // 剩余的数据库提交同步数据库对象任务
        submitTaskByDatabases(databases);
    }

    /**
     * 生成一个用于同步数据库模式的任务
     *
     * @param database 要同步的数据库对象
     * @return 返回一个Callable对象，用于执行同步任务
     */
    private Callable<Void> generateTask(@NonNull Database database) {
        return () -> {
            try {
                // 更新数据库对象的同步状态为正在同步
                databaseService.updateObjectSyncStatus(Collections.singleton(database.getId()),
                        DBObjectSyncStatus.SYNCING);
                if (dbSchemaSyncService.sync(database)) {
                    // 如果同步成功，则更新数据库对象的最后同步时间和同步状态为已同步
                    databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.SYNCED);
                } else {
                    // 如果同步过程中出现异常，则更新数据库对象的最后同步时间和同步状态为同步失败，并记录日志
                    databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.FAILED);
                }
            } catch (ConflictException e) {
                // Ignore conflict exception because it means the database is being synchronized by another thread
                // 忽略冲突异常，因为它意味着数据库正在被另一个线程同步
            } catch (Exception e) {
                // 如果同步过程中出现异常，则更新数据库对象的最后同步时间和同步状态为同步失败，并记录日志
                databaseService.updateObjectLastSyncTimeAndStatus(database.getId(), DBObjectSyncStatus.FAILED);
                log.warn("Failed to synchronize schema for database id={}", database.getId(), e);
            }
            return null;
        };
    }

}
