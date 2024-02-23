/*
 * Copyright (c) 2024 OceanBase.
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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

public class UserConfigDAOTest extends ServiceTestEnv {
    private static final Long USER_ID = 1L;
    @Autowired
    private UserConfigDAO userConfigDAO;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "config_user_configuration");
    }

    @Test
    public void queryByUserId_NotConfigured_Empty() {
        List<UserConfigEntity> entities = userConfigDAO.queryByUserId(USER_ID);
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void queryByUserIdAndKey() {
        userConfigDAO.batchUpsert(Arrays.asList(createEntity()));
        UserConfigEntity entity = userConfigDAO.queryByUserIdAndKey(USER_ID, "key1");
        Assert.assertEquals("value1", entity.getValue());
    }

    @Test
    public void batchUpsert_NotExists_Insert() {
        userConfigDAO.batchUpsert(Arrays.asList(createEntity()));
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_user_configuration"));
    }

    @Test
    public void batchUpsert_Exists_Update() {
        UserConfigEntity entity = createEntity();
        userConfigDAO.batchUpsert(Arrays.asList(entity));
        userConfigDAO.batchUpsert(Arrays.asList(entity));
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_user_configuration"));
    }

    private UserConfigEntity createEntity() {
        UserConfigEntity entity = new UserConfigEntity();
        entity.setUserId(USER_ID);
        entity.setKey("key1");
        entity.setValue("value1");
        entity.setDescription("desc");
        return entity;
    }

}
