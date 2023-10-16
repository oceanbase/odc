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

package com.oceanbase.odc.service.onlineschemachange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.onlineschemachange.ddl.DBAccountLockType;
import com.oceanbase.odc.service.onlineschemachange.ddl.DBUser;
import com.oceanbase.odc.service.onlineschemachange.ddl.OscOBMySqlAccessor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author yaobin
 * @date 2023-10-13
 * @since 4.2.3
 */
public class OscOBMySqlAccessorTest extends OBMySqlOscTestEnv {

    @Test
    public void listUsersDetail_SpecifyUsername_Success() {
        OscOBMySqlAccessor accessor = new OscOBMySqlAccessor(jdbcTemplate);

        List<String> users = new ArrayList<>();
        users.add("root");
        users.add("roottest1");
        List<DBUser> dbUsers = accessor.listUsers(users);
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).type());
        Assert.assertNotNull(dbUsers.get(0).getName());
        Optional<DBUser> dbUser = dbUsers.stream().filter(a -> a.getName().equals("root")).findFirst();
        Assert.assertTrue(dbUser.isPresent());
        Assert.assertSame(DBAccountLockType.UNLOCKED, dbUser.get().getAccountLocked());

        boolean exists = dbUsers.stream().anyMatch(a -> a.getName().equals("roottest1"));
        Assert.assertFalse(exists);
    }

    @Test
    public void listUsersDetail_NoSpecifyUsername_Success() {

        OscOBMySqlAccessor accessor = new OscOBMySqlAccessor(jdbcTemplate);
        List<DBUser> dbUsers = accessor.listUsers(null);
        Assert.assertFalse(dbUsers.isEmpty());
        Assert.assertSame(DBObjectType.USER, dbUsers.get(0).type());
        Assert.assertNotNull(dbUsers.get(0).getName());
        Optional<DBUser> dbUser = dbUsers.stream().filter(a -> a.getName().equals("root")).findFirst();
        Assert.assertTrue(dbUser.isPresent());
        Assert.assertSame(DBAccountLockType.UNLOCKED, dbUser.get().getAccountLocked());
    }

}
