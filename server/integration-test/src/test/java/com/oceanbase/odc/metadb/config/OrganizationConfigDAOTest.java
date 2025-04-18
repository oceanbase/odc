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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.config.model.Configuration;

public class OrganizationConfigDAOTest extends ServiceTestEnv {
    private static final Long ORGANIZATION_ID_T1 = 1000L;
    private static final List<String> CONFIG_KEY = Arrays.asList("test_key1", "test_key2", "test_key3");

    @Autowired
    private OrganizationConfigDAO organizationConfigDAO;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "config_organization_configuration");
    }

    @Test
    public void queryByOrganizationId_NotConfigured_Empty() {
        List<OrganizationConfigEntity> entities = organizationConfigDAO.queryByOrganizationId(ORGANIZATION_ID_T1);
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void queryByOrganizationId() {
        organizationConfigDAO.batchUpsert(createEntity());
        List<OrganizationConfigEntity> result = organizationConfigDAO.queryByOrganizationId(ORGANIZATION_ID_T1);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("value_test", result.get(0).getValue());
    }

    @Test
    public void queryByOrganizationIdAndKey() {
        organizationConfigDAO.batchUpsert(createEntity());
        OrganizationConfigEntity org_config =
                organizationConfigDAO.queryByOrganizationIdAndKey(ORGANIZATION_ID_T1, "test_key1");
        System.out.println(org_config);
        Assert.assertEquals("value_test", org_config.getValue());
    }

    @Test
    public void batchUpsert_NotExists_Insert() {
        List<OrganizationConfigEntity> entities = createEntity();

        organizationConfigDAO.batchUpsert(entities);
        Assert.assertEquals(3, JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_organization_configuration"));
    }

    @Test
    public void batchUpsert_Exists_Update() {
        List<OrganizationConfigEntity> entities = createEntity();

        int rows = organizationConfigDAO.batchUpsert(entities);
        Assert.assertEquals(3, rows);
        List<OrganizationConfigEntity> wait2Update = entities.stream().peek(e -> {
            if ("test_key1".equals(e.getKey())) {
                e.setValue("value_test_update1");
                e.setLastModifierId(110L);
            }
            if ("test_key2".equals(e.getKey())) {
                e.setValue("value_test_update2");
                e.setLastModifierId(120L);
            }
        }).collect(Collectors.toList());
        System.out.println(wait2Update);
        organizationConfigDAO.batchUpsert(wait2Update);

        OrganizationConfigEntity org_config1 = findEntity(ORGANIZATION_ID_T1, "test_key1");
        System.out.println(org_config1);
        Assert.assertEquals("value_test_update1", org_config1.getValue());
        Assert.assertEquals(110L, (long) org_config1.getLastModifierId());
        OrganizationConfigEntity org_config2 = findEntity(ORGANIZATION_ID_T1, "test_key2");
        System.out.println(org_config2);
        Assert.assertEquals("value_test_update2", org_config2.getValue());
        Assert.assertEquals(120L, (long) org_config2.getLastModifierId());
        // update before
        OrganizationConfigEntity org_config3 = findEntity(ORGANIZATION_ID_T1, "test_key3");
        System.out.println(org_config3);
        Assert.assertEquals("value_test", org_config3.getValue());
        Assert.assertEquals(1L, (long) org_config3.getLastModifierId());
    }

    private List<OrganizationConfigEntity> createEntity() {
        List<OrganizationConfigEntity> entities = new ArrayList<>();
        for (String key_test : CONFIG_KEY) {
            Configuration entity = new Configuration();
            entity.setKey(key_test);
            entity.setValue("value_test");
            OrganizationConfigEntity entityOrg = entity.convert2DO(ORGANIZATION_ID_T1, 1L);
            entities.add(entityOrg);
        }
        return entities;
    }

    private OrganizationConfigEntity findEntity(Long organizationId, String key) {
        String sql = "SELECT organization_id, `key`, `value`, creator_id, last_modifier_id, description"
                + " FROM config_organization_configuration WHERE organization_id = ? AND `key` = ?";
        return jdbcTemplate.queryForObject(sql,
                new BeanPropertyRowMapper<>(OrganizationConfigEntity.class), organizationId, key);
    }
}
