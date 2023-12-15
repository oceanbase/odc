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
package com.oceanbase.odc.service.connection;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.service.connection.database.DatabaseSyncManager;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/6/12 20:55
 * @Description: []
 */
@Slf4j
@Component
public class DatabaseSyncSchedules {
    @Autowired
    private DatabaseSyncManager databaseSyncManager;

    @Autowired
    private ConnectionService connectionService;

    @Scheduled(fixedDelayString = "${odc.connect.database.sync.interval-millis:180000}")
    public void syncDatabases() {
        List<ConnectionConfig> orgDataSources =
                connectionService.listByVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        if (CollectionUtils.isEmpty(orgDataSources)) {
            return;
        }
        for (ConnectionConfig dataSource : orgDataSources) {
            try {
                databaseSyncManager.submitSyncDataSourceTask(dataSource);
                log.debug("submit sync datasource task successfully, connectionId={}", dataSource.getId());
            } catch (Exception ex) {
                log.warn("Submit sync datasource task failed, datasourceId={}", dataSource.getId(), ex);
            }
        }
    }

}
