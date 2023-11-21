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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.core.authority.DefaultLoginSecurityManager;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.common.util.EmptyValues;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.QueryConnectionParams;
import com.oceanbase.odc.service.connection.ssl.ConnectionSSLAdaptor;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.UserPermissionService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.test.tool.TestRandom;

public class ConnectionConfigServiceTest extends MockedAuthorityTestEnv {

    private static final Long CREATOR_ID = 1L;
    private static final Long ORGANIZATION_ID = 1L;
    private static final String NAME = "TEST_C1";
    private static final String TENANT_NAME = "T1";
    private static final String PASSWORD = "pswd";
    private static final String SALT = "salt";
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private ConnectionService service;
    @Autowired
    private ConnectionConfigRepository repository;
    @Autowired
    private ResourceGroupRepository resourceGroupRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private UserService userService;
    @MockBean
    private EncryptionFacade encryptionFacade;
    @MockBean
    private AuthorizationFacade authorizationFacade;
    @MockBean
    private ConnectionSSLAdaptor sslAdaptor;
    @MockBean
    private EnvironmentService environmentService;
    @MockBean
    private UserPermissionService userPermissionService;

    @Before
    public void setUp() throws Exception {
        when(authenticationFacade.currentUser()).thenReturn(User.of(CREATOR_ID));
        when(authenticationFacade.currentUserId()).thenReturn(CREATOR_ID);
        when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(environmentService.exists(anyLong())).thenReturn(true);

        UserEntity userEntity = new UserEntity();
        userEntity.setType(UserType.USER);
        userEntity.setId(CREATOR_ID);
        userEntity.setPassword(PASSWORD);
        when(userService.nullSafeGet(eq(CREATOR_ID))).thenReturn(userEntity);

        TextEncryptor mockEncryptor = mock(TextEncryptor.class);
        when(mockEncryptor.decrypt(anyString())).thenReturn(PASSWORD);
        when(mockEncryptor.encrypt(anyString())).thenReturn("encrypted-password");
        when(encryptionFacade.generateSalt()).thenReturn(SALT);
        when(encryptionFacade.userEncryptor(anyLong(), anyString())).thenReturn(mockEncryptor);
        when(encryptionFacade.passwordEncryptor(anyString(), anyString())).thenReturn(mockEncryptor);
        when(encryptionFacade.userEncryptor(eq(CREATOR_ID), eq(SALT))).thenReturn(mockEncryptor);
        when(encryptionFacade.organizationEncryptor(eq(ORGANIZATION_ID), eq(SALT))).thenReturn(mockEncryptor);
        Mockito.when(environmentService.detailSkipPermissionCheck(Mockito.anyLong())).thenReturn(getEnvironment());
        Mockito.when(environmentService.list(Mockito.anyLong()))
                .thenReturn(Collections.singletonList(getEnvironment()));
        doNothing().when(userPermissionService).bindUserAndDataSourcePermission(eq(CREATOR_ID), eq(ORGANIZATION_ID),
                any(Long.class), eq(Arrays.asList("read", "update", "delete")));


        repository.deleteAll();
        resourceGroupRepository.deleteAll();

        grantAllPermissions(ResourceType.ODC_CONNECTION, ResourceType.ODC_RESOURCE_GROUP, ResourceType.ODC_USER,
                ResourceType.ODC_PRIVATE_CONNECTION);
    }

    @After
    public void tearDown() {
        repository.deleteAll();
        DefaultLoginSecurityManager.removeSecurityContext();
        DefaultLoginSecurityManager.removeContext();
    }

    @Test
    public void exists_HasConnection_MatchTrue() {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        boolean exists = service.exists(connection.getName());
        Assert.assertTrue(exists);
    }

    @Test
    public void listOrganizationConnections_OneRecord_NoPermission_ResultSize_0() {
        createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);

        QueryConnectionParams params = QueryConnectionParams.builder()
                .minPrivilege(ConnectionService.DEFAULT_MIN_PRIVILEGE).build();
        Page<ConnectionConfig> page = service.list(params, Pageable.unpaged()).getPage();

        Assert.assertEquals(0, page.getSize());
    }

    @Test
    public void listOrganizationConnections_OneRecord_HasPermission_ResultSize_1() {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        mockHasPermission(connection);

        QueryConnectionParams params = QueryConnectionParams.builder()
                .minPrivilege(ConnectionService.DEFAULT_MIN_PRIVILEGE)
                .build();
        Page<ConnectionConfig> page = service.list(params, Pageable.unpaged()).getPage();

        Assert.assertEquals(1, page.getSize());
    }

    @Test
    public void listOrganizationConnections_OneRecord_GroupMatch_ResultSize_1() {
        ConnectionConfig connection = newConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        ConnectionConfig created = service.create(connection);
        mockHasPermission(created);

        QueryConnectionParams params = QueryConnectionParams.builder()
                .minPrivilege(ConnectionService.DEFAULT_MIN_PRIVILEGE)
                .build();
        Page<ConnectionConfig> page = service.list(params, Pageable.unpaged()).getPage();

        Assert.assertEquals(1, page.getSize());
    }

    @Test
    public void listOrganizationConnections_OneRecord_EmptyGroupMatch_ResultSize_1() {
        ConnectionConfig connection = newConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        ConnectionConfig created = service.create(connection);
        mockHasPermission(created);

        QueryConnectionParams params = QueryConnectionParams.builder()
                .minPrivilege(ConnectionService.DEFAULT_MIN_PRIVILEGE)
                .build();
        Page<ConnectionConfig> page = service.list(params, Pageable.unpaged()).getPage();

        Assert.assertEquals(1, page.getSize());
    }

    @Test
    public void listPrivateConnections_HasTenantNameByTenantNameEmpty_EmptyResult() {
        createConnection(ConnectionVisibleScope.PRIVATE, NAME);

        QueryConnectionParams params = QueryConnectionParams.builder()
                .tenantNames(Collections.singletonList(EmptyValues.EXPRESSION))
                .build();
        Page<ConnectionConfig> page = service.list(params, Pageable.unpaged()).getPage();

        Assert.assertEquals(0, page.getSize());
    }


    @Test
    public void innerList_NoRecord_EmptyResult() {
        QueryConnectionParams params = QueryConnectionParams.builder()
                .minPrivilege(ConnectionService.DEFAULT_MIN_PRIVILEGE)
                .build();

        Page<ConnectionConfig> page = service.list(params, Pageable.unpaged()).getPage();
        Assert.assertEquals(0, page.getSize());
    }


    @Test
    public void create_Success_CreateTimeNotNull() {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);

        Assert.assertNotNull(connection.getCreateTime());
    }

    @Test
    public void create_Exists_BadRequest() {
        createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);

        thrown.expectMessage("ODC_CONNECTION already exists by organizationId,name=1,TEST_C1");

        createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
    }

    @Test
    public void create_SavePasswordWithoutPassword_BadRequest() {
        ConnectionConfig connection = newConnection(ConnectionVisibleScope.PRIVATE, NAME);
        connection.setPasswordSaved(true);
        connection.setPassword(null);

        thrown.expectMessage("parameter connection.password may not be null");

        service.create(connection);
    }


    @Test(expected = NotFoundException.class)
    public void get_NotExists_NotFoundException() {
        service.getWithoutPermissionCheck(-1L);
    }

    @Test
    public void get_Exists_CreateTimeNotNull() {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.PRIVATE, NAME);

        ConnectionConfig get = service.getWithoutPermissionCheck(connection.getId());

        Assert.assertNotNull(get.getCreateTime());
    }

    @Test
    public void deleteOrganizationConnection_Exists_ReturnSameId() {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);

        ConnectionConfig deleted = service.delete(connection.getId());

        Assert.assertEquals(connection.getId(), deleted.getId());
    }

    @Test(expected = NotFoundException.class)
    public void deleteOrganizationConnection_NotExists_NotFoundException() {
        service.delete(-1L);
    }

    @Test(expected = NotFoundException.class)
    public void update_NotExists_NotFoundException() throws InterruptedException {
        ConnectionConfig connection = newConnection(ConnectionVisibleScope.PRIVATE, NAME);
        service.update(-1L, connection);
    }

    @Test
    public void update_SameNameExists_BadRequestException() throws InterruptedException {
        createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        ConnectionConfig connection = newConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        connection.setName("TEST_C2");
        ConnectionConfig connection2 = service.create(connection);
        connection2.setName(NAME);

        thrown.expect(BadRequestException.class);
        thrown.expectMessage("same datasource name exists");

        service.update(connection2.getId(), connection2);
    }

    @Test
    public void update_Success_CreateTimeNotNull() throws InterruptedException {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        connection.setCreateTime(null);
        connection.setUpdateTime(null);
        connection.setName("othername");

        ConnectionConfig updated = service.update(connection.getId(), connection);

        Assert.assertNotNull(updated.getCreateTime());
    }

    @Test
    public void update_SetEnabledFalse_EnabledFalse() throws InterruptedException {
        ConnectionConfig connection = createConnection(ConnectionVisibleScope.ORGANIZATION, NAME);
        connection.setCreateTime(null);
        connection.setUpdateTime(null);
        connection.setEnabled(false);

        ConnectionConfig updated = service.update(connection.getId(), connection);

        Assert.assertFalse(updated.getEnabled());
    }

    private void mockHasPermission(ConnectionConfig... connections) {
        Map<SecurityResource, Set<String>> resource2Actions = new HashMap<>();
        for (ConnectionConfig connection : connections) {
            DefaultSecurityResource resource =
                    new DefaultSecurityResource(connection.getId() + "", ResourceType.ODC_CONNECTION.name());
            Set<String> actions = new HashSet<>(Arrays.asList("connect", "read"));
            resource2Actions.putIfAbsent(resource, actions);
        }
        when(authorizationFacade.getRelatedResourcesAndActions(any(User.class))).thenReturn(resource2Actions);
    }

    private ConnectionConfig createConnection(ConnectionVisibleScope visibleScope, String name) {
        ConnectionConfig connection = newConnection(visibleScope, name);
        return service.create(connection);
    }


    private ConnectionConfig newConnection(ConnectionVisibleScope visibleScope, String name) {
        ConnectionConfig connection = TestRandom.nextObject(ConnectionConfig.class);
        connection.setId(null);
        connection.setType(ConnectType.OB_MYSQL);
        connection.setVisibleScope(visibleScope);
        connection.setEnabled(true);
        connection.setCreatorId(CREATOR_ID);
        connection.setOrganizationId(ORGANIZATION_ID);
        connection.setName(name);
        connection.setPasswordSaved(true);
        connection.setPassword(PASSWORD);
        connection.setPasswordEncrypted(null);
        connection.setSysTenantPassword(null);
        connection.setSysTenantPasswordEncrypted(null);
        connection.setReadonlyPassword(null);
        connection.setReadonlyPasswordEncrypted(null);
        connection.setCopyFromId(null);
        connection.setCreateTime(null);
        connection.setUpdateTime(null);
        connection.setLastAccessTime(null);
        connection.setCipher(null);
        connection.setSalt(null);
        connection.setClusterName(null);
        connection.setTenantName(TENANT_NAME);
        connection.setTemp(false);
        connection.setEnvironmentId(1L);
        connection.setProjectId(null);
        return connection;
    }

    private Environment getEnvironment() {
        Environment environment = new Environment();
        environment.setId(1L);
        environment.setName("fake_env");
        return environment;
    }
}
