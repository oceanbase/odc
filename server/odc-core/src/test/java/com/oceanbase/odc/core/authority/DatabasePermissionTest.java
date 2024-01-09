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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.authority.permission.DatabasePermission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;

/**
 * @author gaoda.xy
 * @date 2024/1/2 16:28
 */
public class DatabasePermissionTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void construct_databasePermission_allAction_1() {
        ResourcePermission permission =
                new DatabasePermission("1", "apply,create,read,update,delete,query,change,export");
        Assert.assertEquals(DatabasePermission.ALL, permission.getMask());
    }

    @Test
    public void construct_databasePermission_allAction_2() {
        ResourcePermission permission = new DatabasePermission("1", "*");
        Assert.assertEquals(DatabasePermission.ALL, permission.getMask());
    }

    @Test
    public void construct_databasePermission_invalidAction_emptyPermission() {
        ResourcePermission permission = new DatabasePermission("1", "connect, readonlyconnect,use");
        Assert.assertEquals(0, permission.getMask());
    }

    @Test
    public void constructDatabasePermission_invalidMask_throwException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Mask value is illegal");
        new DatabasePermission("1", ConnectionPermission.READONLY_CONNECT | ConnectionPermission.READWRITE_CONNECT);
    }

    @Test
    public void implies_databasePermission_impliesTrue() {
        ResourcePermission permission = new DatabasePermission("*", "create,export");
        ResourcePermission permission1 = new DatabasePermission("1", "create,apply,export");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_databasePermissionWithDiffResourceId_impliesFalse() {
        ResourcePermission permission = new DatabasePermission("1", "create,export");
        ResourcePermission permission1 = new DatabasePermission("2", "create,apply,export");
        Assert.assertFalse(permission.implies(permission1));
    }

    @Test
    public void implies_databasePermissionWithWildChar_impliesTrue() {
        ResourcePermission permission =
                new DatabasePermission("1", "apply,create,read,update,delete,query,change,export");
        ResourcePermission permission1 = new DatabasePermission("1", "*");
        Assert.assertTrue(permission.implies(permission1));
    }

    @Test
    public void implies_databasePermissionWithDiffResourceId1_impliesFalse() {
        ResourcePermission permission =
                new DatabasePermission("1", DatabasePermission.CREATE | DatabasePermission.DELETE);
        ResourcePermission permission1 = new DatabasePermission("2", DatabasePermission.CREATE);
        Assert.assertFalse(permission.implies(permission1));
    }

    @Test
    public void test_getActionList_all() {
        ResourcePermission permission = new DatabasePermission("1", DatabasePermission.ALL);
        List<String> actual = DatabasePermission.getActionList(permission.getMask());
        Assert.assertEquals("*", actual.get(0));
    }

    @Test
    public void test_getActionList() {
        ResourcePermission permission = new DatabasePermission("1",
                DatabasePermission.ALL ^ ResourcePermission.READ | ResourcePermission.APPLY);
        List<String> actual = DatabasePermission.getActionList(permission.getMask());
        Set<String> actions = DatabasePermission.getAllActions();
        actions.remove("read");
        List<String> excepted = new ArrayList<>(actions);
        Assert.assertEquals(excepted.size(), actual.size());
        for (String action : excepted) {
            Assert.assertTrue(actual.contains(action));
        }
    }

}
