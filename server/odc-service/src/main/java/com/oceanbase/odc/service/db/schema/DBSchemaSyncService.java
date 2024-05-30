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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.ConflictException;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.syncer.DBSchemaSyncer;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/4/10 20:56
 */
@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class DBSchemaSyncService {

    @Autowired
    private ListableBeanFactory beanFactory;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    private List<DBSchemaSyncer> syncers;

    @PostConstruct
    public void init() {
        Map<String, DBSchemaSyncer> beans = beanFactory.getBeansOfType(DBSchemaSyncer.class);
        List<DBSchemaSyncer> implementations = new ArrayList<>(beans.values());
        implementations.sort(Comparator.comparingInt(Ordered::getOrder));
        this.syncers = implementations;
    }

    public boolean sync(@NonNull Database database) throws InterruptedException, SQLException {
        PreConditions.notNull(database.getDataSource(), "database.dataSource");
        Long dataSourceId = database.getDataSource().getId();
        Lock lock = jdbcLockRegistry.obtain(getSyncDBObjectLockKey(dataSourceId, database.getId()));
        if (!lock.tryLock(3, TimeUnit.SECONDS)) {
            throw new ConflictException(ErrorCodes.ResourceModifying, "Can not acquire jdbc lock");
        }
        try {
            ConnectionConfig config = connectionService.getForConnectionSkipPermissionCheck(dataSourceId);
            OBConsoleDataSourceFactory factory = new OBConsoleDataSourceFactory(config, true);
            try (SingleConnectionDataSource dataSource = (SingleConnectionDataSource) factory.getDataSource();
                    Connection conn = dataSource.getConnection()) {
                boolean success = true;
                for (DBSchemaSyncer syncer : syncers) {
                    if (syncer.supports(config.getDialectType())) {
                        try {
                            syncer.sync(conn, database, config.getDialectType());
                        } catch (UnsupportedOperationException | UnsupportedException | NotImplementedException e) {
                            // ignore unsupported exception
                        } catch (Exception e) {
                            success = false;
                            log.warn("Failed to synchronize {} for database id={}", syncer.getObjectType(),
                                    database.getId(), e);
                        }
                    }
                }
                return success;
            }
        } finally {
            lock.unlock();
        }
    }

    public String getSyncDBObjectLockKey(@NonNull Long dataSourceId, @NonNull Long databaseId) {
        return "sync-datasource-" + dataSourceId + "-database-" + databaseId;
    }

}
