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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionHistoryRepository;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionConfigRecycleServiceTest extends ServiceTestEnv {
    private static final int INTERVAL_SECONDS = 10;
    private static final long USER_ID = 11L;
    private static final long ORGANIZATION_ID = 1L;
    @Autowired
    private ConnectionConfigRecycleService recycleService;
    @Autowired
    private ConnectionHistoryRepository connectionHistoryRepository;
    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;
    @MockBean
    private ConnectProperties connectProperties;
    @MockBean
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() throws Exception {
        connectionHistoryRepository.deleteAll();
        connectionConfigRepository.deleteAll();
        when(connectProperties.getTempExpireAfterInactiveIntervalSeconds()).thenReturn(INTERVAL_SECONDS);
        when(authenticationFacade.currentUserId()).thenReturn(USER_ID);
        when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
    }

    @Test
    public void clearInactiveTempConnectionConfigs_Empty_Return0() {
        int ret = recycleService.clearInactiveTempConnectionConfigs();
        Assert.assertEquals(0, ret);
    }

    @Test
    @Transactional
    public void clearInactiveTempConnectionConfigs_Has2Match1_Return1() {
        ConnectionEntity connection1 = createConnection();
        ConnectionEntity connection2 = createConnection();
        createHistory(connection1.getId(), 1);
        createHistory(connection2.getId(), 20);
        int ret = recycleService.clearInactiveTempConnectionConfigs();
        Assert.assertEquals(1, ret);
    }

    private void createHistory(Long connectionId, int intervalSeconds) {
        connectionHistoryRepository.updateOrInsert(connectionId, USER_ID,
                Date.from(Instant.now().minusSeconds(intervalSeconds)));
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
        entity.setTemp(true);
        entity.setOwnerId(USER_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        return connectionConfigRepository.saveAndFlush(entity);
    }
}
