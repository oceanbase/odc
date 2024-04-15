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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.service.connection.model.ConnectProperties;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.connection.ssl.ConnectionSSLAdaptor;

public class ConnectionTestingTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @InjectMocks
    private ConnectionTesting connectionTesting;
    @Mock
    private ConnectProperties connectProperties;
    @Mock
    private ConnectionSSLAdaptor sslAdaptor;
    @Mock
    private ConnectionAdapter environmentAdapter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Ignore("TODO: fix this test")
    public void test_Success_ReturnTrue() {
        TestConnectionReq req = createReq();

        ConnectionTestResult result = connectionTesting.test(req);

        Assert.assertTrue(result.isActive());
    }

    @Test
    public void test_PasswordSavedFalse_Exception() {
        TestConnectionReq req = createReq();
        req.setPassword(null);

        thrown.expect(BadArgumentException.class);
        thrown.expectMessage("password required for connection without password saved");

        connectionTesting.test(req);
    }

    private TestConnectionReq createReq() {
        ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);
        ConnectionConfig config = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(session);
        TestConnectionReq req = new TestConnectionReq();
        // Use cloud database for testing in GitHub
        req.setType(ConnectType.CLOUD_OB_MYSQL);
        req.setHost(config.getHost());
        req.setPort(config.getPort());
        req.setClusterName(config.getClusterName());
        req.setTenantName(config.getTenantName());
        req.setUsername(config.getUsername());
        req.setPassword(config.getPassword());
        return req;
    }
}
