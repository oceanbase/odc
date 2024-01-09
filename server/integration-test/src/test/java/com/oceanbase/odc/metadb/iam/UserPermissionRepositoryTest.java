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
package com.oceanbase.odc.metadb.iam;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;

/**
 * @author gaoda.xy
 * @date 2022/12/15 15:11
 */
public class UserPermissionRepositoryTest extends ServiceTestEnv {
    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private final static Long CREATOR_ID = 1L;
    private final static Long ORGANIZATION_ID = 1L;

    @Before
    public void setUp() {
        permissionRepository.deleteAll();
        userPermissionRepository.deleteAll();
    }

    @Test
    public void findByUserId() {
        UserPermissionEntity createdEntity = createUserPermission(1L, 1L);
        List<UserPermissionEntity> existsEntity = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(1, existsEntity.size());
        Assert.assertEquals(1L, (long) existsEntity.get(0).getUserId());
        List<UserPermissionEntity> notExistsEntity = userPermissionRepository.findByUserId(10L);
        Assert.assertEquals(0, notExistsEntity.size());
    }

    @Test
    public void findByPermissionId() {
        UserPermissionEntity createdEntity = createUserPermission(2L, 2L);
        List<UserPermissionEntity> existsEntity = userPermissionRepository.findByPermissionId(2L);
        Assert.assertEquals(1, existsEntity.size());
        Assert.assertEquals(2L, (long) existsEntity.get(0).getPermissionId());
        List<UserPermissionEntity> notExistsEntity = userPermissionRepository.findByPermissionId(10L);
        Assert.assertEquals(0, notExistsEntity.size());
    }

    @Test
    public void findByUserIds() {
        UserPermissionEntity createdEntity1 = createUserPermission(1L, 1L);
        UserPermissionEntity createdEntity2 = createUserPermission(1L, 2L);
        UserPermissionEntity createdEntity3 = createUserPermission(2L, 1L);
        List<UserPermissionEntity> existsEntities = userPermissionRepository.findByUserIdIn(Arrays.asList(1L));
        Assert.assertEquals(2, existsEntities.size());
        List<UserPermissionEntity> notExistsEntity = userPermissionRepository.findByUserIdIn(Arrays.asList(10L));
        Assert.assertEquals(0, notExistsEntity.size());
    }

    @Test
    public void findByPermissionIds() {
        UserPermissionEntity createdEntity1 = createUserPermission(1L, 1L);
        UserPermissionEntity createdEntity2 = createUserPermission(1L, 2L);
        UserPermissionEntity createdEntity3 = createUserPermission(2L, 1L);
        List<UserPermissionEntity> existsEntities = userPermissionRepository.findByPermissionIdIn(Arrays.asList(2L));
        Assert.assertEquals(1, existsEntities.size());
        List<UserPermissionEntity> notExistsEntity = userPermissionRepository.findByPermissionIdIn(Arrays.asList(10L));
        Assert.assertEquals(0, notExistsEntity.size());
    }

    @Test
    public void findByUserIdAndResourceIdentifier() {
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1001");
        PermissionEntity permission2 = createPermission("ODC_CONNECTION:1001");
        PermissionEntity permission3 = createPermission("ODC_CONNECTION:1002");
        createUserPermission(1L, permission1.getId());
        createUserPermission(1L, permission3.getId());
        List<UserPermissionEntity> foundUserPermissions = userPermissionRepository
                .findByUserIdAndResourceIdentifiers(1L, Arrays.asList("ODC_CONNECTION:1001", "ODC_CONNECTION:1002"));
        Assert.assertEquals(2, foundUserPermissions.size());
        List<UserPermissionEntity> notFoundUserPermissions =
                userPermissionRepository.findByUserIdAndResourceIdentifiers(1L, Arrays.asList("ODC_CONNECTION:1003"));
        Assert.assertEquals(0, notFoundUserPermissions.size());
    }

    @Test
    public void findByOrganizationIdAndResourceIdentifier() {
        PermissionEntity permission1 = createPermission("ODC_CONNECTION:1001");
        PermissionEntity permission2 = createPermission("ODC_CONNECTION:1001");
        PermissionEntity permission3 = createPermission("ODC_CONNECTION:1002");
        UserPermissionEntity userPermission1 = createUserPermission(1L, permission1.getId());
        UserPermissionEntity userPermission2 = createUserPermission(1L, permission2.getId());
        List<UserPermissionEntity> findUserPermissions = userPermissionRepository
                .findByOrganizationIdAndResourceIdentifier(ORGANIZATION_ID, "ODC_CONNECTION:1001");
        Assert.assertEquals(2, findUserPermissions.size());
        Assert.assertEquals(permission1.getId(), findUserPermissions.get(0).getPermissionId());
        List<UserPermissionEntity> findUserPermissionsEmpty =
                userPermissionRepository.findByOrganizationIdAndResourceIdentifier(2L, "ODC_CONNECTION:1001");
        Assert.assertEquals(0, findUserPermissionsEmpty.size());
    }

    @Test
    public void deleteByIds() {
        UserPermissionEntity createdEntity1 = createUserPermission(1L, 1L);
        UserPermissionEntity createdEntity2 = createUserPermission(1L, 2L);
        List<UserPermissionEntity> existsEntities = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(2, existsEntities.size());
        userPermissionRepository.deleteByIds(Arrays.asList(1024L, createdEntity1.getId()));
        existsEntities = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(1, existsEntities.size());
    }

    @Test
    public void deleteByUserIds() {
        UserPermissionEntity createdEntity1 = createUserPermission(1L, 1L);
        UserPermissionEntity createdEntity2 = createUserPermission(1L, 2L);
        List<UserPermissionEntity> existsEntities = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(2, existsEntities.size());
        userPermissionRepository.deleteByUserIds(Arrays.asList(1L));
        existsEntities = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(0, existsEntities.size());
    }

    @Test
    public void deleteByPermissionIds() {
        UserPermissionEntity createdEntity1 = createUserPermission(1L, 1L);
        UserPermissionEntity createdEntity2 = createUserPermission(1L, 2L);
        List<UserPermissionEntity> existsEntities = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(2, existsEntities.size());
        userPermissionRepository.deleteByPermissionIds(Arrays.asList(1L));
        existsEntities = userPermissionRepository.findByUserId(1L);
        Assert.assertEquals(1, existsEntities.size());
    }

    private UserPermissionEntity createUserPermission(Long userId, Long permissionId) {
        UserPermissionEntity entity = new UserPermissionEntity();
        entity.setUserId(userId);
        entity.setPermissionId(permissionId);
        entity.setCreatorId(CREATOR_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        return userPermissionRepository.saveAndFlush(entity);
    }

    private PermissionEntity createPermission(String resourceIdentifier) {
        PermissionEntity entity = new PermissionEntity();
        entity.setType(PermissionType.PUBLIC_RESOURCE);
        entity.setAction("connect");
        entity.setCreatorId(CREATOR_ID);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
        entity.setBuiltIn(false);
        entity.setResourceIdentifier(resourceIdentifier);
        return permissionRepository.saveAndFlush(entity);
    }
}
