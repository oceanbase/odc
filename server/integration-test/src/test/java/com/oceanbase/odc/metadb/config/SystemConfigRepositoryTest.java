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
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liuyizhuo.lyz
 * @date 2024/3/21
 */
@Slf4j
public class SystemConfigRepositoryTest extends ServiceTestEnv {

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Test
    public void test_Insert_NotExists() {
        int prevRows = JdbcTestUtils.countRowsInTable(systemConfigRepository.getJdbcTemplate(),
                "config_system_configuration");
        int inserted = systemConfigRepository.insert(getConfigEntity());
        Assert.assertEquals(inserted, 1);
        Assert.assertEquals(prevRows + 1, JdbcTestUtils.countRowsInTable(systemConfigRepository.getJdbcTemplate(),
                "config_system_configuration"));
        delete("dummy.key");
    }

    @Test
    public void test_Insert_Exists() {
        SystemConfigEntity config = getConfigEntity();
        systemConfigRepository.insert(config);

        config.setValue("value1");
        systemConfigRepository.insert(config);

        SystemConfigEntity entity = systemConfigRepository.queryByKey("dummy.key");
        Assert.assertEquals("value", entity.getValue());
        delete("dummy.key");
    }

    @Test
    public void test_Upsert_NotExists() {
        int prevRows = JdbcTestUtils.countRowsInTable(systemConfigRepository.getJdbcTemplate(),
                "config_system_configuration");
        int inserted = systemConfigRepository.upsert(getConfigEntity());
        Assert.assertEquals(inserted, 1);
        Assert.assertEquals(prevRows + 1, JdbcTestUtils.countRowsInTable(systemConfigRepository.getJdbcTemplate(),
                "config_system_configuration"));
        delete("dummy.key");
    }

    @Test
    public void test_Upsert_Exists() {
        SystemConfigEntity config = getConfigEntity();
        systemConfigRepository.upsert(config);

        config.setValue("value1");
        systemConfigRepository.upsert(config);

        SystemConfigEntity entity = systemConfigRepository.queryByKey("dummy.key");
        Assert.assertEquals("value1", entity.getValue());
        delete("dummy.key");
    }

    @Test
    public void test_QueryByKeyPrefix() {
        SystemConfigEntity entity = getConfigEntity();
        systemConfigRepository.upsert(entity);
        entity.setKey("dummy.key1");
        systemConfigRepository.upsert(entity);

        List<SystemConfigEntity> entities = systemConfigRepository.queryByKeyPrefix("dummy");
        Assert.assertEquals(2, entities.size());
        delete("dummy.key");
        delete("dummy.key1");
    }

    @Test
    public void test_QueryByKey() {
        systemConfigRepository.upsert(getConfigEntity());
        SystemConfigEntity entity = systemConfigRepository.queryByKey("dummy.key");
        Assert.assertEquals("value", entity.getValue());
        delete("dummy.key");
    }

    private void delete(String... keys) {
        for (String key : keys) {
            int rows = systemConfigRepository.getJdbcTemplate()
                    .update("delete from `config_system_configuration` where `key` = '" + key + "'");
            log.info("delete {} rows with key = {}", rows, key);
        }
    }

    private SystemConfigEntity getConfigEntity() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setKey("dummy.key");
        entity.setValue("value");
        entity.setLabel("master");
        entity.setApplication("odc");
        entity.setProfile("default");
        entity.setDescription("description");
        return entity;
    }

}
