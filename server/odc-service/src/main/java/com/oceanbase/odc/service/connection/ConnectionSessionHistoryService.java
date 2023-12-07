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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryEntity;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryRepository;
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
    private ConnectionHistoryRepository repository;

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private ConnectSessionService connectSessionService;

    @Autowired
    private ConnectProperties connectProperties;

    public void refreshAllSessionHistory() {
        Collection<ConnectionSession> sessions = connectSessionService.listAllSessions();
        log.info("refresh all session history in db, size={}", sessions.size());
        for (ConnectionSession session : sessions) {
            ConnectionConfig conn = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
            Long connectionId = conn.getId();
            Long userId = ConnectionSessionUtil.getUserId(session);
            Date lastAccessTime = session.getLastAccessTime();
            if (Objects.nonNull(userId) && Objects.nonNull(lastAccessTime) && Objects.nonNull(connectionId)) {
                repository.updateOrInsert(connectionId, userId, lastAccessTime);
                log.debug("update or insert connection history successfully, connectionId={}", connectionId);
            }
        }
    }

    public List<ConnectionEntity> listInactiveConnections(Boolean temp) {
        int intervalSeconds = connectProperties.getTempExpireAfterInactiveIntervalSeconds();
        Date expireDate = new Date(System.currentTimeMillis() - intervalSeconds * 1000L);
        Set<Long> connIds = repository.findByLastAccessTimeAfter(expireDate).stream()
                .map(ConnectionHistoryEntity::getConnectionId).collect(Collectors.toSet());
        if (temp == null) {
            return connectionConfigRepository.findByUpdateTimeBefore(expireDate).stream()
                    .filter(conn -> !connIds.contains(conn.getId())).collect(Collectors.toList());
        }
        return connectionConfigRepository.findByUpdateTimeBeforeAndTemp(expireDate, temp).stream()
                .filter(conn -> !connIds.contains(conn.getId())).collect(Collectors.toList());
    }

}
