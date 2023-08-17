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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;

public class ConnectionTestServiceTest {

    @InjectMocks
    private ConnectionTestService connectionTestService;
    @Mock
    private ConnectionService connectionService;
    @Mock
    private ConnectionTesting connectionTesting;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(connectionTesting.test(any(TestConnectionReq.class))).thenReturn(ConnectionTestResult.success(null));
    }

    @Test
    public void test_Success_ReturnTrue() {
        TestConnectionReq req = createReq();

        ConnectionTestResult result = connectionTestService.test(req);

        Assert.assertTrue(result.isActive());
    }

    @Test
    public void test_PasswordFromSaved_Success() {
        TestConnectionReq req = createReq();
        ConnectionConfig connection = new ConnectionConfig();
        connection.setPassword(req.getPassword());
        when(connectionService.getForConnect(eq(1L))).thenReturn(connection);

        req.setId(1L);
        req.setPassword(null);

        ConnectionTestResult result = connectionTestService.test(req);

        Assert.assertTrue(result.isActive());
    }

    private TestConnectionReq createReq() {
        ConnectionConfig config = TestConnectionUtil.getTestConnectionConfig(ConnectType.OB_MYSQL);
        TestConnectionReq req = new TestConnectionReq();
        req.setType(config.getType());
        req.setHost(config.getHost());
        req.setPort(config.getPort());
        req.setClusterName(config.getClusterName());
        req.setTenantName(config.getTenantName());
        req.setUsername(config.getUsername());
        req.setPassword(config.getPassword());
        return req;
    }
}
