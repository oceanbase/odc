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
package com.oceanbase.odc.metadb.databasechange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

/**
 * @author: zijia.cj
 * @date: 2024/4/24
 */
public class DatabaseChangeChangingOrderTemplateRepositoryTest extends ServiceTestEnv {

    private static final Long PROJECT_ID = 1L;
    private static final Long CURRENT_USER_ID = 1L;

    private static final Long ORGANIZATION_ID = 1L;
    private static final String TEMPLATE_NAME = "template";

    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository databaseChangeChangingOrderTemplateRepository;

    @Before
    public void setUp() {
        databaseChangeChangingOrderTemplateRepository.deleteAll();
    }

    @After
    public void clear() {
        databaseChangeChangingOrderTemplateRepository.deleteAll();
    }


    @Test
    public void existsByNameAndProjectId_checkTemplateExist_succeed() {
        create();
        Boolean result =
                databaseChangeChangingOrderTemplateRepository.existsByNameAndProjectId(TEMPLATE_NAME, PROJECT_ID);
        assertTrue(result);
    }

    @Test
    public void findByNameAndProjectId_getTemplate_succeed() {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity = create();
        Optional<DatabaseChangeChangingOrderTemplateEntity> result =
                databaseChangeChangingOrderTemplateRepository.findByNameAndProjectId(TEMPLATE_NAME, PROJECT_ID);
        assertNotNull(result.get());
        assertEquals(databaseChangeChangingOrderTemplateEntity, result.get());
    }

    private DatabaseChangeChangingOrderTemplateEntity create() {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                new DatabaseChangeChangingOrderTemplateEntity();
        databaseChangeChangingOrderTemplateEntity.setName(TEMPLATE_NAME);
        databaseChangeChangingOrderTemplateEntity.setProjectId(PROJECT_ID);
        databaseChangeChangingOrderTemplateEntity.setOrganizationId(ORGANIZATION_ID);
        databaseChangeChangingOrderTemplateEntity.setCreatorId(CURRENT_USER_ID);
        List<List<Long>> orders = new ArrayList<>();
        orders.add(Arrays.asList(1L, 2L));
        orders.add(Arrays.asList(3L, 4L));
        databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(orders);
        return databaseChangeChangingOrderTemplateRepository.save(
                databaseChangeChangingOrderTemplateEntity);
    }
}

