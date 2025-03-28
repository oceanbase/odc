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

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.oceanbase.odc.ServiceTestEnv;

public class OrganizationConfigDAOTest extends ServiceTestEnv {
    private static final Long ORGANIZATION_ID = 1000L;
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
        List<OrganizationConfigEntity> entities = organizationConfigDAO.queryByOrganizationId(ORGANIZATION_ID);
        Assert.assertEquals(0, entities.size());
    }

    @Test
    public void batchUpsert_NotExists_Insert() {
        organizationConfigDAO.batchUpsert(Arrays.asList(createEntity()));
        Assert.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_organization_configuration"));
    }

    @Test
    public void batchUpsert_Exists_Update() {
        OrganizationConfigEntity entity = createEntity();
        organizationConfigDAO.batchUpsert(Arrays.asList(entity));
        organizationConfigDAO.batchUpsert(Arrays.asList(entity));
        Assert.assertEquals(1,
                JdbcTestUtils.countRowsInTable(jdbcTemplate, "config_organization_configuration"));
    }

    private OrganizationConfigEntity createEntity() {
        OrganizationConfigEntity entity = new OrganizationConfigEntity();
        entity.setKey("key_test");
        entity.setValue("value_test");
        entity.setDescription("desc_test");
        entity.setOrganizationId(ORGANIZATION_ID);
        return entity;
    }
}
