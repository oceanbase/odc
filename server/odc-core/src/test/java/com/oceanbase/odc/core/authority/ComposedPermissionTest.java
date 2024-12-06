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
package com.oceanbase.odc.core.authority;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.ComposedPermission;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ProjectPermission;
import com.oceanbase.odc.core.authority.permission.ResourceRoleBasedPermission;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;

/**
 * @Author: Lebie
 * @Date: 2024/11/13 17:45
 * @Description: []
 */
public class ComposedPermissionTest {
    @Test
    public void implies_HasResourcePermission_impliesTrue() {
        ComposedPermission thisPermission = new ComposedPermission(Arrays.asList(getResourcePermission("*", "DBA")));
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertTrue(thisPermission.implies(thatPermission));
    }

    @Test
    public void implies_HasResourceRolePermission_impliesTrue() {
        ComposedPermission thisPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA))));
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertTrue(thisPermission.implies(thatPermission));
    }

    @Test
    public void implies_HasBothPermissions_impliesTrue() {
        ComposedPermission thisPermission = new ComposedPermission(Arrays.asList(getResourcePermission("*", "DBA"),
                getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA))));
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertTrue(thisPermission.implies(thatPermission));
    }

    @Test
    public void implies_HasWrongAndRightPermissions_impliesTrue() {
        ComposedPermission thisPermission = new ComposedPermission(Arrays.asList(getResourcePermission("*", "OWNER"),
                getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA))));
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertTrue(thisPermission.implies(thatPermission));
    }

    @Test
    public void implies_ImpliesNoPermission_impliesFalse() {
        ComposedPermission thisPermission = new ComposedPermission(Arrays.asList(getResourcePermission("*", "OWNER")));
        ComposedPermission thatPermission = new ComposedPermission(Collections.emptyList());
        Assert.assertTrue(thisPermission.implies(thatPermission));
    }


    @Test
    public void implies_HasWrongResourceRolePermission_impliesFalse() {
        ComposedPermission thisPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.OWNER))));
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertFalse(thisPermission.implies(thatPermission));
    }

    @Test
    public void implies_HasWrongResourcePermission_impliesFalse() {
        ComposedPermission thisPermission = new ComposedPermission(
                Arrays.asList(getResourcePermission("*", "OWNER")));
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertFalse(thisPermission.implies(thatPermission));
    }

    @Test
    public void implies_HasNoPermission_impliesFalse() {
        ComposedPermission thisPermission = new ComposedPermission(Collections.emptyList());
        ComposedPermission thatPermission = new ComposedPermission(
                Arrays.asList(getResourceRolePermission("1", Arrays.asList(ResourceRoleName.DBA)),
                        getResourcePermission("1", "DBA")));
        Assert.assertFalse(thisPermission.implies(thatPermission));
    }

    private Permission getResourceRolePermission(String resourceId, List<ResourceRoleName> resourceRoles) {
        return new ResourceRoleBasedPermission(new DefaultSecurityResource(resourceId, "ODC_PROJECT"),
                resourceRoles);
    }

    private Permission getResourcePermission(String resourceId, String actions) {
        return new ProjectPermission(new DefaultSecurityResource(resourceId, "ODC_PROJECT"), actions);
    }
}
