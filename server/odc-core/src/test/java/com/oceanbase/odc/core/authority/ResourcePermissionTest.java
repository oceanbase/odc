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
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;

/**
 * Test object for {@link Permission}
 *
 * @author yh263208
 * @date 2021-08-02 15:24
 * @since ODC_release_3.2.0
 */
public class ResourcePermissionTest {

    @Test
    public void implies_resourcePermission_impliesTrue() {
        ResourcePermission permission = new ResourcePermission(new DefaultSecurityResource("*", "*"), "create,write");
        ResourcePermission permission1 = new ResourcePermission(new DefaultSecurityResource("12", "conn"), "create");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_resourcePermissionWithDiffResourceId_impliesFalse() {
        ResourcePermission permission =
                new ResourcePermission(new DefaultSecurityResource("12", "conn"), "create,write");
        ResourcePermission permission1 = new ResourcePermission(new DefaultSecurityResource("112", "conn"), "create");
        Assert.assertFalse(permission.implies(permission1));
    }

    @Test
    public void implies_resourcePermissionWithWildChar_impliesTrue() {
        ResourcePermission permission = new ResourcePermission(
                new DefaultSecurityResource("12", "conn"), "create,write,update, delete, readwrite,read");
        ResourcePermission permission1 = new ResourcePermission(new DefaultSecurityResource("12", "conn"), "*");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_resourcePermissionWithDiffResourceId1_impliesFalse() {
        ResourcePermission permission = new ResourcePermission(new DefaultSecurityResource("12", "conn"),
                ResourcePermission.CREATE | ResourcePermission.DELETE);
        ResourcePermission permission1 = new ResourcePermission(new DefaultSecurityResource("112", "conn"),
                ResourcePermission.CREATE);
        Assert.assertFalse(permission.implies(permission1));
    }

}
