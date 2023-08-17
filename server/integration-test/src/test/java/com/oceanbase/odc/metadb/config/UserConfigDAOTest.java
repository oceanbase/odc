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
package com.oceanbase.odc.metadb.config;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;

public class UserConfigDAOTest extends ServiceTestEnv {

    @Autowired
    private UserConfigDAO userConfigDAO;

    @Test
    public void insert_insertUserConfig_insertSucceed() {
        UserConfigDO expect = TestRandom.nextObject(UserConfigDO.class);
        Assert.assertEquals(1, userConfigDAO.insert(expect));
    }

    @Test
    public void get_getByUserIdAndKey_getSucceed() {
        UserConfigDO expect = TestRandom.nextObject(UserConfigDO.class);
        userConfigDAO.insert(expect);
        UserConfigDO actual = userConfigDAO.get(expect.getUserId(), expect.getKey());
        expect.setUpdateTime(null);
        actual.setUpdateTime(null);
        expect.setCreateTime(null);
        actual.setCreateTime(null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void get_noCandidate_getFailed() {
        UserConfigDO expect = TestRandom.nextObject(UserConfigDO.class);
        userConfigDAO.insert(expect);
        UserConfigDO actual = userConfigDAO.get(expect.getUserId(), expect.getKey() + "sss");
        Assert.assertNull(actual);
    }

    @Test
    public void listByUserId_listByUserId_listSucceed() {
        UserConfigDO expect = TestRandom.nextObject(UserConfigDO.class);
        userConfigDAO.insert(expect);
        long userId = expect.getUserId();
        expect = TestRandom.nextObject(UserConfigDO.class);
        expect.setUserId(userId + 1);
        userConfigDAO.insert(expect);
        List<UserConfigDO> actual = userConfigDAO.listByUserId(expect.getUserId());
        expect.setCreateTime(null);
        expect.setUpdateTime(null);
        actual.forEach(i -> {
            i.setUpdateTime(null);
            i.setCreateTime(null);
        });
        Assert.assertEquals(Collections.singletonList(expect), actual);
    }

    @Test
    public void update_updateConfig_updateSucceed() {
        UserConfigDO expect = TestRandom.nextObject(UserConfigDO.class);
        userConfigDAO.insert(expect);
        expect.setValue("new value");
        expect.setDescription("new desp");
        userConfigDAO.update(expect);
        UserConfigDO actual = userConfigDAO.get(expect.getUserId(), expect.getKey());
        expect.setUpdateTime(null);
        actual.setUpdateTime(null);
        expect.setCreateTime(null);
        actual.setCreateTime(null);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void delete_deleteByUserIdAndKey_deleteSucceed() {
        UserConfigDO expect = TestRandom.nextObject(UserConfigDO.class);
        userConfigDAO.insert(expect);
        Assert.assertEquals(1, userConfigDAO.delete(expect.getUserId(), expect.getKey()));
    }

}
