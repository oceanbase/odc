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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.service.iam.util.ResourceContextUtil;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * @author wenniu.ly
 * @date 2021/8/10
 */
public class PermissionServiceTest extends AuthorityTestEnv {

    @Autowired
    private PermissionService permissionService;

    @Test
    public void testDeleteResourceRelatedPermissions() {
        permissionRepository.saveAndFlush(buildPermission());
        List<PermissionEntity> deletedPermissions =
                permissionService.deleteResourceRelatedPermissions(10L, ResourceType.ODC_RESOURCE_GROUP,
                        PermissionType.PUBLIC_RESOURCE);
        Validate.notNull(deletedPermissions);
        Validate.isTrue(deletedPermissions.size() == 1);
        Validate.isTrue(PermissionType.PUBLIC_RESOURCE == deletedPermissions.get(0).getType());
        Validate.isTrue("connect".equals(deletedPermissions.get(0).getAction()));
        Validate.isTrue(ResourceContextUtil.generateResourceIdentifierString(10L, ResourceType.ODC_RESOURCE_GROUP)
                .equals(deletedPermissions.get(0).getResourceIdentifier()));
    }

    @Test
    public void deleteResourceRelatedPermissions_permissionExists_deleteSucceed() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        PermissionEntity e1 = buildPermission();
        e1.setResourceIdentifier(
                ResourceContextUtil.generateResourceIdentifierString(ids.get(0), ResourceType.ODC_CONNECTION));
        PermissionEntity e2 = buildPermission();
        e2.setResourceIdentifier(
                ResourceContextUtil.generateResourceIdentifierString(ids.get(1), ResourceType.ODC_CONNECTION));
        PermissionEntity e3 = buildPermission();
        e3.setResourceIdentifier(
                ResourceContextUtil.generateResourceIdentifierString(ids.get(2), ResourceType.ODC_CONNECTION));

        List<PermissionEntity> entities = this.permissionRepository.saveAll(Arrays.asList(e1, e2, e3));

        List<RolePermissionEntity> entities1 = entities.stream().map(this::buildRolePermission)
                .collect(Collectors.toList());
        this.rolePermissionRepository.saveAll(entities1);

        List<PermissionEntity> deletedPermissions =
                permissionService.deleteResourceRelatedPermissions(new HashSet<>(ids), ResourceType.ODC_CONNECTION);
        Assert.assertTrue(deletedPermissions.containsAll(entities));
    }

    @Test
    public void queryByIds_EmptyIds_ReturnEmpty() {
        PermissionEntity permissionEntity = buildPermission();

        permissionRepository.saveAndFlush(permissionEntity);
        List<PermissionEntity> entities = permissionRepository.findByIdIn(Collections.emptyList());
        Assert.assertTrue(entities.isEmpty());
    }

    @Test
    public void queryByIds_RightIds_ReturnNotEmpty() {
        PermissionEntity permissionEntity = buildPermission();

        permissionRepository.saveAndFlush(permissionEntity);
        List<PermissionEntity> entities =
                permissionRepository.findByIdIn(Collections.singleton(permissionEntity.getId()));
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(permissionEntity.getId(), entities.get(0).getId());
    }

    @Test
    public void queryByIds_WrongIds_ReturnEmpty() {
        PermissionEntity permissionEntity = buildPermission();

        permissionRepository.saveAndFlush(permissionEntity);
        List<PermissionEntity> entities =
                permissionRepository.findByIdIn(Collections.singleton(permissionEntity.getId() + 1));
        Assert.assertTrue(entities.isEmpty());
    }

    private PermissionEntity buildPermission() {
        PermissionEntity permissionEntity = new PermissionEntity();
        permissionEntity.setCreatorId(ADMIN_USER_ID);
        permissionEntity.setAction("connect");
        permissionEntity.setType(PermissionType.PUBLIC_RESOURCE);
        permissionEntity.setOrganizationId(ORGANIZATION_ID);
        permissionEntity.setResourceIdentifier(
                ResourceContextUtil.generateResourceIdentifierString(10L, ResourceType.ODC_RESOURCE_GROUP));
        permissionEntity.setBuiltIn(false);
        permissionEntity.setAuthorizationType(AuthorizationType.USER_AUTHORIZATION);
        return permissionEntity;
    }

    private RolePermissionEntity buildRolePermission(PermissionEntity permissionEntity) {
        RolePermissionEntity entity = TestRandom.nextObject(RolePermissionEntity.class);
        entity.setId(null);
        entity.setPermissionId(permissionEntity.getId());
        return entity;
    }
}
