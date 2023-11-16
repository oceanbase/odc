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
package com.oceanbase.odc.service.session;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.config.UserConfigFacade;
import com.oceanbase.odc.service.config.model.UserConfig;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionTesting;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionTestResult;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.User;

/**
 * Test cases for {@link ConnectConsoleService}
 *
 * @author yh263208
 * @date 2021-11-20 22:24
 * @since ODC_release_3.2.2
 */
public class ConnectSessionServiceTest extends AuthorityTestEnv {

    private final Long connectionId = 1L;
    private long userId;
    private Long organizationId;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @MockBean
    private ConnectionService connectionService;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private AuthorizationFacade authorizationFacade;
    @MockBean
    private ConnectionTesting connectionTesting;
    @MockBean
    private UserConfigFacade userConfigFacade;
    @Autowired
    private ConnectSessionService sessionService;

    @Before
    public void setUp() throws Exception {
        UserEntity currentUserEntity = grantAllPermissions(ResourceType.ODC_CONNECTION);
        this.userId = currentUserEntity.getId();
        this.organizationId = currentUserEntity.getOrganizationId();
        UserConfig userConfig = new UserConfig();
        when(userConfigFacade.queryByCache(eq(userId))).thenReturn(userConfig);
        when(authenticationFacade.currentUserId()).thenReturn(userId);
        when(authenticationFacade.currentOrganizationId()).thenReturn(organizationId);
        User user = new User();
        user.setId(userId);
        user.setName("user1");
        when(authenticationFacade.currentUser()).thenReturn(user);
        when(authorizationFacade.getAllPermittedActions(Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(new HashSet<>(Collections.singletonList("*")));
        when(connectionTesting.test(Mockito.any(ConnectionConfig.class)))
                .thenReturn(ConnectionTestResult.success(ConnectType.OB_ORACLE));
    }

    @Test
    public void testCreateSession_sessionCreated() {
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(connectionId))
                .thenReturn(buildTestConnection(DialectType.OB_ORACLE));
        ConnectionSession connectionSession = sessionService.create(connectionId, null);

        Assert.assertNotNull(connectionSession.getId());
    }


    @Test
    public void testCreateSessionForPublicConnection_sessionCreated() {
        ConnectionConfig connectionConfig = buildTestConnection(DialectType.OB_ORACLE);
        connectionConfig.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);

        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(connectionId)).thenReturn(connectionConfig);
        ConnectionSession connectionSession = sessionService.create(connectionId, null);

        Assert.assertNotNull(connectionSession.getId());
    }

    @Test
    public void testCreateSessionForReadonlyPublicConnection_sessionCreated() {
        ConnectionConfig connectionConfig = buildTestConnection(DialectType.OB_ORACLE);
        connectionConfig.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        connectionConfig.setReadonlyUsername(connectionConfig.getUsername());
        connectionConfig.setReadonlyPassword(connectionConfig.getPassword());

        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(connectionId)).thenReturn(connectionConfig);
        when(authorizationFacade.getAllPermittedActions(Mockito.any(), Mockito.any(), Mockito.anyString()))
                .thenReturn(new HashSet<>(
                        Collections.singletonList("readonlyconnect")));
        ConnectionSession connectionSession = sessionService.create(connectionId, null);

        Assert.assertNotNull(connectionSession.getId());
    }

    @Test
    public void testCreateSessionWithSysyUser_sessionCreated() {
        ConnectionConfig connectionConfig = buildTestConnection(DialectType.OB_ORACLE);
        connectionConfig.setSysTenantUsername(null);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(connectionId)).thenReturn(connectionConfig);
        ConnectionSession connectionSession = sessionService.create(connectionId, null);

        thrown.expectMessage("Sys username can not be null");
        thrown.expect(NullPointerException.class);
        connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.SYS_DS_KEY).execute("select 1 from dual");
    }

    @Test
    public void testGetSession_sessionGetted() {
        ConnectionSession connectionSession = createSession();
        ConnectionSession gettedSession = sessionService.nullSafeGet(connectionSession.getId());

        Assert.assertEquals(connectionSession, gettedSession);
    }

    @Test
    public void testGetSession_nullGetted() {
        String sessionId = "12223";
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("ODC_SESSION not found by ID=" + sessionId);
        sessionService.nullSafeGet(sessionId);
    }

    @Test
    public void testGetSession_NotFoundExceptionGetted() {
        ConnectionSession connectionSession = createSession();
        when(authenticationFacade.currentUserId()).thenReturn(userId + 10);
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("ODC_SESSION not found by ID=" + connectionSession.getId());
        sessionService.nullSafeGet(connectionSession.getId());
    }

    @Test
    public void testCloseSession_SuccessClosed() {
        ConnectionSession connectionSession = createSession();
        sessionService.close(connectionSession.getId());
    }

    @Test
    public void testGetClosedSession_NullGetted() {
        ConnectionSession connectionSession = createSession();
        sessionService.close(connectionSession.getId());
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("ODC_SESSION not found by ID=" + connectionSession.getId());
        sessionService.nullSafeGet(connectionSession.getId());
    }

    private ConnectionSession createSession() {
        ConnectionConfig connectionConfig = buildTestConnection(DialectType.OB_ORACLE);
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(connectionId)).thenReturn(connectionConfig);
        return sessionService.create(connectionId, null);
    }


    private ConnectionConfig buildTestConnection(DialectType dialectType) {
        ConnectionConfig config = TestConnectionUtil.getTestConnectionConfig(ConnectType.from(dialectType));
        config.setOrganizationId(organizationId);
        return config;
    }

}
