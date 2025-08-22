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
package com.oceanbase.odc.service.llm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.llm.AIConfigEntity;
import com.oceanbase.odc.metadb.llm.AIConfigRepository;
import com.oceanbase.odc.metadb.llm.LlmModelEntity;
import com.oceanbase.odc.metadb.llm.LlmModelRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.llm.model.AIConfig;
import com.oceanbase.odc.service.llm.model.AIConfigUpdateEvent;
import com.oceanbase.odc.service.llm.util.AIConfigMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIConfigService {
    private static final String MINIMAL_METADB_VERSION = "4.3.3";
    private final AIConfigMapper aiConfigMapper = AIConfigMapper.INSTANCE;
    @Autowired(required = false)
    @Qualifier("vectordbDataSource")
    protected DataSource vectordbDataSource;
    @Autowired
    private AIConfigRepository aiConfigRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private LlmModelRepository llmModelRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public AIConfig getAIConfig() {
        return aiConfigRepository.findByOrganizationId(authenticationFacade.currentOrganizationId())
                .map(aiConfigMapper::entityToModel)
                .orElse(new AIConfig());
    }

    public AIConfig getAIConfigSkipPermissionCheck(Long organizationId) {
        return aiConfigRepository.findByOrganizationId(organizationId)
                .map(aiConfigMapper::entityToModel)
                .orElse(new AIConfig());
    }

    @Transactional(rollbackFor = Exception.class)
    public AIConfig setAIConfig(AIConfig aiConfig) throws SQLException {
        try (Connection conn = vectordbDataSource.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("show parameters like 'ob_vector_memory_limit_percentage'")) {
            if (!rs.next()) {
                throw new BadRequestException(ErrorCodes.VectorDBNotConfigured, new Object[] {MINIMAL_METADB_VERSION},
                        "please check the metadb version should be equal to or higher than " + MINIMAL_METADB_VERSION);
            }
            if (rs.getInt("value") <= 0) {
                throw new BadRequestException(ErrorCodes.VectorDBNotConfigured, new Object[] {MINIMAL_METADB_VERSION},
                        "when ob_vector_memory_limit_percentage = 0 or memstore_limit >= 85, vector index is not supported");
            }
        }
        Long orgId = authenticationFacade.currentOrganizationId();
        validateAIConfig(aiConfig);
        aiConfig.setOrganizationId(orgId);
        aiConfig.setLastModifierId(authenticationFacade.currentUserId());

        Optional<AIConfigEntity> existingEntityOpt = aiConfigRepository.findByOrganizationId(orgId);
        if (existingEntityOpt.isPresent()) {
            AIConfigEntity existingEntity = existingEntityOpt.get();
            AIConfigEntity updatedEntity = mergeNonNullFields(existingEntity, aiConfig);
            aiConfigRepository.updateByOrganizationId(orgId, updatedEntity);
        } else {
            // 不存在配置时，创建新的配置
            AIConfigEntity entity = aiConfigMapper.modelToEntity(aiConfig);
            aiConfigRepository.save(entity);
        }
        if (Objects.equals(Boolean.TRUE, aiConfig.getChatEnabled())
                || Objects.equals(Boolean.TRUE, aiConfig.getCopilotEnabled())
                        && aiConfig.getDefaultEmbeddingModel() != null) {
            eventPublisher.publishEvent(new AIConfigUpdateEvent(aiConfig));
        }
        return getAIConfig();
    }

    public List<AIConfig> listAllOrganizationConfigForEmbedding() {
        // 返回所有启用了 embedding 且开启了 chat 或 copilot 的配置
        return aiConfigRepository.findAll().stream()
                .filter(config -> (Objects.equals(Boolean.TRUE, config.getChatEnabled())
                        || Objects.equals(Boolean.TRUE, config.getCopilotEnabled()))
                        && config.getDefaultEmbeddingModel() != null)
                .map(aiConfigMapper::entityToModel)
                .toList();
    }

    /**
     * 将新配置中不为null的字段合并到现有实体中，model 配置除外
     */
    private AIConfigEntity mergeNonNullFields(AIConfigEntity existing, AIConfig newConfig) {
        if (newConfig.getAiEnabled() != null) {
            existing.setAiEnabled(newConfig.getAiEnabled());
        }
        if (newConfig.getChatEnabled() != null) {
            existing.setChatEnabled(newConfig.getChatEnabled());
        }
        if (newConfig.getCopilotEnabled() != null) {
            existing.setCopilotEnabled(newConfig.getCopilotEnabled());
        }
        if (newConfig.getCompletionEnabled() != null) {
            existing.setCompletionEnabled(newConfig.getCompletionEnabled());
        }
        if (StringUtils.isNotBlank(newConfig.getDefaultEmbeddingModel())) {
            existing.setDefaultEmbeddingModel(newConfig.getDefaultEmbeddingModel());
        }
        if (StringUtils.isNotBlank(newConfig.getDefaultLlmModel())) {
            existing.setDefaultLlmModel(newConfig.getDefaultLlmModel());
        }
        if (StringUtils.isNotBlank(newConfig.getDefaultChatModel())) {
            existing.setDefaultChatModel(newConfig.getDefaultChatModel());
        }
        return existing;
    }

    private void validateAIConfig(AIConfig aiConfig) {
        if (aiConfig.getDefaultLlmModel() != null) {
            String[] providerModel = parseModel(aiConfig.getDefaultLlmModel());
            validateModel(providerModel[0], providerModel[1], false);
        }
        // chat model 必须支持 function call
        if (aiConfig.getDefaultChatModel() != null) {
            String[] providerModel = parseModel(aiConfig.getDefaultChatModel());
            validateModel(providerModel[0], providerModel[1], true);
        }
        if (aiConfig.getDefaultEmbeddingModel() != null) {
            String[] providerModel = parseModel(aiConfig.getDefaultEmbeddingModel());
            validateModel(providerModel[0], providerModel[1], false);
        }
    }

    private String[] parseModel(String model) {
        int i = model.indexOf("/");
        if (i < 0) {
            throw new IllegalArgumentException("model is invalid");
        }
        return new String[] {model.substring(0, i), model.substring(i + 1)};
    }

    private void validateModel(String provider, String model, boolean requireFunctionCall) {
        LlmModelEntity entity = llmModelRepository.findByOrganizationIdAndProviderNameAndModelName(
                authenticationFacade.currentOrganizationId(), provider, model)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_LLM_MODEL, "name", model));
        if (!entity.getEnabled()) {
            throw new BadRequestException(String.format("model %s is not enabled", model));
        }
        if (entity.getDeprecated()) {
            throw new BadRequestException(String.format("model %s is deprecated", model));
        }
        if (requireFunctionCall && !entity.getFunctionCallingSupport()) {
            throw new BadRequestException(String.format("model %s does not support function call", model));
        }
    }

}
