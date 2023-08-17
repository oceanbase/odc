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
package com.oceanbase.odc.service.iam;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.BatchUpdateUserPermissionsReq;
import com.oceanbase.odc.service.iam.model.BatchUpdateUserPermissionsReq.UserAction;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.model.UserPermissionResp;

public class UserPermissionServiceTest extends AuthorityTestEnv {
    @Autowired
    private UserPermissionService userPermissionService;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private ConnectionService connectionService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private static final Long OTHER_ORGANIZATION_ID = 1001L;

    @Before
    public void setUp() {
        // clear related tables
        deleteAll();
        // create a test user and role, and grant suitable permissions
        UserEntity currentUserEntity = grantAllPermissions(ResourceType.ODC_CONNECTION);
        User user = new User(currentUserEntity);
        when(authenticationFacade.currentUser()).thenReturn(user);
        when(authenticationFacade.currentUserId()).thenReturn(user.getId());
        when(authenticationFacade.currentOrganizationId()).thenReturn(user.getOrganizationId());
    }

    @After
    public void clear() {
        deleteAll();
    }

    @Test
    public void test_listUserPermissions() {
        UserEntity user1 = createUser("user1", ORGANIZATION_ID);
        UserEntity user2 = createUser("user2", ORGANIZATION_ID);
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1", "apply");
        PermissionEntity permission2 = createPermission("ODC_CONNECTION:1", "connect");
        createUserPermission(user1.getId(), permission1.getId());
        createUserPermission(user2.getId(), permission2.getId());
        List<UserPermissionResp> respNotEmpty = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(2, respNotEmpty.size());
        List<UserPermissionResp> respEmpty = userPermissionService.list("ODC_CONNECTION:2");
        Assert.assertEquals(0, respEmpty.size());
    }

    @Test
    public void test_batchUpdateForConnection_Success() {
        ConnectionConfig config = new ConnectionConfig();
        config.setOrganizationId(ORGANIZATION_ID);
        when(connectionService.getWithoutPermissionCheck(anyLong())).thenReturn(config);
        UserEntity user1 = createUser("user1", ORGANIZATION_ID);
        UserEntity user2 = createUser("user2", ORGANIZATION_ID);
        UserEntity user3 = createUser("user3", ORGANIZATION_ID);
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1", "apply");
        PermissionEntity permission2 = createPermission("ODC_CONNECTION:1", "connect");
        createUserPermission(user1.getId(), permission1.getId());
        createUserPermission(user2.getId(), permission2.getId());
        List<UserPermissionResp> originalResp = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(2, originalResp.size());

        BatchUpdateUserPermissionsReq req = new BatchUpdateUserPermissionsReq();
        req.setResourceIdentifier("ODC_CONNECTION:1");
        req.setUserActions(Collections.singletonList(createUserActions(user3.getId(), "apply")));
        List<UserPermissionResp> updateResp = userPermissionService.batchUpdateForConnection(req);
        Assert.assertEquals(1, updateResp.size());
        List<UserPermissionResp> currentResp = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(1, currentResp.size());
        Assert.assertEquals("apply", currentResp.get(0).getAction());
    }

    @Test
    public void test_batchUpdateForConnection_HorizontalAuthoring_Connection() {
        ConnectionConfig config = new ConnectionConfig();
        config.setId(1L);
        config.setOrganizationId(OTHER_ORGANIZATION_ID);
        config.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        when(connectionService.getWithoutPermissionCheck(anyLong())).thenReturn(config);
        UserEntity user1 = createUser("user1", ORGANIZATION_ID);
        UserEntity user2 = createUser("user2", ORGANIZATION_ID);
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1", "apply");
        createUserPermission(user1.getId(), permission1.getId());
        List<UserPermissionResp> originalResp = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(1, originalResp.size());

        BatchUpdateUserPermissionsReq req = new BatchUpdateUserPermissionsReq();
        req.setResourceIdentifier("ODC_CONNECTION:1");
        req.setUserActions(Collections.singletonList(createUserActions(user2.getId(), "apply")));
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("%s not found by %s=%s", ResourceType.ODC_CONNECTION, "id", 1));
        List<UserPermissionResp> updateResp = userPermissionService.batchUpdateForConnection(req);
    }

    @Test
    public void test_batchUpdateForConnection_HorizontalAuthoring_User() {
        UserEntity user1 = createUser("user1", ORGANIZATION_ID);
        UserEntity user2 = createUser("user2", OTHER_ORGANIZATION_ID);
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1", "apply");
        createUserPermission(user1.getId(), permission1.getId());
        List<UserPermissionResp> originalResp = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(1, originalResp.size());

        BatchUpdateUserPermissionsReq req = new BatchUpdateUserPermissionsReq();
        req.setResourceIdentifier("ODC_CONNECTION:1");
        req.setUserActions(Collections.singletonList(createUserActions(user2.getId(), "apply")));
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("%s not found by %s=%s", ResourceType.ODC_USER, "id", user2.getId()));
        List<UserPermissionResp> updateResp = userPermissionService.batchUpdateForConnection(req);
    }

    @Test
    public void test_bindUserAndPermission_Internal() {
        UserEntity user = createUser("user", ORGANIZATION_ID);
        userPermissionService.bindUserAndConnectionAccessPermission(user.getId(), null, "connect", 1L);
        List<UserPermissionEntity> userPermission = userPermissionRepository.findByUserId(user.getId());
        Assert.assertEquals(1, userPermission.size());
    }

    @Test
    public void test_deleteExistsUserPermissions() {
        UserEntity user1 = createUser("user1", ORGANIZATION_ID);
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1", "apply");
        PermissionEntity permission2 = createPermission("ODC_CONNECTION:2", "connect");
        createUserPermission(user1.getId(), permission1.getId());
        createUserPermission(user1.getId(), permission2.getId());
        List<UserPermissionResp> resp1 = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(1, resp1.size());
        List<UserPermissionResp> resp2 = userPermissionService.list("ODC_CONNECTION:2");
        Assert.assertEquals(1, resp2.size());
        userPermissionService.deleteExistsUserPermissions(new User(user1), Collections.singletonList(permission1));
        List<UserPermissionResp> resp3 = userPermissionService.list("ODC_CONNECTION:1");
        Assert.assertEquals(0, resp3.size());
        List<UserPermissionResp> resp4 = userPermissionService.list("ODC_CONNECTION:2");
        Assert.assertEquals(1, resp4.size());
    }

    private UserEntity createUser(String name, Long organizationId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setName(name);
        userEntity.setAccountName(name);
        userEntity.setPassword("123456");
        userEntity.setCreatorId(ADMIN_USER_ID);
        userEntity.setEnabled(true);
        userEntity.setBuiltIn(false);
        userEntity.setType(UserType.USER);
        userEntity.setOrganizationId(organizationId);
        userEntity.setCipher(Cipher.RAW);
        userEntity.setActive(true);
        return userRepository.saveAndFlush(userEntity);
    }

    private PermissionEntity createPermission(String resourceIdentifier, String action) {
        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setAction(action);
        permissionEntity.setCreatorId(ADMIN_USER_ID);
        permissionEntity.setOrganizationId(ORGANIZATION_ID);
        permissionEntity.setResourceIdentifier(resourceIdentifier);
        permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
        permissionEntity.setBuiltIn(false);
        return permissionRepository.saveAndFlush(permissionEntity);
    }

    private UserPermissionEntity createUserPermission(Long userId, Long permissionId) {
        UserPermissionEntity userPermissionEntity = new UserPermissionEntity();
        userPermissionEntity.setUserId(userId);
        userPermissionEntity.setPermissionId(permissionId);
        userPermissionEntity.setCreatorId(ADMIN_USER_ID);
        userPermissionEntity.setOrganizationId(ORGANIZATION_ID);
        return userPermissionRepository.saveAndFlush(userPermissionEntity);
    }

    private UserAction createUserActions(Long userId, String action) {
        UserAction userAction = new UserAction();
        userAction.setUserId(userId);
        userAction.setAction(action);
        return userAction;
    }
}
