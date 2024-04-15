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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.shared.constant.ConnectionStatus;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.service.common.SystemTimeService;
import com.oceanbase.odc.service.connection.ConnectionStatusManager.CheckState;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.connection.model.TestConnectionReq;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionStatusManagerTest extends ServiceTestEnv {

    @InjectMocks
    private ConnectionStatusManager statusManager;
    @Mock
    private ConnectionTesting connectionTesting;
    @Mock
    private ConnectionEncryption connectionEncryption;
    @Mock
    private AuthenticationFacade authenticationFacade;

    @Spy
    private SystemTimeService systemTimeService = new SystemTimeService();
    @Spy
    public ThreadPoolTaskExecutor statusCheckExecutor = statusCheckExecutor();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(connectionTesting.test(any(TestConnectionReq.class))).thenReturn(ConnectionTestResult.success(null));

        TextEncryptor mockEncryptor = mock(TextEncryptor.class);
        when(mockEncryptor.decrypt(anyString())).thenReturn("pwd");
        when(connectionEncryption.getEncryptor(any(ConnectionConfig.class))).thenReturn(mockEncryptor);

        statusManager.clear();
    }

    @Test
    public void getAndRefreshStatus_EnabledFalse_Return_DISABLED() {
        ConnectionConfig connection = newConnection();
        connection.setEnabled(false);
        CheckState checkState = statusManager.getAndRefreshStatus(connection);
        Assert.assertEquals(ConnectionStatus.DISABLED, checkState.getStatus());
    }

    @Test
    public void getAndRefreshStatus_FirstTime_Return_TESTING() {
        ConnectionConfig connection = newConnection();
        CheckState checkState = statusManager.getAndRefreshStatus(connection);
        Assert.assertEquals(ConnectionStatus.TESTING, checkState.getStatus());
    }

    @Test
    public void getAndRefreshStatus_SecondTime_Return_ACTIVE() throws InterruptedException {
        ConnectionConfig connection = newConnection();
        statusManager.getAndRefreshStatus(connection);

        TimeUnit.SECONDS.sleep(1L);

        CheckState checkState = statusManager.getAndRefreshStatus(connection);
        Assert.assertEquals(ConnectionStatus.ACTIVE, checkState.getStatus());
    }

    @Test
    public void getAndRefreshStatus_CannotConnectSecondTime_Return_INACTIVE() throws InterruptedException {
        when(connectionTesting.test(any(TestConnectionReq.class))).thenReturn(ConnectionTestResult.unknownError(null));

        ConnectionConfig connection = newConnection();
        statusManager.getAndRefreshStatus(connection);

        TimeUnit.SECONDS.sleep(1L);

        CheckState checkState = statusManager.getAndRefreshStatus(connection);
        Assert.assertEquals(ConnectionStatus.INACTIVE, checkState.getStatus());
    }

    private ConnectionConfig newConnection() {
        ConnectionConfig connection = TestRandom.nextObject(ConnectionConfig.class);
        connection.setId(1L);
        connection.setPassword(null);
        connection.setPasswordSaved(true);
        connection.setEnabled(true);
        connection.setPasswordEncrypted("encrypted-pswd");
        connection.setVisibleScope(ConnectionVisibleScope.PRIVATE);
        return connection;
    }

    private ThreadPoolTaskExecutor statusCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("test-status-check-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }
}
