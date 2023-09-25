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
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
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
    @Qualifier("syncDatabaseTaskExecutor")
    private ThreadPoolTaskExecutor executor;

    @SkipAuthorize("internal usage")
    public Future<Boolean> submitSyncDataSourceTask(@NonNull ConnectionConfig connection) {
        return doExecute(() -> executor.submit(() -> {
            SecurityContextUtils.setCurrentUser(connection.getCreatorId(), connection.getOrganizationId(), null);
            return databaseService.internalSyncDataSourceSchemas(connection.getId());
        }));
    }

    private Future<Boolean> doExecute(Supplier<Future<Boolean>> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            throw new BadRequestException("sync database failed");
        }
    }
}
