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

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryDAO;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryEntity;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionSessionHistoryServiceTest extends ServiceTestEnv {
    private static final int INTERVAL_SECONDS = 15;
    private static final long USER_ID = 11L;
    private static final long ORGANIZATION_ID = 1L;
    @Autowired
    private ConnectionSessionHistoryService connectionSessionHistoryService;
    @Autowired
    private ConnectionHistoryDAO connectionHistoryDAO;
    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;
    @MockBean
    private ConnectProperties connectProperties;

    @Before
    public void setUp() throws Exception {
        connectionHistoryDAO.deleteAll();
        connectionConfigRepository.deleteAll();
        when(connectProperties.getTempExpireAfterInactiveIntervalSeconds()).thenReturn(INTERVAL_SECONDS);
    }

    @After
    public void tearDown() {
        connectionHistoryDAO.deleteAll();
        connectionConfigRepository.deleteAll();
    }

    @Test
    public void testListInactiveConnections_Has2Matches1_Return1() {
        ConnectionEntity connection1 = createConnection();
        ConnectionEntity connection2 = createConnection();
        createHistory(connection1.getId(), 10);
        createHistory(connection2.getId(), 20);
        List<ConnectionHistoryEntity> historyEntities = connectionSessionHistoryService.listInactiveConnections();
        Assert.assertEquals(1, historyEntities.size());
        connectionHistoryDAO.deleteAll();
        connectionConfigRepository.deleteAll();
    }

    @Test
    public void testListAllConnections_MultiSessions_Return1() {
        createHistory(1L, new Date(30));
        createHistory(1L, new Date(20));
        createHistory(1L, new Date(10));
        List<ConnectionHistoryEntity> all = connectionSessionHistoryService.listAll();
        Assert.assertEquals(1, all.size());
        Assert.assertEquals(new Date(30), all.get(0).getLastAccessTime());
        connectionHistoryDAO.deleteAll();
    }

    private void createHistory(Long connectionId, Date date) {
        ConnectionHistoryEntity history = new ConnectionHistoryEntity();
        history.setConnectionId(connectionId);
        history.setUserId(USER_ID);
        history.setLastAccessTime(date);
        connectionHistoryDAO.updateOrInsert(history);
    }

    private void createHistory(Long connectionId, int intervalSeconds) {
        ConnectionHistoryEntity history = new ConnectionHistoryEntity();
        history.setConnectionId(connectionId);
        history.setUserId(USER_ID);
        history.setLastAccessTime(Date.from(Instant.now().minusSeconds(intervalSeconds)));
        connectionHistoryDAO.updateOrInsert(history);
    }

    private ConnectionEntity createConnection() {
        ConnectionEntity entity = TestRandom.nextObject(ConnectionEntity.class);
        entity.setId(null);
        entity.setEnabled(true);
        entity.setVisibleScope(ConnectionVisibleScope.PRIVATE);
        entity.setCreatorId(USER_ID);
        entity.setOrganizationId(1L);
        entity.setType(ConnectType.OB_MYSQL);
        entity.setDialectType(DialectType.OB_MYSQL);
        entity.setCreateTime(null);
        entity.setUpdateTime(null);
        entity.setOwnerId(USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        return connectionConfigRepository.saveAndFlush(entity);
    }
}
