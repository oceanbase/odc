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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

/**
 * @author liuyizhuo.lyz
 * @date 2024/3/21
 */
public class SystemConfigDAOTest extends ServiceTestEnv {

    @Autowired
    private SystemConfigDAO systemConfigDAO;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        jdbcTemplate.batchUpdate("delete from config_system_configuration");
    }

    @Test
    public void test_Upsert_NotExists() {
        systemConfigDAO.upsert(getConfigEntity());
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_system_configuration"));
    }

    @Test
    public void test_Upsert_Exists() {
        systemConfigDAO.upsert(getConfigEntity());
        systemConfigDAO.upsert(getConfigEntity());

        Assert.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_system_configuration"));
    }

    @Test
    public void test_QueryByKeyPrefix() {
        SystemConfigEntity entity = getConfigEntity();
        systemConfigDAO.upsert(entity);
        entity.setKey("dummy.key1");
        systemConfigDAO.upsert(entity);

        List<SystemConfigEntity> entities = systemConfigDAO.queryByKeyPrefix("dummy");
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void test_QueryByKey() {
        systemConfigDAO.upsert(getConfigEntity());
        SystemConfigEntity entity = systemConfigDAO.queryByKey("dummy.key");
        Assert.assertEquals("value", entity.getValue());
    }

    private SystemConfigEntity getConfigEntity() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setKey("dummy.key");
        entity.setValue("value");
        entity.setDescription("description");
        return entity;
    }

}
