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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/4/15 19:22
 */
@Slf4j
@Component
public class DBSchemaSyncScheduler {

    @Autowired
    private DBSchemaSyncTaskManager dbSchemaSyncTaskManager;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    private static final String LOCK_KEY = "db-schema-sync-schedule-lock";
    private static final long LOCK_HOLD_TIME_SECONDS = 10;

    @Scheduled(cron = "${odc.database.schema.sync.cron-expression:0 0 2 * * ?}")
    public void sync() throws InterruptedException {
        Lock lock = jdbcLockRegistry.obtain(LOCK_KEY);
        if (!lock.tryLock()) {
            log.info("Skip syncing database schema due to trying lock failed, may other odc-server node is handling");
            return;
        }
        try {
            doSync();
            // Sleep for a while before unlock to avoid other nodes acquiring the lock again.
            TimeUnit.SECONDS.sleep(LOCK_HOLD_TIME_SECONDS);
        } finally {
            lock.unlock();
        }
    }

    private void doSync() {
        List<Long> teamOrgIds = organizationRepository.findIdByType(OrganizationType.TEAM);
        if (CollectionUtils.isEmpty(teamOrgIds)) {
            return;
        }
        List<ConnectionConfig> dataSources = connectionService.listByOrganizationIdIn(teamOrgIds);
        if (CollectionUtils.isEmpty(dataSources)) {
            return;
        }
        Collections.shuffle(dataSources);
        for (ConnectionConfig dataSource : dataSources) {
            try {
                dbSchemaSyncTaskManager.submitTaskByDataSource(dataSource);
            } catch (Exception e) {
                log.warn("Failed to submit sync database schema task for datasource id={}", dataSource.getId(), e);
            }
        }
    }

}
