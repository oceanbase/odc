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
import org.springframework.stereotype.Service;

import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryDAO;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryEntity;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 连接配置回收服务
 */
@Slf4j
@Service
class ConnectionConfigRecycleService {

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private ConnectionHistoryDAO connectionHistoryDAO;

    @Autowired
    private ConnectProperties connectProperties;

    @Autowired
    private ConnectionService connectionService;

    /**
     * 清理不活跃的临时连接配置
     * 
     * @return clear count
     */
    int clearInactiveTempConnectionConfigs() {
        int tempExpireAfterInactiveIntervalSeconds = connectProperties.getTempExpireAfterInactiveIntervalSeconds();
        List<ConnectionHistoryEntity> connectionHistoryEntities =
                connectionHistoryDAO.listInactiveTempConnections(tempExpireAfterInactiveIntervalSeconds);
        for (ConnectionHistoryEntity history : connectionHistoryEntities) {
            try {
                SecurityContextUtils.setCurrentUser(history.getUserId(), history.getOrganizationId(),
                        null);
                connectionService.delete(history.getConnectionId());
            } catch (Exception exception) {
                log.warn("Delete inactive temp connection failed, connectionId={}, reason={}",
                        history.getConnectionId(), exception.getMessage());
            } finally {
                SecurityContextUtils.clear();
            }
        }
        log.info("Clear inactive temp connection configs, deletedCount={}", connectionHistoryEntities.size());
        return connectionHistoryEntities.size();
    }

}
