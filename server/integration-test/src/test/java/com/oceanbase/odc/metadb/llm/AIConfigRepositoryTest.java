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

import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * Test cases for {@link AIConfigRepository}
 *
 * @author AIConfigRepository测试类
 */
public class AIConfigRepositoryTest extends ServiceTestEnv {

    @Autowired
    private AIConfigRepository aiConfigRepository;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    private static final Long ORGANIZATION_ID = 1L;
    private static final Long LAST_MODIFIER_ID = 1L;

    @Before
    public void setUp() {
        // 清理测试数据
        aiConfigRepository.deleteAll();
    }

    @After
    public void tearDown() {
        // 清理测试数据
        aiConfigRepository.deleteAll();
    }

    @Test
    public void testFindByOrganizationId_ExistingRecord_ReturnsRecord() {
        // 准备测试数据
        AIConfigEntity entity = createTestAIConfigEntity();
        aiConfigRepository.save(entity);

        // 执行测试
        Optional<AIConfigEntity> result = aiConfigRepository.findByOrganizationId(ORGANIZATION_ID);

        // 验证结果
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(ORGANIZATION_ID, result.get().getOrganizationId());
        Assert.assertEquals(entity.getAiEnabled(), result.get().getAiEnabled());
        Assert.assertEquals(entity.getChatEnabled(), result.get().getChatEnabled());
        Assert.assertEquals(entity.getCopilotEnabled(), result.get().getCopilotEnabled());
        Assert.assertEquals(entity.getCompletionEnabled(), result.get().getCompletionEnabled());
        Assert.assertEquals(entity.getDefaultEmbeddingModel(), result.get().getDefaultEmbeddingModel());
        Assert.assertEquals(entity.getDefaultLlmModel(), result.get().getDefaultLlmModel());
        Assert.assertEquals(entity.getDefaultChatModel(), result.get().getDefaultChatModel());
    }

    @Test
    public void testFindByOrganizationId_NonExistingRecord_ReturnsEmpty() {
        // 执行测试
        Optional<AIConfigEntity> result = aiConfigRepository.findByOrganizationId(999L);

        // 验证结果
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testUpdateByOrganizationId_ExistingRecord_UpdatesRecord() {
        // 准备测试数据
        AIConfigEntity originalEntity = createTestAIConfigEntity();
        aiConfigRepository.save(originalEntity);

        // 创建更新数据
        AIConfigEntity updateEntity = createUpdatedAIConfigEntity();

        // 执行更新
        int updateCount = aiConfigRepository.updateByOrganizationId(ORGANIZATION_ID, updateEntity);

        // 验证更新结果
        Assert.assertEquals(1, updateCount);

        // 验证数据已更新
        Optional<AIConfigEntity> result = aiConfigRepository.findByOrganizationId(ORGANIZATION_ID);
        Assert.assertTrue(result.isPresent());
        AIConfigEntity updatedEntity = result.get();
        Assert.assertEquals(updateEntity.getAiEnabled(), updatedEntity.getAiEnabled());
        Assert.assertEquals(updateEntity.getChatEnabled(), updatedEntity.getChatEnabled());
        Assert.assertEquals(updateEntity.getCopilotEnabled(), updatedEntity.getCopilotEnabled());
        Assert.assertEquals(updateEntity.getCompletionEnabled(), updatedEntity.getCompletionEnabled());
        Assert.assertEquals(updateEntity.getDefaultEmbeddingModel(), updatedEntity.getDefaultEmbeddingModel());
        Assert.assertEquals(updateEntity.getDefaultLlmModel(), updatedEntity.getDefaultLlmModel());
        Assert.assertEquals(updateEntity.getDefaultChatModel(), updatedEntity.getDefaultChatModel());
    }

    @Test
    public void testUpdateByOrganizationId_NonExistingRecord_ReturnsZero() {
        // 创建更新数据
        AIConfigEntity updateEntity = createUpdatedAIConfigEntity();

        // 执行更新
        int updateCount = aiConfigRepository.updateByOrganizationId(999L, updateEntity);

        // 验证更新结果
        Assert.assertEquals(0, updateCount);
    }

    @Test
    public void testSaveAndFindById_SavesAndRetrievesRecord() {
        // 准备测试数据
        AIConfigEntity entity = createTestAIConfigEntity();

        // 保存数据
        AIConfigEntity savedEntity = aiConfigRepository.save(entity);

        // 验证保存结果
        Assert.assertNotNull(savedEntity.getId());
        Assert.assertNotNull(savedEntity.getCreateTime());
        Assert.assertNotNull(savedEntity.getUpdateTime());

        // 通过ID查找
        Optional<AIConfigEntity> foundEntity = aiConfigRepository.findById(savedEntity.getId());
        Assert.assertTrue(foundEntity.isPresent());
        Assert.assertEquals(savedEntity.getId(), foundEntity.get().getId());
        Assert.assertEquals(savedEntity.getOrganizationId(), foundEntity.get().getOrganizationId());
    }

    @Test
    public void testDeleteByOrganizationId_DeletesRecord() {
        // 准备测试数据
        AIConfigEntity entity = createTestAIConfigEntity();
        aiConfigRepository.save(entity);

        // 验证数据存在
        Optional<AIConfigEntity> result = aiConfigRepository.findByOrganizationId(ORGANIZATION_ID);
        Assert.assertTrue(result.isPresent());

        // 删除数据
        aiConfigRepository.deleteAll();

        // 验证数据已删除
        result = aiConfigRepository.findByOrganizationId(ORGANIZATION_ID);
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testUpdateByOrganizationId_WithNullValues_HandlesNullValues() {
        // 准备测试数据
        AIConfigEntity originalEntity = createTestAIConfigEntity();
        aiConfigRepository.save(originalEntity);

        // 创建包含null值的更新数据
        AIConfigEntity updateEntity = new AIConfigEntity();
        updateEntity.setAiEnabled(false);
        updateEntity.setChatEnabled(false);
        updateEntity.setCopilotEnabled(false);
        updateEntity.setCompletionEnabled(false);
        updateEntity.setDefaultEmbeddingModel(null);
        updateEntity.setDefaultLlmModel(null);
        updateEntity.setDefaultChatModel(null);

        // 执行更新
        int updateCount = aiConfigRepository.updateByOrganizationId(ORGANIZATION_ID, updateEntity);

        // 验证更新结果
        Assert.assertEquals(1, updateCount);

        // 验证数据已更新
        Optional<AIConfigEntity> result = aiConfigRepository.findByOrganizationId(ORGANIZATION_ID);
        Assert.assertTrue(result.isPresent());
        AIConfigEntity updatedEntity = result.get();
        Assert.assertEquals(false, updatedEntity.getAiEnabled());
        Assert.assertEquals(false, updatedEntity.getChatEnabled());
        Assert.assertEquals(false, updatedEntity.getCopilotEnabled());
        Assert.assertEquals(false, updatedEntity.getCompletionEnabled());
        Assert.assertNull(updatedEntity.getDefaultEmbeddingModel());
        Assert.assertNull(updatedEntity.getDefaultLlmModel());
        Assert.assertNull(updatedEntity.getDefaultChatModel());
    }

    /**
     * 创建测试用的AIConfigEntity
     */
    private AIConfigEntity createTestAIConfigEntity() {
        AIConfigEntity entity = new AIConfigEntity();
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setLastModifierId(LAST_MODIFIER_ID);
        entity.setAiEnabled(true);
        entity.setChatEnabled(true);
        entity.setCopilotEnabled(true);
        entity.setCompletionEnabled(true);
        entity.setDefaultEmbeddingModel("embedding-model-v1");
        entity.setDefaultLlmModel("generation-model-v1");
        entity.setDefaultChatModel("chat-model-v1");
        return entity;
    }

    /**
     * 创建更新测试用的AIConfigEntity
     */
    private AIConfigEntity createUpdatedAIConfigEntity() {
        AIConfigEntity entity = new AIConfigEntity();
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setLastModifierId(LAST_MODIFIER_ID);
        entity.setAiEnabled(false);
        entity.setChatEnabled(false);
        entity.setCopilotEnabled(false);
        entity.setCompletionEnabled(false);
        entity.setDefaultEmbeddingModel("embedding-model-v2");
        entity.setDefaultLlmModel("generation-model-v2");
        entity.setDefaultChatModel("chat-model-v2");
        return entity;
    }
}
