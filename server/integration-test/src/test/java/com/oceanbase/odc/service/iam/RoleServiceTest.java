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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.CreateRoleReq;
import com.oceanbase.odc.service.iam.model.PermissionConfig;
import com.oceanbase.odc.service.iam.model.Role;
import com.oceanbase.odc.service.iam.model.UpdateRoleReq;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * @author wenniu.ly
 * @date 2021/7/18
 */
public class RoleServiceTest extends AuthorityTestEnv {
    private static final String ROLE_NAME_PREFIX = "odc_role_name_";

    @Autowired
    private RoleService roleService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    @MockBean
    private ResourceGroupRepository resourceGroupRepository;

    @MockBean
    private ConnectionConfigRepository connectionConfigRepository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // create a test user and role, and grant suitable permissions
        UserEntity currentUserEntity = grantAllPermissions(
                ResourceType.ODC_CONNECTION,
                ResourceType.ODC_RESOURCE_GROUP,
                ResourceType.ODC_USER,
                ResourceType.ODC_ROLE,
                ResourceType.ODC_PRIVATE_CONNECTION);
        User user = new User(currentUserEntity);
        when(authenticationFacade.currentUser()).thenReturn(user);
        when(authenticationFacade.currentUserId()).thenReturn(user.getId());
        when(authenticationFacade.currentUserIdStr()).thenReturn(user.getId().toString());
        when(authenticationFacade.currentOrganizationId()).thenReturn(user.getOrganizationId());
        when(resourceGroupRepository.findByIdIn(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(createResourceGroupEntity()));
        when(connectionConfigRepository.findByIdIn(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(createConnectionEntity()));
    }

    @After
    public void tearDown() {
        SecurityContextUtils.clear();
        deleteAll();
    }

    @Test
    public void testExists() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("exists");
        Role createdRole = roleService.create(createRoleReq);
        Assert.assertEquals(true, roleService.exists(createdRole.getName()));
    }

    @Test
    public void testCreate() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("create");
        Role createdRole = roleService.create(createRoleReq);
        Validate.notNull(createdRole);
    }

    @Test(expected = BadRequestException.class)
    public void testCreateDuplicate() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("create");
        Role createdRole = roleService.create(createRoleReq);
        roleService.create(createRoleReq);
    }

    // delete
    @Test
    public void testDelete() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("delete");
        Role createdRole = roleService.create(createRoleReq);
        Validate.notNull(createdRole);
        Role deletedRole = roleService.delete(createdRole.getId());
        Validate.notNull(deletedRole);
        Validate.isTrue(createdRole.getId().equals(deletedRole.getId()));
    }

    @Test
    public void testDetail() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("detail");
        Role createdRole = roleService.create(createRoleReq);
        Role detailRole = roleService.detail(createdRole.getId());
        Validate.notNull(detailRole);
        Validate.isTrue(detailRole.getName().equals("odc_role_name_detail"));
        Validate.notNull(detailRole.getResourceManagementPermissions());
        Validate.notNull(detailRole.getSystemOperationPermissions());
        Validate.isTrue(detailRole.getResourceManagementPermissions().size() == 3);
    }

    @Test
    public void testList() {
        List<Role> createdRoles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Role createdRole = roleService.create(buildCreateRoleReq("list" + i));
            createdRoles.add(createdRole);
        }
        Page<Role> listRoles = roleService.list(null);
        Validate.notNull(listRoles);
        Validate.isTrue(listRoles.getContent().size() == (createdRoles.size() + 1));
    }

    @Test
    public void queryByRoleIds_RightRoleIds_ReturnNotEmpty() {
        RolePermissionEntity relation = TestRandom.nextObject(RolePermissionEntity.class);
        rolePermissionRepository.saveAndFlush(relation);
        List<RolePermissionEntity> relations =
                rolePermissionRepository.findByRoleIds(Collections.singletonList(relation.getRoleId()));
        Assert.assertEquals(1, relations.size());
        Assert.assertEquals(relation.getPermissionId(), relations.get(0).getPermissionId());
    }

    @Test
    public void queryByRoleIds_WrongRoleIds_ReturnEmpty() {
        RolePermissionEntity relation = TestRandom.nextObject(RolePermissionEntity.class);
        relation.setRoleId(10001L);
        rolePermissionRepository.saveAndFlush(relation);
        List<RolePermissionEntity> relations =
                rolePermissionRepository.findByRoleIds(Collections.singletonList(relation.getRoleId() + 1));
        Assert.assertTrue(relations.isEmpty());
    }

    @Test
    public void testListWithPage() {
        List<Role> createdRoles = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Role createdRole = roleService.create(buildCreateRoleReq("list_page" + i));
            createdRoles.add(createdRole);
        }
        Pageable pageable = PageRequest.of(1, 2, Direction.ASC, "id");
        Page<Role> listRoles = roleService.list(pageable);
        Validate.notNull(listRoles);
        Validate.notNull(listRoles.get());
        Validate.isTrue(listRoles.getContent().size() == 2);
        Validate.isTrue(listRoles.getContent().get(0).getName().endsWith("list_page1"));
        Validate.isTrue(listRoles.getTotalElements() == 4);
        Validate.isTrue(listRoles.getTotalPages() == 2);
        Validate.isTrue(listRoles.getNumber() == 1);
    }

    // update
    @Test
    public void testUpdate() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("update");
        Role createdRole = roleService.create(createRoleReq);
        UpdateRoleReq updateRoleRequest = new UpdateRoleReq();
        updateRoleRequest.setName("update_role_name");
        updateRoleRequest.setEnabled(false);
        updateRoleRequest.setDescription("test update role");

        Role updatedRole = roleService.update(createdRole.getId(), updateRoleRequest);
        Validate.notNull(updatedRole);
        Validate.isTrue(updatedRole.getName().equals("update_role_name"));
        Validate.isTrue(!updatedRole.getEnabled());
        Validate.isTrue(updatedRole.getDescription().equals("test update role"));
    }

    @Test
    public void testUpdateWithBindAndUnbindUsers_success() {
        UserEntity user1 = createUser("user1", "user1");
        UserEntity user2 = createUser("user2", "user2");
        CreateRoleReq createRoleReq = buildCreateRoleReq("related_user");
        Role createdRole = roleService.create(createRoleReq);
        UserRoleEntity userRoleRelation = new UserRoleEntity();
        userRoleRelation.setUserId(user1.getId());
        userRoleRelation.setOrganizationId(user1.getOrganizationId());
        userRoleRelation.setCreatorId(authenticationFacade.currentUserId());
        userRoleRelation.setRoleId(createdRole.getId());
        userRoleRepository.saveAndFlush(userRoleRelation);

        UpdateRoleReq updateRoleRequest = new UpdateRoleReq();
        updateRoleRequest.setName("update_role_name");
        updateRoleRequest.setEnabled(false);
        updateRoleRequest.setDescription("test update role");
        updateRoleRequest.setUnbindUserIds(Arrays.asList(user1.getId()));
        updateRoleRequest.setBindUserIds(Arrays.asList(user2.getId()));

        Role updatedRole = roleService.update(createdRole.getId(), updateRoleRequest);
        Validate.notNull(updatedRole);
        Validate.isTrue(updatedRole.getName().equals("update_role_name"));
        Validate.isTrue(!updatedRole.getEnabled());
        Validate.isTrue(updatedRole.getDescription().equals("test update role"));
    }

    @Test
    public void testUpdateWithBindAndUnbindUsers_failed() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("related_user");
        Role createdRole = roleService.create(createRoleReq);

        UpdateRoleReq updateRoleRequest = new UpdateRoleReq();
        updateRoleRequest.setName("update_role_name");
        updateRoleRequest.setBindUserIds(Arrays.asList(1L, 2L, 3L));

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("1,2,3");
        Role updatedRole = roleService.update(createdRole.getId(), updateRoleRequest);
    }

    @Test
    public void testEnabled() {
        CreateRoleReq createRoleReq = buildCreateRoleReq("enable");
        Role createdRole = roleService.create(createRoleReq);
        Role disabledRole = roleService.setEnabled(createdRole.getId(), false);
        Validate.isTrue(!disabledRole.getEnabled());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void test_BindUserRole_Internal() {
        Role role = roleService.create(buildCreateRoleReq("role"));
        UserEntity userEntity = new UserEntity();
        userEntity.setName("user");
        userEntity.setAccountName("test");
        userEntity.setPassword("123123");
        userEntity.setCreatorId(1L);
        userEntity.setEnabled(true);
        userEntity.setBuiltIn(false);
        userEntity.setType(UserType.USER);
        userEntity.setOrganizationId(1L);
        userEntity.setCipher(Cipher.RAW);
        userEntity.setActive(true);
        UserEntity user = userRepository.saveAndFlush(userEntity);
        UserRoleEntity userRoleEntity = roleService.bindUserRole(user.getId(), role.getId(), 1L, null);
        Assert.assertEquals(role.getId(), userRoleEntity.getRoleId());
        userEntity.setOrganizationId(2L);
        UserEntity user1 = userRepository.saveAndFlush(userEntity);
        roleService.bindUserRole(user1.getId(), role.getId(), 1L, null);
    }

    @Test
    public void test_BatchNullSafeGet_Success() {
        Role role = roleService.create(buildCreateRoleReq("role"));
        List<RoleEntity> roleEntities = roleService.batchNullSafeGet(Collections.singleton(role.getId()));
        Assert.assertEquals(1, roleEntities.size());
    }

    @Test
    public void test_BatchNullSafeGet_Failed() {
        thrown.expect(NotFoundException.class);
        thrown.expectMessage("-1,-2");
        Role role = roleService.create(buildCreateRoleReq("role"));
        List<RoleEntity> roleEntities = roleService.batchNullSafeGet(Arrays.asList(role.getId(), -1L, -2L));
    }

    @Test
    public void test_filteringExistsPermissions_containsALL_emptyList() {
        Role role = roleService.create(buildCreateRoleReq("test_filteringExistsPermissions"));
        UpdateRoleReq updateRoleReq = new UpdateRoleReq();
        updateRoleReq.setResourceManagementPermissions(role.getResourceManagementPermissions());
        updateRoleReq.setSystemOperationPermissions(role.getSystemOperationPermissions());
        CreateRoleReq createRoleReq = roleService.filteringExistsPermissions(role.getId(), updateRoleReq);

        Assert.assertTrue(CollectionUtils.isEmpty(createRoleReq.getResourceManagementPermissions()));
        Assert.assertTrue(CollectionUtils.isEmpty(createRoleReq.getSystemOperationPermissions()));
    }

    private CreateRoleReq buildCreateRoleReq(String name) {
        CreateRoleReq createRoleReq = new CreateRoleReq();
        createRoleReq.setName(ROLE_NAME_PREFIX + name);
        createRoleReq.setEnabled(true);
        createRoleReq.setDescription("test role");

        ResourceGroupEntity resourceGroup = new ResourceGroupEntity();
        resourceGroup.setId(2L);
        Mockito.when(resourceGroupRepository.findById(any())).thenReturn(Optional.of(resourceGroup));
        ResourceGroupConnectionRepository resourceGroupConnectionRepository =
                Mockito.mock(ResourceGroupConnectionRepository.class);
        when(resourceGroupConnectionRepository.findAll((Specification) any())).thenReturn(new ArrayList<>());
        when(resourceGroupRepository.existsById(any())).thenReturn(true);

        PermissionConfig roleManagementPermission =
                new PermissionConfig(null, ResourceType.ODC_ROLE, Arrays.asList("read", "update", "delete"));
        PermissionConfig roleManagementPermissionCreate =
                new PermissionConfig(null, ResourceType.ODC_ROLE, Arrays.asList("create"));
        PermissionConfig userManagementPermission =
                new PermissionConfig(null, ResourceType.ODC_USER, Arrays.asList("read", "update", "delete"));
        createRoleReq.setResourceManagementPermissions(Arrays.asList(roleManagementPermission,
                roleManagementPermissionCreate, userManagementPermission));
        return createRoleReq;
    }

    private ResourceGroupEntity createResourceGroupEntity() {
        ResourceGroupEntity entity = new ResourceGroupEntity();
        entity.setId(2L);
        entity.setOrganizationId(ORGANIZATION_ID);
        return entity;
    }

    private ConnectionEntity createConnectionEntity() {
        ConnectionEntity entity = new ConnectionEntity();
        entity.setId(2L);
        entity.setOrganizationId(ORGANIZATION_ID);
        return entity;
    }

}
