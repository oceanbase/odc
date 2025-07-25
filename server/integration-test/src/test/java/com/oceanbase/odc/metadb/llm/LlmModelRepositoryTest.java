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
import com.oceanbase.odc.service.llm.model.ModelType;

/**
 * Test cases for {@link LlmModelRepository}
 *
 * @author yh263208
 * @date 2023-12-20 15:30
 * @since ODC_release_4.4.0
 * @see ServiceTestEnv
 */
public class LlmModelRepositoryTest extends ServiceTestEnv {

    private static final Long ORGANIZATION_ID = 1L;
    private static final Long CREATOR_ID = 1L;
    private static final Long MODIFIER_ID = 2L;
    private static final String PROVIDER_NAME = "test_provider";
    private static final String PROVIDER_NAME_2 = "test_provider_2";
    private static final String MODEL_NAME = "test_model";
    private static final String MODEL_NAME_2 = "test_model_2";
    private static final String DISPLAY_NAME = "Test Model";
    private static final String PROPERTIES_JSON = "{\"max_tokens\":1000}";
    private static final String SALT = "test_salt";
    @Autowired
    private LlmModelRepository repository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void findProviderNamesByOrganizationId_entityExists_returnNotEmpty() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME_2);
        LlmModelEntity entity3 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2);
        LlmModelEntity entity4 = createEntity(2L, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);
        repository.save(entity4);

        // When
        List<Object[]> result = repository.countModelsByProviderNameInOrganization(ORGANIZATION_ID);

        // Then
        Assert.assertEquals(2, result.size());
        Assert.assertArrayEquals(new Object[] {PROVIDER_NAME, 2L}, result.get(0));
        Assert.assertArrayEquals(new Object[] {PROVIDER_NAME_2, 1L}, result.get(1));
    }

    @Test
    public void findProviderNamesByOrganizationId_entityNotExists_returnEmpty() {
        // Given
        LlmModelEntity entity = createEntity(2L, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity);

        // When
        List<Object[]> result = repository.countModelsByProviderNameInOrganization(ORGANIZATION_ID);

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrganizationId_entityExists_returnNotEmpty() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME_2);
        LlmModelEntity entity3 = createEntity(2L, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);

        // When
        List<LlmModelEntity> result = repository.findByOrganizationId(ORGANIZATION_ID);

        // Then
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().anyMatch(e -> e.getModelName().equals(MODEL_NAME)));
        Assert.assertTrue(result.stream().anyMatch(e -> e.getModelName().equals(MODEL_NAME_2)));
    }

    @Test
    public void findByOrganizationId_entityNotExists_returnEmpty() {
        // Given
        LlmModelEntity entity = createEntity(2L, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity);

        // When
        List<LlmModelEntity> result = repository.findByOrganizationId(ORGANIZATION_ID);

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrganizationIdAndProviderName_entityExists_returnNotEmpty() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2);
        LlmModelEntity entity3 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME);
        LlmModelEntity entity4 = createEntity(2L, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);
        repository.save(entity4);

        // When
        List<LlmModelEntity> result = repository.findByOrganizationIdAndProviderName(ORGANIZATION_ID, PROVIDER_NAME);

        // Then
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.stream().anyMatch(e -> e.getModelName().equals(MODEL_NAME)));
        Assert.assertTrue(result.stream().anyMatch(e -> e.getModelName().equals(MODEL_NAME_2)));
        Assert.assertTrue(result.stream().allMatch(e -> e.getProviderName().equals(PROVIDER_NAME)));
    }

    @Test
    public void findByOrganizationIdAndProviderName_entityNotExists_returnEmpty() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity);

        // When
        List<LlmModelEntity> result = repository.findByOrganizationIdAndProviderName(ORGANIZATION_ID, "non_existing");

        // Then
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void findByOrganizationIdAndProviderNameAndModelName_entityExists_returnNotEmpty() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        entity = repository.save(entity);

        // When
        Optional<LlmModelEntity> result = repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);

        // Then
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(entity.getId(), result.get().getId());
        Assert.assertEquals(entity.getModelName(), result.get().getModelName());
        Assert.assertEquals(entity.getProviderName(), result.get().getProviderName());
        Assert.assertEquals(entity.getOrganizationId(), result.get().getOrganizationId());
    }

    @Test
    public void findByOrganizationIdAndProviderNameAndModelName_entityNotExists_returnEmpty() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity);

        // When
        Optional<LlmModelEntity> result = repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, "non_existing");

        // Then
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void deleteAllBuiltinModelsByOrganizationIdAndProviderName_entityExists_returnCount() {
        // Given
        LlmModelEntity builtinModel1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        builtinModel1.setCustom(false);
        LlmModelEntity builtinModel2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2);
        builtinModel2.setCustom(false);
        LlmModelEntity customModel = createEntity(ORGANIZATION_ID, PROVIDER_NAME, "custom_model");
        customModel.setCustom(true);
        LlmModelEntity otherProviderModel = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME);
        otherProviderModel.setCustom(false);
        repository.save(builtinModel1);
        repository.save(builtinModel2);
        repository.save(customModel);
        repository.save(otherProviderModel);

        // When
        int deletedCount = repository.deleteAllBuiltinModelsByOrganizationIdAndProviderName(
                ORGANIZATION_ID, PROVIDER_NAME);

        // Then
        Assert.assertEquals(2, deletedCount);
        Assert.assertEquals(2, repository.count());
        Assert.assertFalse(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME).isPresent());
        Assert.assertFalse(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2).isPresent());
        Assert.assertTrue(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, "custom_model").isPresent());
        Assert.assertTrue(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME).isPresent());
    }

    @Test
    public void deleteAllBuiltinModelsByOrganizationIdAndProviderName_entityNotExists_returnZero() {
        // Given
        LlmModelEntity customModel = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        customModel.setCustom(true);
        repository.save(customModel);

        // When
        int deletedCount = repository.deleteAllBuiltinModelsByOrganizationIdAndProviderName(
                ORGANIZATION_ID, PROVIDER_NAME);

        // Then
        Assert.assertEquals(0, deletedCount);
        Assert.assertEquals(1, repository.count());
    }

    @Test
    public void deleteByOrganizationIdAndProviderNameAndModelName_entityExists_returnCount() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2);
        LlmModelEntity entity3 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME);
        repository.save(entity1);
        repository.save(entity2);
        repository.save(entity3);

        // When
        int deletedCount = repository.deleteByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);

        // Then
        Assert.assertEquals(1, deletedCount);
        Assert.assertEquals(2, repository.count());
        Assert.assertFalse(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME).isPresent());
        Assert.assertTrue(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2).isPresent());
        Assert.assertTrue(repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME).isPresent());
    }

    @Test
    public void deleteByOrganizationIdAndProviderNameAndModelName_entityNotExists_returnZero() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity);

        // When
        int deletedCount = repository.deleteByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, "non_existing");

        // Then
        Assert.assertEquals(0, deletedCount);
        Assert.assertEquals(1, repository.count());
    }

    @Test
    public void updateEnabledByOrganizationIdAndProviderNameAndModelName_entityExists_returnCount() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        entity1.setEnabled(false);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2);
        entity2.setEnabled(true);
        repository.save(entity1);
        repository.save(entity2);

        // When
        int updatedCount = repository.updateEnabledByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME, true);

        // Then
        Assert.assertEquals(1, updatedCount);
        Optional<LlmModelEntity> updatedEntity = repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        Assert.assertTrue(updatedEntity.isPresent());
        Assert.assertTrue(updatedEntity.get().getEnabled());

        // 验证其他实体没有被影响
        Optional<LlmModelEntity> otherEntity = repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME_2);
        Assert.assertTrue(otherEntity.isPresent());
        Assert.assertTrue(otherEntity.get().getEnabled());
    }

    @Test
    public void updateEnabledByOrganizationIdAndProviderNameAndModelName_entityNotExists_returnZero() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        repository.save(entity);

        // When
        int updatedCount = repository.updateEnabledByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, "non_existing", true);

        // Then
        Assert.assertEquals(0, updatedCount);
    }

    @Test
    public void save_normalEntity_saveSucceed() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);

        // When
        LlmModelEntity saved = repository.save(entity);

        // Then
        Assert.assertNotNull(saved.getId());
        Assert.assertEquals(entity.getModelName(), saved.getModelName());
        Assert.assertEquals(entity.getProviderName(), saved.getProviderName());
        Assert.assertEquals(entity.getOrganizationId(), saved.getOrganizationId());
        Assert.assertEquals(entity.getCreatorId(), saved.getCreatorId());
        Assert.assertEquals(entity.getLastModifierId(), saved.getLastModifierId());
        Assert.assertEquals(entity.getDisplayName(), saved.getDisplayName());
        Assert.assertEquals(entity.getModelType(), saved.getModelType());
        Assert.assertEquals(entity.getEnabled(), saved.getEnabled());
        Assert.assertEquals(entity.getDeprecated(), saved.getDeprecated());
        Assert.assertEquals(entity.getCustom(), saved.getCustom());
        Assert.assertEquals(entity.getFunctionCallingSupport(), saved.getFunctionCallingSupport());
        Assert.assertEquals(entity.getPropertiesJson(), saved.getPropertiesJson());
        Assert.assertEquals(entity.getSalt(), saved.getSalt());
        Assert.assertEquals(entity.getMaxToken(), saved.getMaxToken());
        Assert.assertEquals(entity.getUsedToken(), saved.getUsedToken());
    }

    @Test
    public void save_multipleEntities_saveSucceed() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME_2);

        // When
        repository.save(entity1);
        repository.save(entity2);

        // Then
        List<LlmModelEntity> all = repository.findAll();
        Assert.assertEquals(2, all.size());
    }

    @Test
    public void batchCreate_multipleEntities_createSucceed() {
        // Given
        LlmModelEntity entity1 = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        LlmModelEntity entity2 = createEntity(ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME_2);
        LlmModelEntity entity3 = createEntity(2L, PROVIDER_NAME, MODEL_NAME);
        List<LlmModelEntity> entities = List.of(entity1, entity2, entity3);

        // When
        List<LlmModelEntity> result = repository.batchCreate(entities);

        // Then
        Assert.assertEquals(3, result.size());
        Assert.assertEquals(3, repository.count());

        // 验证所有实体都被正确保存并分配了ID
        for (LlmModelEntity entity : result) {
            Assert.assertNotNull(entity.getId());
        }

        // 验证具体的实体内容
        Optional<LlmModelEntity> saved1 = repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        Assert.assertTrue(saved1.isPresent());
        Assert.assertEquals(entity1.getDisplayName(), saved1.get().getDisplayName());
        Assert.assertEquals(entity1.getModelType(), saved1.get().getModelType());

        Optional<LlmModelEntity> saved2 = repository.findByOrganizationIdAndProviderNameAndModelName(
                ORGANIZATION_ID, PROVIDER_NAME_2, MODEL_NAME_2);
        Assert.assertTrue(saved2.isPresent());
        Assert.assertEquals(entity2.getDisplayName(), saved2.get().getDisplayName());

        Optional<LlmModelEntity> saved3 = repository.findByOrganizationIdAndProviderNameAndModelName(
                2L, PROVIDER_NAME, MODEL_NAME);
        Assert.assertTrue(saved3.isPresent());
        Assert.assertEquals(entity3.getDisplayName(), saved3.get().getDisplayName());
    }

    @Test
    public void batchCreate_emptyList_returnEmpty() {
        // Given
        List<LlmModelEntity> entities = List.of();

        // When
        List<LlmModelEntity> result = repository.batchCreate(entities);

        // Then
        Assert.assertTrue(result.isEmpty());
        Assert.assertEquals(0, repository.count());
    }

    @Test
    public void batchCreate_singleEntity_createSucceed() {
        // Given
        LlmModelEntity entity = createEntity(ORGANIZATION_ID, PROVIDER_NAME, MODEL_NAME);
        List<LlmModelEntity> entities = List.of(entity);

        // When
        List<LlmModelEntity> result = repository.batchCreate(entities);

        // Then
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, repository.count());

        LlmModelEntity savedEntity = result.get(0);
        Assert.assertNotNull(savedEntity.getId());
        Assert.assertEquals(entity.getOrganizationId(), savedEntity.getOrganizationId());
        Assert.assertEquals(entity.getProviderName(), savedEntity.getProviderName());
        Assert.assertEquals(entity.getModelName(), savedEntity.getModelName());
        Assert.assertEquals(entity.getDisplayName(), savedEntity.getDisplayName());
        Assert.assertEquals(entity.getModelType(), savedEntity.getModelType());
        Assert.assertEquals(entity.getEnabled(), savedEntity.getEnabled());
        Assert.assertEquals(entity.getDeprecated(), savedEntity.getDeprecated());
        Assert.assertEquals(entity.getCustom(), savedEntity.getCustom());
        Assert.assertEquals(entity.getFunctionCallingSupport(), savedEntity.getFunctionCallingSupport());
        Assert.assertEquals(entity.getPropertiesJson(), savedEntity.getPropertiesJson());
        Assert.assertEquals(entity.getSalt(), savedEntity.getSalt());
        Assert.assertEquals(entity.getMaxToken(), savedEntity.getMaxToken());
        Assert.assertEquals(entity.getUsedToken(), savedEntity.getUsedToken());
    }

    private LlmModelEntity createEntity(Long organizationId, String providerName, String modelName) {
        LlmModelEntity entity = new LlmModelEntity();
        entity.setOrganizationId(organizationId);
        entity.setCreatorId(CREATOR_ID);
        entity.setLastModifierId(MODIFIER_ID);
        entity.setProviderName(providerName);
        entity.setModelName(modelName);
        entity.setDisplayName(DISPLAY_NAME);
        entity.setModelType(ModelType.CHAT);
        entity.setEnabled(true);
        entity.setDeprecated(false);
        entity.setCustom(false);
        entity.setFunctionCallingSupport(true);
        entity.setPropertiesJson(PROPERTIES_JSON);
        entity.setSalt(SALT);
        entity.setMaxToken(1000);
        entity.setUsedToken(0L);
        return entity;
    }
}
