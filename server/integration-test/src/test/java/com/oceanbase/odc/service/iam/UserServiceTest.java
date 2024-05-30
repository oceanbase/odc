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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.MockedAuthorityTestEnv;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RoleRepository;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.common.response.PaginatedData;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.ChangePasswordReq;
import com.oceanbase.odc.service.iam.model.CreateUserReq;
import com.oceanbase.odc.service.iam.model.QueryUserParams;
import com.oceanbase.odc.service.iam.model.UpdateUserReq;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * @author wenniu.ly
 * @date 2021/7/18
 */
public class UserServiceTest extends MockedAuthorityTestEnv {
    private static final String ACCOUNT_NAME_PREFIX = "odc_user_account_";
    private final List<Long> DEFAULT_ROLE_IDS = new ArrayList<>();
    private static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, Integer.MAX_VALUE, Direction.DESC, "id");

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private RoleRepository roleRepository;
    @MockBean
    private AuthorizationFacade authorizationFacade;
    @MockBean
    private ProjectService projectService;
    @MockBean
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() {
        userRepository.deleteAll();
        userRoleRepository.deleteAll();
        roleRepository.deleteAll();
        Mockito.when(authorizationFacade.isImpliesPermissions(Mockito.any(), Mockito.anyCollection())).thenReturn(true);
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(ADMIN_USER_ID);
        Map<SecurityResource, Set<String>> returnValue = new HashMap<>();
        returnValue.put(new DefaultSecurityResource(ResourceType.ODC_USER.name()), Collections.singleton("*"));
        Mockito.when(authorizationFacade.getRelatedResourcesAndActions(Mockito.any())).thenReturn(returnValue);
        grantAllPermissions(ResourceType.ODC_USER, ResourceType.ODC_ROLE);
        initRole();
    }

    @After
    public void tearDown() {
        SecurityContextUtils.clear();
    }

    @Test
    public void testCreateUserNotExists() {
        RoleEntity roleEntity1 = createRoleEntity("role1", ORGANIZATION_ID);
        roleRepository.saveAndFlush(roleEntity1);
        RoleEntity roleEntity2 = createRoleEntity("roleEntity2", ORGANIZATION_ID);
        roleRepository.saveAndFlush(roleEntity2);
        UserEntity userEntity = UserEntity.autoCreatedEntity("accountName", "notExistsUser", ORGANIZATION_ID);
        User createdUser = userService.upsert(userEntity, Arrays.asList("role1", "roleEntity2"));
        Assert.assertNotNull(createdUser);
        Assert.assertEquals(2, createdUser.getRoles().size());
    }

    @Test
    public void testExistsByOrganizationIdAndAccountName() {
        CreateUserReq req = buildCreateUserReq("exists");
        User user = userService.create(req);
        Assert.assertTrue(userService.exists(ORGANIZATION_ID, ACCOUNT_NAME_PREFIX + "exists"));
        Assert.assertFalse(userService.exists(ORGANIZATION_ID, "not_exists"));
    }

    @Test
    public void testExistsByAccountName() {
        CreateUserReq req = buildCreateUserReq("exists");
        User user = userService.create(req);
        Assert.assertTrue(userService.exists(ACCOUNT_NAME_PREFIX + "exists"));
        Assert.assertFalse(userService.exists("not_Exists"));
    }

    @Test
    public void listByIds_EnabledRoles_ReturnEnabledRole() {
        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        RoleEntity roleEntity1 = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity1);

        List<RoleEntity> roleEntities =
                roleRepository.findByRoleIdsAndEnabled(Arrays.asList(roleEntity.getId(), roleEntity1.getId()), true);
        Assert.assertEquals(1, roleEntities.size());
        Assert.assertEquals(roleEntity1.getId(), roleEntities.get(0).getId());
    }

    @Test
    public void listByIds_DisabledRoles_ReturnDiabledRole() {
        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        RoleEntity roleEntity1 = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity1);

        List<RoleEntity> roleEntities =
                roleRepository.findByRoleIdsAndEnabled(Arrays.asList(roleEntity.getId(), roleEntity1.getId()), false);
        Assert.assertEquals(1, roleEntities.size());
        Assert.assertEquals(roleEntity.getId(), roleEntities.get(0).getId());
    }

    @Test
    public void listByIds_NullEnabledSetting_ReturnAllRoles() {
        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        RoleEntity roleEntity1 = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity1);

        List<RoleEntity> roleEntities =
                roleRepository.findByIdIn(Arrays.asList(roleEntity.getId(), roleEntity1.getId()));
        Assert.assertEquals(2, roleEntities.size());
        Set<Long> roleIds = roleEntities.stream().map(RoleEntity::getId).collect(Collectors.toSet());
        Assert.assertTrue(roleIds.contains(roleEntity.getId()));
        Assert.assertTrue(roleIds.contains(roleEntity1.getId()));
    }

    @Test
    public void listByUserIds_NotEmptyUserIds_ReturnNotEmpty() {
        UserRoleEntity relation = new UserRoleEntity();
        relation.setUserId(1L);
        relation.setRoleId(2L);
        relation.setOrganizationId(1L);
        relation.setCreatorId(1L);
        userRoleRepository.saveAndFlush(relation);

        UserRoleEntity relation1 = new UserRoleEntity();
        relation1.setUserId(1L);
        relation1.setRoleId(4L);
        relation1.setOrganizationId(1L);
        relation1.setCreatorId(1L);
        userRoleRepository.saveAndFlush(relation1);

        List<UserRoleEntity> relations =
                userRoleRepository.findByOrganizationIdAndUserIdIn(ORGANIZATION_ID,
                        Collections.singletonList(relation.getUserId()));
        Assert.assertEquals(2, relations.size());
        Set<Long> roleIds = relations.stream().map(UserRoleEntity::getRoleId).collect(Collectors.toSet());
        Assert.assertTrue(roleIds.contains(relation.getRoleId()));
        Assert.assertTrue(roleIds.contains(relation1.getRoleId()));
    }

    @Test
    public void create() {
        CreateUserReq req = buildCreateUserReq("create");
        User user = userService.create(req);
        Assert.assertNotNull(user);
    }

    @Test
    public void batchImport() {
        CreateUserReq req1 = buildCreateUserReq("create1");
        CreateUserReq req2 = buildCreateUserReq("create2");
        List<CreateUserReq> createUserReqs = new ArrayList<>();
        createUserReqs.add(req1);
        createUserReqs.add(req2);
        List<User> users = userService.batchImport(createUserReqs);
        for (User user : users) {
            Assert.assertNotNull(user);
        }
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchUserId_ReturnNotEmpty() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        List<UserEntity> userEntities =
                userRepository.findByUserIdsAndEnabled(Arrays.asList(userEntity.getId()), true);
        Assert.assertEquals(1, userEntities.size());
        Assert.assertEquals(userEntity.getId(), userEntities.get(0).getId());
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchRole_ReturnNotEmpty() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<UserEntity> userEntities =
                userRepository.findByRoleIdsAndEnabled(Collections.singletonList(roleEntity.getId()), true);
        Assert.assertEquals(1, userEntities.size());
        Assert.assertEquals(userEntity.getId(), userEntities.get(0).getId());
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchRoleDisableUser_ReturnEmpty() {
        UserEntity userEntity = createUserEntity();
        userEntity.setEnabled(false);
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<UserEntity> userEntities =
                userRepository.findByRoleIdsAndEnabled(Collections.singletonList(roleEntity.getId()), true);
        Assert.assertTrue(userEntities.isEmpty());
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchRoleDisableUser_ReturnNotEmpty() {
        UserEntity userEntity = createUserEntity();
        userEntity.setEnabled(false);
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<UserEntity> userEntities =
                userRepository.findByRoleIdsAndEnabled(Collections.singletonList(roleEntity.getId()), false);
        Assert.assertEquals(1, userEntities.size());
        Assert.assertEquals(userEntity.getId(), userEntities.get(0).getId());
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchRoleNullEnabledUser_ReturnNotEmpty() {
        UserEntity userEntity = createUserEntity();
        userEntity.setEnabled(false);
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<UserEntity> userEntities =
                userRepository.findByRoleIds(Collections.singletonList(roleEntity.getId()));
        Assert.assertEquals(1, userEntities.size());
        Assert.assertEquals(userEntity.getId(), userEntities.get(0).getId());
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchUserAndRole_ReturnNotEmpty() {
        UserEntity userEntity1 = createUserEntity();
        userRepository.saveAndFlush(userEntity1);

        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<UserEntity> userEntities =
                userRepository.findByUserIdsOrRoleIds(Collections.singletonList(userEntity1.getId()),
                        Collections.singletonList(roleEntity.getId()), true);
        Assert.assertEquals(2, userEntities.size());
    }

    @Test
    public void listByUserIdsAndRoleIds_MatchDisableUserAndRole_ReturnNotEmpty() {
        UserEntity userEntity1 = createUserEntity();
        userEntity1.setEnabled(false);
        userRepository.saveAndFlush(userEntity1);

        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<UserEntity> userEntities =
                userRepository.findByUserIdsOrRoleIds(Collections.singletonList(userEntity1.getId()),
                        Collections.singletonList(roleEntity.getId()), false);
        Assert.assertEquals(1, userEntities.size());
        Assert.assertEquals(userEntity1.getId(), userEntities.get(0).getId());
    }

    @Test
    public void listRolesByUserId_NoRelatedRole_ReturnEmpty() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndEnabled(userEntity.getId(), true);
        Assert.assertTrue(CollectionUtils.isEmpty(roleEntities));
    }

    @Test
    public void listRolesByUserId_ExistRelatedRole_ReturnRoleEntity() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndEnabled(userEntity.getId(), true);
        Assert.assertEquals(1, roleEntities.size());
        Assert.assertEquals(roleEntity.getId(), roleEntities.get(0).getId());
    }

    @Test
    public void listRolesByUserId_ExistRelatedDisabledRole_ReturnEmpty() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndEnabled(userEntity.getId(), true);
        Assert.assertTrue(roleEntities.isEmpty());
    }

    @Test
    public void listRolesByUserId_ExistRelatedDisabledRole_ReturnRoleEntity() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<RoleEntity> roleEntities = roleRepository.findByUserId(userEntity.getId());
        Assert.assertEquals(1, roleEntities.size());
        Assert.assertEquals(roleEntity.getId(), roleEntities.get(0).getId());
    }

    @Test
    public void listRolesByUserId_ExistDisabledRelatedRole_ReturnEmpty() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setEnabled(false);
        roleRepository.saveAndFlush(roleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);

        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndEnabled(userEntity.getId(), true);
        Assert.assertTrue(roleEntities.isEmpty());
    }

    @Test
    public void listRolesByUserId_ExistBothEnabledDisabledRelatedRole_ReturnEnableRole() {
        UserEntity userEntity = createUserEntity();
        userRepository.saveAndFlush(userEntity);

        RoleEntity disRoleEntity = createRoleEntity();
        disRoleEntity.setEnabled(false);
        roleRepository.saveAndFlush(disRoleEntity);

        RoleEntity enRoleEntity = createRoleEntity();
        roleRepository.saveAndFlush(enRoleEntity);

        UserRoleEntity relation = createUserRoleRelation(userEntity, enRoleEntity);
        userRoleRepository.saveAndFlush(relation);

        UserRoleEntity relation1 = createUserRoleRelation(userEntity, disRoleEntity);
        userRoleRepository.saveAndFlush(relation1);

        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndEnabled(userEntity.getId(), true);
        Assert.assertEquals(1, roleEntities.size());
        Assert.assertEquals(enRoleEntity.getId(), roleEntities.get(0).getId());
    }

    @Test
    public void listRolesByUserId_OnlyRoleExist_ReturnEmpty() {
        RoleEntity roleEntity = createRoleEntity();
        roleRepository.saveAndFlush(roleEntity);

        List<RoleEntity> roleEntities = roleRepository.findByUserIdAndEnabled(1L, true);
        Assert.assertTrue(roleEntities.isEmpty());
    }

    @Test(expected = BadRequestException.class)
    public void create_DuplicateAccountName() {
        CreateUserReq req = buildCreateUserReq("create");
        userService.create(req);
        userService.create(req);
    }

    @Test
    public void delete() {
        CreateUserReq req = buildCreateUserReq("delete");
        User user = userService.create(req);
        User deletedUser = userService.delete(user.getId());

        Assert.assertEquals(user.getId(), deletedUser.getId());
    }

    @Test
    public void detail() {
        CreateUserReq req = buildCreateUserReq("detail");
        User user = userService.create(req);
        userRoleRepository.findByUserId(user.getId());
        User detailUser = userService.detail(user.getId());

        Assert.assertTrue(detailUser.getName().startsWith("odc_user_"));
        Assert.assertEquals(3, detailUser.getRoleIds().size());
    }

    @Test
    public void exists_Exists_ReturnTrue() {
        CreateUserReq req = buildCreateUserReq("detail");
        User user = userService.create(req);

        boolean exists = userService.exists(user.getOrganizationId(), user.getAccountName());

        Assert.assertTrue(exists);
    }

    @Test
    public void exists_NotExists_ReturnFalse() {
        boolean exists = userService.exists(1L, "userAccountNameNotExists");

        Assert.assertFalse(exists);
    }

    @Test
    public void list_QueryAll() {
        batchCreate(3);

        QueryUserParams queryParams = QueryUserParams.builder().build();
        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(3, listUsers.getContents().size());
    }

    @Test
    public void list_QueryByRoleId_NotMatch() {
        batchCreate(1);

        QueryUserParams queryParams = QueryUserParams.builder().roleIds(Arrays.asList(2L)).build();
        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(0, listUsers.getContents().size());
    }

    @Test
    public void list_QueryByRoleId_Match() {
        batchCreate(1);
        QueryUserParams queryParams = QueryUserParams.builder().roleIds(DEFAULT_ROLE_IDS).build();
        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(1, listUsers.getContents().size());
    }

    @Test
    public void list_QueryByRoleIdEmpty_NotMatch() {
        batchCreate(1);

        QueryUserParams queryParams = QueryUserParams.builder().roleIds(Arrays.asList(0L)).build();
        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(0, listUsers.getContents().size());
    }

    @Test
    public void list_QueryByRoleIdEmpty_Match() {
        batchCreate(0, 1, Collections.emptyList());

        QueryUserParams queryParams = QueryUserParams.builder().roleIds(Arrays.asList(0L)).build();
        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(1, listUsers.getContents().size());
    }

    @Test
    public void list_QueryByRoleIdHybridEmpty_Match() {
        batchCreate(1);
        batchCreate(1, 3, Collections.emptyList());
        batchCreate(4, 3, DEFAULT_ROLE_IDS);

        QueryUserParams queryParams = QueryUserParams.builder().roleIds(DEFAULT_ROLE_IDS).build();
        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(4, listUsers.getContents().size());
    }

    @Test
    public void list_Filter() {
        List<CreateUserReq> requestList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            requestList.add(buildCreateUserReq("list_filter" + i));
        }
        CreateUserReq customUserRequest = buildCreateUserReq("custom");
        requestList.add(customUserRequest);
        List<User> createdUsers = userService.batchCreate(requestList);
        Assert.assertNotNull(createdUsers);

        QueryUserParams queryParams = QueryUserParams.builder()
                .names(Arrays.asList("odc_user_name"))
                .accountNames(Arrays.asList(ACCOUNT_NAME_PREFIX + "list_filter")).build();

        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);
        Assert.assertNotNull(listUsers);
        Assert.assertEquals(createdUsers.size(), listUsers.getContents().size());
    }

    @Test
    public void list_Page() {
        List<CreateUserReq> requestList = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            requestList.add(buildCreateUserReq("list_page" + i));
        }
        List<User> createdUsers = userService.batchCreate(requestList);
        Assert.assertNotNull(createdUsers);
        Pageable pageable = PageRequest.of(1, 2);

        PaginatedData<User> listUsers =
                userService.list(new QueryUserParams(), pageable);

        Assert.assertEquals(3, createdUsers.size());
        Assert.assertEquals(3, listUsers.getPage().getTotalElements().intValue());
        Assert.assertEquals(2, listUsers.getPage().getTotalPages().intValue());
        Assert.assertEquals(1, listUsers.getContents().size());
    }

    @Test
    public void list_WithAuthorizedResource() {
        CreateUserReq req = buildCreateUserReq("authorized_resource");
        User user = userService.create(req);

        Map<User, Set<String>> mockPermisstionResult = new HashMap<>();
        Set<String> permissionActions = new HashSet<String>() {
            {
                add("read");
                add("create");
                add("update");
                add("delete");
            }
        };
        mockPermisstionResult.put(user, permissionActions);
        Mockito.when(authorizationFacade.getRelatedUsersAndPermittedActions(ResourceType.ODC_CONNECTION, "10",
                PermissionType.PUBLIC_RESOURCE)).thenReturn(mockPermisstionResult);

        QueryUserParams queryParams = QueryUserParams.builder().authorizedResource("ODC_CONNECTION:10").build();

        PaginatedData<User> listUsers = userService.list(queryParams, DEFAULT_PAGEABLE);

        Assert.assertEquals(1, listUsers.getContents().size());
        Assert.assertEquals(ACCOUNT_NAME_PREFIX + "authorized_resource",
                listUsers.getContents().get(0).getAccountName());
        Assert.assertEquals(4, listUsers.getContents().get(0).getAuthorizedActions().size());
        Assert.assertTrue(listUsers.getContents().get(0).getAuthorizedActions().contains("delete"));
    }

    @Test
    public void update() {
        CreateUserReq req = buildCreateUserReq("update");
        User user = userService.create(req);

        UpdateUserReq updateUserReq = new UpdateUserReq();
        updateUserReq.setEnabled(false);
        updateUserReq.setName("change_name");
        updateUserReq.setDescription("change_description");
        updateUserReq.setRoleIds(DEFAULT_ROLE_IDS);
        User updatedUser = userService.update(user.getId(), updateUserReq);

        Assert.assertEquals("change_name", updatedUser.getName());
        Assert.assertEquals(3, updatedUser.getRoleIds().size());
    }

    @Test
    public void update_WithEmptyDescription() {
        CreateUserReq req = buildCreateUserReq("update");
        User user = userService.create(req);

        UpdateUserReq updateUserReq = new UpdateUserReq();
        updateUserReq.setEnabled(false);

        User updatedUser = userService.update(user.getId(), updateUserReq);

        Assert.assertFalse(updatedUser.isEnabled());
        Assert.assertNull(updatedUser.getDescription());
    }

    @Test
    public void setEnabled() {
        CreateUserReq preReq = buildCreateUserReq("admin");
        userService.create(preReq);

        CreateUserReq req = buildCreateUserReq("enable");
        User user = userService.create(req);

        User unabledUser = userService.setEnabled(user.getId(), false);

        Assert.assertFalse(unabledUser.isEnabled());
    }

    @Test
    public void changePassword() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        CreateUserReq req = buildCreateUserReq("change_password");
        User user = userService.create(req);

        SecurityContextUtils.setCurrentUser(user.getId(), ORGANIZATION_ID, "mock user");

        ChangePasswordReq request = new ChangePasswordReq();
        request.setUsername(user.getAccountName());
        request.setCurrentPassword("Ab123456");
        request.setNewPassword("Ab654321");

        Mockito.when(authenticationFacade.currentUserId()).thenReturn(user.getId());
        Mockito.when(authorizationFacade.isImpliesPermissions(Mockito.any(), Mockito.anyCollection())).thenReturn(true);

        User changePasswordUser = userService.changePassword(request);

        Assert.assertNotNull(changePasswordUser);
        Assert.assertTrue(passwordEncoder.matches(request.getNewPassword(), changePasswordUser.getPassword()));
    }

    @Test
    public void test_getCurrentUserRoleIds() {
        UserEntity userEntity = createUserEntity();
        userEntity.setOrganizationId(ORGANIZATION_ID);
        userEntity = userRepository.saveAndFlush(userEntity);
        RoleEntity roleEntity = createRoleEntity();
        roleEntity.setOrganizationId(ORGANIZATION_ID);
        roleRepository.saveAndFlush(roleEntity);
        UserRoleEntity relation = createUserRoleRelation(userEntity, roleEntity);
        userRoleRepository.saveAndFlush(relation);
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(userEntity.getId());
        Set<Long> roleIds = userService.getCurrentUserRoleIds();
        Assert.assertEquals(1, roleIds.size());
        Assert.assertEquals(roleEntity.getId(), roleIds.iterator().next());
    }

    private List<User> batchCreate(int count) {
        return batchCreate(0, count, DEFAULT_ROLE_IDS);
    }

    private List<User> batchCreate(int start, int count, List<Long> roleIds) {
        List<CreateUserReq> requestList = new ArrayList<>();
        for (int i = start; i < start + count; i++) {
            requestList.add(buildCreateUserReq("list" + i, roleIds));
        }
        return userService.batchCreate(requestList);
    }

    private CreateUserReq buildCreateUserReq(String accountName) {
        return buildCreateUserReq(accountName, DEFAULT_ROLE_IDS);
    }

    private CreateUserReq buildCreateUserReq(String accountName, List<Long> roleIds) {
        CreateUserReq createUserReq = new CreateUserReq();
        createUserReq.setName("odc_user_name");
        createUserReq.setAccountName(ACCOUNT_NAME_PREFIX + accountName);
        createUserReq.setPassword("Ab123456");
        createUserReq.setEnabled(true);
        createUserReq.setRoleIds(roleIds);
        createUserReq.setDescription("test user");
        return createUserReq;
    }

    private UserEntity createUserEntity() {
        UserEntity entity = TestRandom.nextObject(UserEntity.class);
        entity.setId(null);
        entity.setType(UserType.USER);
        entity.setActive(true);
        entity.setEnabled(true);
        return entity;
    }

    private RoleEntity createRoleEntity() {
        RoleEntity entity = TestRandom.nextObject(RoleEntity.class);
        entity.setId(null);
        entity.setEnabled(true);
        return entity;
    }

    private RoleEntity createRoleEntity(String roleName, Long organizationId) {
        RoleEntity entity = TestRandom.nextObject(RoleEntity.class);
        entity.setId(null);
        entity.setBuiltIn(false);
        entity.setType(RoleType.CUSTOM);
        entity.setEnabled(true);
        entity.setName(roleName);
        entity.setOrganizationId(organizationId);
        return entity;
    }

    private UserRoleEntity createUserRoleRelation(UserEntity userEntity, RoleEntity roleEntity) {
        UserRoleEntity relation = new UserRoleEntity();
        Validate.notNull(userEntity.getId());
        Validate.notNull(roleEntity.getId());
        relation.setUserId(userEntity.getId());
        relation.setRoleId(roleEntity.getId());
        relation.setOrganizationId(ORGANIZATION_ID);
        relation.setCreatorId(ADMIN_USER_ID);
        return relation;
    }

    private void initRole() {
        for (int i = 1; i <= 3; i++) {
            RoleEntity entity = createRoleEntity("test_role_" + i, ORGANIZATION_ID);
            DEFAULT_ROLE_IDS.add(roleRepository.saveAndFlush(entity).getId());
        }
    }

    private ProjectEntity getProjectEntity() {
        ProjectEntity project = new ProjectEntity();
        project.setId(1L);
        project.setBuiltin(false);
        project.setArchived(false);
        project.setOrganizationId(1L);
        project.setLastModifierId(1L);
        project.setCreatorId(1L);
        return project;
    }

}
