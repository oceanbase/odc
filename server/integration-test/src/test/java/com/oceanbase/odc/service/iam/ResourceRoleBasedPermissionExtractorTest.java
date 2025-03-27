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
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;

/**
 * @Author: Lebie
 * @Date: 2025/3/27 15:28
 * @Description: []
 */
public class ResourceRoleBasedPermissionExtractorTest extends AuthorityTestEnv {
    @Autowired
    private ResourceRoleBasedPermissionExtractor extractor;

    @Test
    public void testGetResourcePermissions_MultiResourceIds_Success() {
        UserResourceRoleEntity entity1 = new UserResourceRoleEntity();
        entity1.setId(1L);
        entity1.setResourceId(1L);
        entity1.setResourceRoleId(1L);

        UserResourceRoleEntity entity2 = new UserResourceRoleEntity();
        entity2.setId(2L);
        entity2.setResourceId(2L);
        entity2.setResourceRoleId(2L);

        List<Permission> actual = extractor.getResourcePermissions(Arrays.asList(entity1, entity2));
        Assert.assertEquals(2, actual.size());
    }

    @Test
    public void testGetResourcePermissions_OneResourceIdMultiRoles_Success() {
        UserResourceRoleEntity entity1 = new UserResourceRoleEntity();
        entity1.setId(1L);
        entity1.setResourceId(1L);
        entity1.setResourceRoleId(1L);

        UserResourceRoleEntity entity2 = new UserResourceRoleEntity();
        entity2.setId(2L);
        entity2.setResourceId(1L);
        entity2.setResourceRoleId(2L);

        List<Permission> actual = extractor.getResourcePermissions(Arrays.asList(entity1, entity2));
        Assert.assertEquals(1, actual.size());
    }

    @Test
    public void testGetResourcePermissions_SameResourceIdMultiTypes_Success() {
        UserResourceRoleEntity entity1 = new UserResourceRoleEntity();
        entity1.setId(1L);
        entity1.setResourceId(1L);
        entity1.setResourceRoleId(1L);

        UserResourceRoleEntity entity2 = new UserResourceRoleEntity();
        entity2.setId(2L);
        entity2.setResourceId(1L);
        entity2.setResourceRoleId(6L);

        List<Permission> result = extractor.getResourcePermissions(Arrays.asList(entity1, entity2));
        Assert.assertEquals(2, result.size());
    }

}
