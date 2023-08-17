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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.service.iam.model.PermissionConfig;
import com.oceanbase.odc.service.iam.util.PermissionUtil;

/**
 * @author gaoda.xy
 * @date 2023/1/5 19:56
 */
public class PermissionUtilTest {

    @Test
    public void test_aggregatePermissions() {
        List<PermissionEntity> permissionEntities = Arrays.asList(
                createPermission("ODC_CONNECTION:1", "connect", PermissionType.PUBLIC_RESOURCE),
                createPermission("ODC_CONNECTION:1", "apply", PermissionType.PUBLIC_RESOURCE),
                createPermission("ODC_CONNECTION:2", "apply", PermissionType.PUBLIC_RESOURCE),
                createPermission("ODC_CONNECTION:2", "apply", PermissionType.PUBLIC_RESOURCE));
        List<PermissionConfig> permissionConfigs = PermissionUtil.aggregatePermissions(permissionEntities);
        Assert.assertEquals(2, permissionConfigs.size());
        for (PermissionConfig config : permissionConfigs) {
            if (config.getResourceId() == 1) {
                Assert.assertEquals(2, config.getActions().size());
                Assert.assertTrue(config.getActions().contains("connect"));
                Assert.assertTrue(config.getActions().contains("apply"));
            } else {
                Assert.assertEquals(1, config.getActions().size());
                Assert.assertTrue(config.getActions().contains("apply"));
            }
        }
    }

    @Test
    public void aggregateResourceManagementPermissions() {
        List<PermissionEntity> permissionEntities = Arrays.asList(
                createPermission("ODC_CONNECTION:1", "create", PermissionType.SYSTEM),
                createPermission("ODC_CONNECTION:1", "read", PermissionType.SYSTEM),
                createPermission("ODC_CONNECTION:1", "update", PermissionType.SYSTEM),
                createPermission("ODC_CONNECTION:1", "delete", PermissionType.SYSTEM));
        List<PermissionConfig> permissionConfigs =
                PermissionUtil.aggregateResourceManagementPermissions(permissionEntities);
        Assert.assertEquals(2, permissionConfigs.size());
        for (PermissionConfig config : permissionConfigs) {
            if (config.getActions().size() == 1) {
                Assert.assertTrue(config.getActions().contains("create"));
            } else {
                Assert.assertTrue(config.getActions().contains("read"));
                Assert.assertTrue(config.getActions().contains("update"));
                Assert.assertTrue(config.getActions().contains("delete"));
            }
        }
    }

    @Test
    public void isConnectionAccessPermission() {
        Assert.assertTrue(PermissionUtil.isConnectionAccessPermission(
                createPermission("ODC_CONNECTION:1", "connect", PermissionType.PUBLIC_RESOURCE)));
        Assert.assertTrue(PermissionUtil.isConnectionAccessPermission(
                createPermission("ODC_CONNECTION:*", "readonlyconnect", PermissionType.PUBLIC_RESOURCE)));
        Assert.assertTrue(PermissionUtil.isConnectionAccessPermission(
                createPermission("ODC_CONNECTION:1", "apply", PermissionType.PUBLIC_RESOURCE)));
        Assert.assertFalse(PermissionUtil.isConnectionAccessPermission(
                createPermission("ODC_CONNECTION:1", "read", PermissionType.PUBLIC_RESOURCE)));
        Assert.assertFalse(PermissionUtil.isConnectionAccessPermission(
                createPermission("ODC_PRIVATE_CONNECTION:1", "create", PermissionType.PRIVATE_RESOURCE)));
    }

    @Test
    public void isResourceManagementPermission() {
        Assert.assertTrue(PermissionUtil.isResourceManagementPermission(
                createPermission("ODC_CONNECTION:1", "read", PermissionType.PUBLIC_RESOURCE)));
        Assert.assertTrue(PermissionUtil.isResourceManagementPermission(
                createPermission("ODC_USER:1", "create", PermissionType.SYSTEM)));
        Assert.assertTrue(PermissionUtil.isResourceManagementPermission(
                createPermission("ODC_ROLE:*", "delete", PermissionType.SYSTEM)));
        Assert.assertFalse(PermissionUtil.isResourceManagementPermission(
                createPermission("ODC_CONNECTION:1", "connect", PermissionType.PUBLIC_RESOURCE)));
    }

    @Test
    public void isSystemOperationPermission() {
        Assert.assertTrue(PermissionUtil.isSystemOperationPermission(
                createPermission("ODC_AUDIT_EVENT:*", "create", PermissionType.SYSTEM)));
        Assert.assertFalse(PermissionUtil.isSystemOperationPermission(
                createPermission("ODC_ROLE:1", "create", PermissionType.SYSTEM)));
        Assert.assertFalse(PermissionUtil.isSystemOperationPermission(
                createPermission("ODC_CONNECTION:1", "connect", PermissionType.PUBLIC_RESOURCE)));
    }

    private PermissionEntity createPermission(String resourceIdentifier, String action, PermissionType type) {
        PermissionEntity entity = new PermissionEntity();
        entity.setType(type);
        entity.setAction(action);
        entity.setResourceIdentifier(resourceIdentifier);
        return entity;
    }
}
