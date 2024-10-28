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
package com.oceanbase.odc.migrate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.springframework.integration.jdbc.lock.JdbcLockRegistry;

import com.oceanbase.odc.core.migrate.DefaultSchemaHistoryRepository;
import com.oceanbase.odc.core.migrate.MigrateConfiguration;
import com.oceanbase.odc.core.migrate.Migrates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMigrate {

    private static final String LOCK_KEY = "ODC_METADB_MIGRATE";
    private static final long TRY_LOCK_TIMEOUT_SECONDS = 60L;

    abstract public MigrateConfiguration migrateConfiguration();

    protected void doMigrate(JdbcLockRegistry jdbcLockRegistry) throws InterruptedException {
        log.info("try lock...");
        Lock lock = jdbcLockRegistry.obtain(LOCK_KEY);
        if (lock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            try {
                log.info("get lock success");
                MigrateConfiguration configuration = migrateConfiguration();
                log.info("init configuration success, migrate starting, initVersion={}",
                        configuration.getInitVersion());

                new Migrates(configuration, new DefaultSchemaHistoryRepository(
                        configuration.getDataSource())).migrate();
                log.info("migrate success");
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("failed to start migrate due try lock timeout, TRY_LOCK_TIMEOUT_SECONDS={}",
                    TRY_LOCK_TIMEOUT_SECONDS);
            throw new RuntimeException("failed to start migrate due try lock timeout");
        }
    }

}
