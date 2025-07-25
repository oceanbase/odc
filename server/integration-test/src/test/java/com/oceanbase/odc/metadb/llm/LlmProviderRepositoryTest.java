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
package com.oceanbase.odc.metadb.llm;

import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

/**
 * Test cases for {@link LlmProviderRepository}
 *
 * @author yh263208
 * @date 2023-12-20 15:30
 * @since ODC_release_4.4.0
 * @see ServiceTestEnv
 */
public class LlmProviderRepositoryTest extends ServiceTestEnv {

    @Autowired
    private LlmProviderRepository repository;

    private static final Long ORGANIZATION_ID = 1L;
    private static final Long CREATOR_ID = 1L;
    private static final Long MODIFIER_ID = 2L;
    private static final String PROVIDER_NAME = "test_provider";
    private static final String PROVIDER_NAME_2 = "test_provider_2";
    private static final String PROPERTIES_JSON = "{\"api_key\":\"test_key\"}";
    private static final String SALT = "test_salt";

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void findAllByOrganizationId_entityExists_returnNotEmpty() {
        // Given
        LlmProviderEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        LlmProviderEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2);
        LlmProviderEntity entity3 = createEntity(2L, PROVIDER_NAME);
        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);

        // When
        List<LlmProviderEntity> result = repository.findAllByOrganizationId(ORGANIZATION_ID);

        // Then
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().anyMatch(e -> e.getName().equals(PROVIDER_NAME)));
        Assert.assertTrue(result.stream().anyMatch(e -> e.getName().equals(PROVIDER_NAME_2)));
    }

    @Test
    public void findAllByOrganizationId_entityNotExists_returnEmpty() {
        // Given
        LlmProviderEntity entity = createEntity(2L, PROVIDER_NAME);
        repository.save(entity);

        // When
        List<LlmProviderEntity> result = repository.findAllByOrganizationId(ORGANIZATION_ID);

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrganizationIdAndName_entityExists_returnNotEmpty() {
        // Given
        LlmProviderEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        entity = repository.save(entity);

        // When
        Optional<LlmProviderEntity> result = repository.findByOrganizationIdAndName(ORGANIZATION_ID, PROVIDER_NAME);

        // Then
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(entity.getId(), result.get().getId());
        Assert.assertEquals(entity.getName(), result.get().getName());
        Assert.assertEquals(entity.getOrganizationId(), result.get().getOrganizationId());
    }

    @Test
    public void findByOrganizationIdAndName_entityNotExists_returnEmpty() {
        // Given
        LlmProviderEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        repository.save(entity);

        // When
        Optional<LlmProviderEntity> result = repository.findByOrganizationIdAndName(ORGANIZATION_ID, "non_existing");

        // Then
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void findByOrganizationIdAndName_differentOrganization_returnEmpty() {
        // Given
        LlmProviderEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        repository.save(entity);

        // When
        Optional<LlmProviderEntity> result = repository.findByOrganizationIdAndName(2L, PROVIDER_NAME);

        // Then
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void deleteByOrganizationIdAndName_entityExists_returnCount() {
        // Given
        LlmProviderEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        LlmProviderEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2);
        LlmProviderEntity entity3 = createEntity(2L, PROVIDER_NAME);
        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);

        // When
        int deletedCount = repository.deleteByOrganizationIdAndName(ORGANIZATION_ID, PROVIDER_NAME);

        // Then
        Assert.assertEquals(1, deletedCount);
        Assert.assertEquals(2, repository.count());
        Assert.assertFalse(repository.findByOrganizationIdAndName(ORGANIZATION_ID, PROVIDER_NAME).isPresent());
        Assert.assertTrue(repository.findByOrganizationIdAndName(ORGANIZATION_ID, PROVIDER_NAME_2).isPresent());
        Assert.assertTrue(repository.findByOrganizationIdAndName(2L, PROVIDER_NAME).isPresent());
    }

    @Test
    public void deleteByOrganizationIdAndName_entityNotExists_returnZero() {
        // Given
        LlmProviderEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        repository.save(entity);

        // When
        int deletedCount = repository.deleteByOrganizationIdAndName(ORGANIZATION_ID, "non_existing");

        // Then
        Assert.assertEquals(0, deletedCount);
        Assert.assertEquals(1, repository.count());
    }

    @Test
    public void save_normalEntity_saveSucceed() {
        // Given
        LlmProviderEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME);

        // When
        LlmProviderEntity saved = repository.save(entity);

        // Then
        Assert.assertNotNull(saved.getId());
        Assert.assertEquals(entity.getName(), saved.getName());
        Assert.assertEquals(entity.getOrganizationId(), saved.getOrganizationId());
        Assert.assertEquals(entity.getCreatorId(), saved.getCreatorId());
        Assert.assertEquals(entity.getLastModifierId(), saved.getLastModifierId());
        Assert.assertEquals(entity.getPropertiesJson(), saved.getPropertiesJson());
        Assert.assertEquals(entity.getSalt(), saved.getSalt());
    }

    @Test
    public void save_multipleEntities_saveSucceed() {
        // Given
        LlmProviderEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME);
        LlmProviderEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2);

        // When
        repository.save(entity1);
        repository.save(entity2);

        // Then
        List<LlmProviderEntity> all = repository.findAll();
        Assert.assertEquals(2, all.size());
    }

    private LlmProviderEntity createEntity(Long organizationId, String name) {
        LlmProviderEntity entity = new LlmProviderEntity();
        entity.setOrganizationId(organizationId);
        entity.setCreatorId(CREATOR_ID);
        entity.setLastModifierId(MODIFIER_ID);
        entity.setName(name);
        entity.setPropertiesJson(PROPERTIES_JSON);
        entity.setSalt(SALT);
        return entity;
    }
}
