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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.ResourceRoleBasedPermission;

public class ResourceRoleBasedPermissionTest {
    @Test
    public void implies_SameResourceTypeAndId_impliesTrue() {
        ResourceRoleBasedPermission permission =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_PROJECT"),
                        "OWNER, DBA");
        ResourceRoleBasedPermission permission1 =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_PROJECT"),
                        "OWNER");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_SameResourceTypeAndId_impliesFalse() {
        ResourceRoleBasedPermission permission =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_PROJECT"),
                        "DBA");
        ResourceRoleBasedPermission permission1 =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_PROJECT"),
                        "OWNER");
        Assert.assertFalse(permission.implies(permission1));
    }

    @Test
    public void implies_DifferentResourceId_impliesFalse() {
        ResourceRoleBasedPermission permission =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_PROJECT"),
                        "DBA");
        ResourceRoleBasedPermission permission1 =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("2", "ODC_PROJECT"),
                        "DBA");
        Assert.assertFalse(permission.implies(permission1));
    }

    @Test
    public void implies_DifferentResourceType_impliesFalse() {
        ResourceRoleBasedPermission permission =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_DATABASE"),
                        "DBA");
        ResourceRoleBasedPermission permission1 =
                new ResourceRoleBasedPermission(new DefaultSecurityResource("1", "ODC_PROJECT"),
                        "DBA");
        Assert.assertFalse(permission.implies(permission1));
    }
}
