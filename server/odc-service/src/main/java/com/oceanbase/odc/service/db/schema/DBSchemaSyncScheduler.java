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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.common.ConditionOnServer;
import com.oceanbase.odc.service.config.UserConfigKeys;
import com.oceanbase.odc.service.config.UserConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2024/4/15 19:22
 */
@Slf4j
@Component
@ConditionOnServer
public class DBSchemaSyncScheduler {

    @Autowired
    private DBSchemaSyncTaskManager dbSchemaSyncTaskManager;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserConfigService userConfigService;

    @Autowired
    private JdbcLockRegistry jdbcLockRegistry;

    @Autowired
    private GlobalSearchProperties globalSearchProperties;

    private static final String LOCK_KEY = "db-schema-sync-schedule-lock";
    private static final long LOCK_HOLD_TIME_SECONDS = 10;

    @Scheduled(cron = "${odc.database.schema.sync.cron-expression:0 0 2 * * ?}")
    public void sync() throws InterruptedException {
        if (!globalSearchProperties.isEnableGlobalSearch()) {
            log.info("Skip syncing database schema due to global search is disabled");
            return;
        }
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
        this.databaseService.refreshExpiredPendingDBObjectStatus();
        List<ConnectionConfig> dataSources = new ArrayList<>();
        Map<OrganizationType, List<OrganizationEntity>> orgMap = organizationRepository.findAll().stream()
                .collect(Collectors.groupingBy(OrganizationEntity::getType));
        if (CollectionUtils.isNotEmpty(orgMap.get(OrganizationType.TEAM))) {
            dataSources.addAll(connectionService
                    .listSyncableDataSourcesByOrganizationIdIn(orgMap.get(OrganizationType.TEAM).stream()
                            .map(OrganizationEntity::getId).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(orgMap.get(OrganizationType.INDIVIDUAL))) {
            List<ConnectionConfig> individualDataSources =
                    connectionService
                            .listSyncableDataSourcesByOrganizationIdIn(orgMap.get(OrganizationType.INDIVIDUAL).stream()
                                    .map(OrganizationEntity::getId).collect(Collectors.toList()));
            for (ConnectionConfig ds : individualDataSources) {
                Map<String, Configuration> userConfigs =
                        userConfigService.getUserConfigurationsFromCache(ds.getCreatorId());
                Configuration config = userConfigs.get(UserConfigKeys.DEFAULT_ENABLE_GLOBAL_OBJECT_SEARCH);
                if (config != null && config.getValue().equalsIgnoreCase("true")) {
                    dataSources.add(ds);
                }
            }
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
