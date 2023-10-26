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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryDAO;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryEntity;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.session.ConnectSessionService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/1/4 上午11:22
 * @Description: [Service for update connect session history]
 */
@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class ConnectionSessionHistoryService {

    @Autowired
    private ConnectionHistoryDAO connectionHistoryDAO;

    @Autowired
    private ConnectSessionService connectSessionService;

    @Autowired
    private ConnectProperties connectProperties;

    public void refreshAllSessionHistory() {
        Collection<ConnectionSession> sessions = connectSessionService.listAllSessions();
        log.info("refresh all session history in db, size={}", sessions.size());
        for (ConnectionSession session : sessions) {
            if (session.isExpired()) {
                continue;
            }
            Long userId = ConnectionSessionUtil.getUserId(session);
            Date lastAccessTime = session.getLastAccessTime();
            Object connectionConfig = ConnectionSessionUtil.getConnectionConfig(session);
            if (Objects.nonNull(userId) && Objects.nonNull(lastAccessTime) && Objects.nonNull(connectionConfig)) {
                updateOrInsert(((ConnectionConfig) connectionConfig).getId(), userId, lastAccessTime);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateOrInsert(Long connectionId, Long userId, Date lastAccessTime) {
        connectionHistoryDAO.updateOrInsert(ConnectionHistoryEntity.of(connectionId, userId, lastAccessTime));
        log.debug("update or insert connection history successfully, connectionId={}", connectionId);
    }

    public List<ConnectionHistoryEntity> listAll() {
        return connectionHistoryDAO.listAll();
    }

    public List<ConnectionHistoryEntity> listInactiveConnections() {
        return connectionHistoryDAO
                .listInactiveConnections(connectProperties.getTempExpireAfterInactiveIntervalSeconds());
    }
}
