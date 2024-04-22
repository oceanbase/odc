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
package com.oceanbase.odc.service.connection.database;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.DBSchemaSyncTaskManager;
import com.oceanbase.odc.service.iam.OrganizationService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/14 15:29
 * @Description: []
 */
@Service
@Slf4j
public class DatabaseSyncManager {
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private UserService userService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    DBSchemaSyncTaskManager dbSchemaSyncTaskManager;
    @Autowired
    @Qualifier("syncDatabaseTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    LoadingCache<Long, UserEntity> id2UserEntity = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Long, UserEntity>() {
                @Override
                public UserEntity load(Long creatorId) {
                    return userService.nullSafeGet(creatorId);
                }
            });

    @SkipAuthorize("internal usage")
    public Future<Boolean> submitSyncDataSourceTask(@NonNull ConnectionConfig connection) {
        return doExecute(() -> executor.submit(() -> syncDBForDataSource(connection)));
    }

    @SkipAuthorize("internal usage")
    public Future<Boolean> submitSyncDataSourceAndDBSchemaTask(@NonNull ConnectionConfig connection) {
        return doExecute(() -> executor.submit(() -> {
            Boolean res = syncDBForDataSource(connection);
            // only sync db schema for team organization
            organizationService.get(connection.getOrganizationId()).ifPresent(organization -> {
                if (organization.getType() == OrganizationType.TEAM) {
                    try {
                        dbSchemaSyncTaskManager.submitTaskByDataSource(connection);
                    } catch (Exception e) {
                        log.warn("Failed to submit sync database schema task for datasource id={}", connection.getId(),
                                e);
                    }
                }
            });
            return res;
        }));
    }

    private Boolean syncDBForDataSource(@NonNull ConnectionConfig dataSource) throws InterruptedException {
        Long creatorId = dataSource.getCreatorId();
        SecurityContextUtils.setCurrentUser(creatorId, dataSource.getOrganizationId(), getAccountName(creatorId));
        return databaseService.internalSyncDataSourceSchemas(dataSource.getId());
    }

    private Future<Boolean> doExecute(Supplier<Future<Boolean>> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new BadRequestException("sync database failed");
        }
    }

    private String getAccountName(@NonNull Long creatorId) {
        try {
            UserEntity userEntity = id2UserEntity.get(creatorId);
            Validate.notNull(userEntity, "UserEntity not found by id:" + creatorId);
            return userEntity.getAccountName();
        } catch (Exception e) {
            log.warn("Failed to get user entity from cache, message:{}", e.getMessage());
            return null;
        }
    }
}
