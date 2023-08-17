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
package com.oceanbase.odc.metadb.iam;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.service.iam.model.UserInfo;
import com.oceanbase.odc.test.tool.TestRandom;

public class UserInfoDAOTest extends ServiceTestEnv {

    @Autowired
    private UserInfoDAO userDAO;

    @Test
    public void insert_insertUser_insertSucceed() {
        UserInfo expect = TestRandom.nextObject(UserInfo.class);
        Assert.assertEquals(1, userDAO.insert(expect));
    }

    @Test
    public void delete_deleteUser_deleteSucceed() {
        UserInfo expect = TestRandom.nextObject(UserInfo.class);
        userDAO.insert(expect);
        expect = userDAO.detail(expect.getEmail());
        Assert.assertEquals(1, userDAO.delete(expect.getId()));
    }

    @Test
    public void update_updateUser_updateSucceed() {
        UserInfo expect = TestRandom.nextObject(UserInfo.class);
        userDAO.insert(expect);
        expect = userDAO.detail(expect.getEmail());
        expect.setName("new name");
        expect.setPassword("new password");
        expect.setRole("new role");
        expect.setStatus(expect.getStatus() + 10);
        expect.setDesc("new desc");
        expect.setCipher(Cipher.BCRYPT);
        userDAO.update(expect);
        UserInfo actual = userDAO.get(expect.getId());
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void get_getById_getSucceed() {
        UserInfo expect = TestRandom.nextObject(UserInfo.class);
        userDAO.insert(expect);
        expect = userDAO.detail(expect.getEmail());
        UserInfo actual = userDAO.get(expect.getId());
        expect.setGmtCreated(null);
        expect.setGmtModified(null);
        expect.setEnabled(true);
        actual.setEnabled(true);
        Assert.assertEquals(expect, actual);
    }

    @Test
    public void detail_detailByEmail_detailSucceed() {
        UserInfo expect = TestRandom.nextObject(UserInfo.class);
        userDAO.insert(expect);
        UserInfo actual = userDAO.detail(expect.getEmail());
        expect.setId(actual.getId());
        expect.setGmtCreated(null);
        expect.setGmtModified(null);
        expect.setEnabled(true);
        actual.setEnabled(true);
        Assert.assertEquals(expect, actual);
    }

}
