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

import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;

public class ConnectionPermissionTest {

    @Test
    public void implies_connectionPermission_impliesTrue() {
        ConnectionPermission permission = new ConnectionPermission("*", "create,write,connect");
        ConnectionPermission permission1 = new ConnectionPermission("12", "create,readonlyconnect");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_connectionPermissionWithDiffResourceId_impliesFalse() {
        ConnectionPermission permission = new ConnectionPermission("12", "create,write");
        ConnectionPermission permission1 = new ConnectionPermission("112", "create");
        Assert.assertFalse(permission.implies(permission1));
    }

    @Test
    public void implies_connectionPermissionWithWildChar_impliesTrue() {
        ConnectionPermission permission =
                new ConnectionPermission("12", "create,write,update, delete, readonlyconnect,read,connect");
        ConnectionPermission permission1 = new ConnectionPermission("12", "*");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_connectionPermissionWithDiffResourceId1_impliesFalse() {
        ConnectionPermission permission =
                new ConnectionPermission("12", ConnectionPermission.CREATE | ResourcePermission.DELETE);
        ConnectionPermission permission1 = new ConnectionPermission("112", ResourcePermission.CREATE);
        Assert.assertFalse(permission.implies(permission1));
    }

}
