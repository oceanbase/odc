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

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.iam.OrganizationRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;

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
    private DatabaseService databaseService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Scheduled(cron = "${odc.database.schema.sync-cron-expression:0 0 2 * * ?}")
    public void sync() {
        List<Long> teamOrgIds = organizationRepository.findIdByType(OrganizationType.TEAM);
        if (CollectionUtils.isEmpty(teamOrgIds)) {
            return;
        }
        Set<Long> connectionIds = connectionConfigRepository.findIdsByOrganizationIdIn(teamOrgIds);
        if (CollectionUtils.isEmpty(connectionIds)) {
            return;
        }
        List<Database> databases = databaseService.listDatabasesByConnectionIds(connectionIds);
        databases.removeIf(e -> Boolean.FALSE.equals(e.getExisted())
                || e.getObjectSyncStatus() == DBObjectSyncStatus.PENDING);
        try {
            dbSchemaSyncTaskManager.submitDBSchemaSyncTask(databases);
        } catch (Exception e) {
            log.warn("Submit database schema sync task failed", e);
        }
    }

}
