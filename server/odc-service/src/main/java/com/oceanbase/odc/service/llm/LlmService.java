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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.llm.LlmModelEntity;
import com.oceanbase.odc.metadb.llm.LlmModelRepository;
import com.oceanbase.odc.metadb.llm.LlmProviderEntity;
import com.oceanbase.odc.metadb.llm.LlmProviderRepository;
import com.oceanbase.odc.service.encryption.EncryptionFacade;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.llm.model.LlmModel;
import com.oceanbase.odc.service.llm.model.LlmProvider;
import com.oceanbase.odc.service.llm.model.ModelCredentialDto;
import com.oceanbase.odc.service.llm.model.ModelType;
import com.oceanbase.odc.service.llm.model.ProviderCredentialDto;
import com.oceanbase.odc.service.llm.model.ProviderDescription;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.LlmProviderFacade;
import com.oceanbase.odc.service.llm.provider.LlmProviderFacades;
import com.oceanbase.odc.service.llm.provider.ModelCredential;
import com.oceanbase.odc.service.llm.provider.ProviderCredential;
import com.oceanbase.odc.service.llm.provider.template.ProviderTemplate;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;
import com.oceanbase.odc.service.llm.util.LlmModelMapper;
import com.oceanbase.odc.service.llm.util.LlmProviderMapper;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.Response;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LlmService {

    private final LlmModelMapper modelMapper = LlmModelMapper.INSTANCE;
    private final LlmProviderMapper providerMapper = LlmProviderMapper.INSTANCE;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private LlmProviderRepository llmProviderRepository;
    @Autowired
    private LlmModelRepository llmModelRepository;
    @Autowired
    private EncryptionFacade encryptionFacade;
    @Autowired
    private LlmProviderFacades providerFacades;

    public List<ProviderDescription> getProviders() {
        Map<String, LlmProviderEntity> providers = llmProviderRepository
                .findAllByOrganizationId(authenticationFacade.currentOrganizationId())
                .stream()
                .collect(Collectors.toMap(LlmProviderEntity::getName, Function.identity()));

        Map<String, Long> modelCounts = llmModelRepository
                .countModelsByProviderNameInOrganization(authenticationFacade.currentOrganizationId())
                .stream()
                .collect(Collectors.toMap(o -> (String) o[0], o -> (long) o[1]));

        return providerFacades.getProviderFacades().values().stream()
                .map(facade -> {
                    ProviderTemplate template = facade.getProviderTemplate();
                    LlmProviderEntity entity = providers.get(template.getProvider());
                    ProviderDescription detail = ProviderDescription.from(template);
                    if (entity != null) {
                        detail.setDescription(entity.getDescription());
                        if (entity.getPropertiesJson() != null) {
                            detail.setCredentialConfigured(true);
                        }
                    }
                    if (modelCounts.containsKey(template.getProvider())) {
                        detail.setModelCounts(modelCounts.get(template.getProvider()));
                    }
                    return detail;
                })
                .collect(Collectors.toList());
    }

    public List<LlmModel> getModels(String provider) {
        return llmModelRepository
                .findByOrganizationIdAndProviderName(authenticationFacade.currentOrganizationId(),
                        provider.toUpperCase())
                .stream()
                .map(modelMapper::entityToModel)
                .collect(Collectors.toList());
    }

    public ProviderCredentialDto getProviderCredential(String provider) {
        Optional<LlmProviderEntity> providerEntity = llmProviderRepository
                .findByOrganizationIdAndName(authenticationFacade.currentOrganizationId(), provider);
        if (providerEntity.isEmpty()) {
            return null;
        }
        LlmProviderFacade<ModelCredential, ProviderCredential> facade =
                providerFacades.getProviderFacade(providerEntity.get().getName());
        String propertiesJson = providerEntity.get().getPropertiesJson();
        String decrypted = decypt(propertiesJson, providerEntity.get().getOrganizationId(),
                providerEntity.get().getSalt());

        ProviderCredential credential = facade.deserializeProviderCredential(decrypted);
        facade.maskProviderCredentialForDisplay(credential);
        ProviderCredentialDto result = new ProviderCredentialDto();
        result.setProvider(ProviderType.valueOf(provider));
        result.setCredential(credential);
        result.setDescription(providerEntity.get().getDescription());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public String setProviderCredential(ProviderCredentialDto request) {
        LlmProviderFacade<ModelCredential, ProviderCredential> facade =
                providerFacades.getProviderFacade(request.getProvider());
        ProviderCredential credential = request.getCredential();
        facade.decryptProviderCredential(credential);

        // 如果表中配置了供应商，需要判断是否需要使用已存储的敏感信息
        Optional<LlmProviderEntity> entity = llmProviderRepository
                .findByOrganizationIdAndName(authenticationFacade.currentOrganizationId(),
                        request.getProvider().name());
        if (entity.isPresent()) {
            String propertiesJson = entity.get().getPropertiesJson();
            String decrypted = decypt(propertiesJson, entity.get().getOrganizationId(), entity.get().getSalt());
            ProviderCredential savedCredential = facade.deserializeProviderCredential(decrypted);
            credential = facade.generateNewProviderCredential(savedCredential, credential);
            String encrypted = encrypt(JsonUtils.toJson(credential),
                    authenticationFacade.currentOrganizationId(), entity.get().getSalt());
            llmProviderRepository.updatePropertiesJsonByOrganizationIdAndName(
                    authenticationFacade.currentOrganizationId(), request.getProvider().name(), encrypted);
        } else {
            String salt = encryptionFacade.generateSalt();
            LlmProvider newProvider = new LlmProvider();
            newProvider.setOrganizationId(authenticationFacade.currentOrganizationId());
            newProvider.setCreatorId(authenticationFacade.currentUserId());
            newProvider.setLastModifierId(authenticationFacade.currentUserId());
            newProvider.setName(request.getProvider().name());
            newProvider.setSalt(salt);
            newProvider.setPropertiesJson(encrypt(JsonUtils.toJson(credential),
                    authenticationFacade.currentOrganizationId(), salt));
            llmProviderRepository.save(providerMapper.modelToEntity(newProvider));
        }

        List<ModelCredential> models = facade.generateModelsByProviderCredential(credential);
        if (CollectionUtils.isNotEmpty(models)) {
            ModelCredential credential0 = models.get(0);
            if (credential0.getModelType() == ModelType.CHAT) {
                validateChat(facade, credential0.getModelName(), credential0);
            } else {
                validateEmbedding(facade, credential0.getModelName(), credential0);
            }
            log.info("Validate model {} success, provider: {}", credential0.getModelName(),
                    request.getProvider().name());

            llmModelRepository.deleteAllBuiltinModelsByOrganizationIdAndProviderName(
                    authenticationFacade.currentOrganizationId(), request.getProvider().name());

            // 批量写
            llmModelRepository.batchCreate(models.stream().map(m -> {
                LlmModel model = buildLlmModel(request.getProvider().name(), m.getModelName(), m.getModelType(), m,
                        encryptionFacade.generateSalt(), false);
                return modelMapper.modelToEntity(model);
            }).collect(Collectors.toList()));
        }
        return "success";
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteProviderCredential(ProviderCredentialDto request) {
        llmProviderRepository.updatePropertiesJsonByOrganizationIdAndName(authenticationFacade.currentOrganizationId(),
                request.getProvider().name(), null);
        llmModelRepository.deleteAllBuiltinModelsByOrganizationIdAndProviderName(
                authenticationFacade.currentOrganizationId(), request.getProvider().name());
        return "success";
    }

    public ModelCredentialDto getModelCredential(@NotBlank String provider, @NotBlank String model) {
        Optional<LlmModelEntity> modelEntity = llmModelRepository.findByOrganizationIdAndProviderNameAndModelName(
                authenticationFacade.currentOrganizationId(), provider, model);
        if (modelEntity.isEmpty()) {
            return null;
        }
        LlmProviderFacade<ModelCredential, ProviderCredential> facade = providerFacades.getProviderFacade(provider);
        String propertiesJson = modelEntity.get().getPropertiesJson();
        String decrypted = decypt(propertiesJson, modelEntity.get().getOrganizationId(), modelEntity.get().getSalt());
        ModelCredential credential = facade.deserializeModelCredential(decrypted);
        facade.maskModelCredentialForDisplay(credential);
        ModelCredentialDto result = new ModelCredentialDto();
        result.setProvider(ProviderType.valueOf(provider));
        result.setModel(model);
        result.setCredential(credential);
        result.setDescription(modelEntity.get().getDescription());
        result.setType(modelEntity.get().getModelType());
        return result;
    }

    public ModelCredential getModelCredentialSkipPermissionCheck(String provider, String model, Long organizationId) {
        Optional<LlmModelEntity> modelEntity = llmModelRepository.findByOrganizationIdAndProviderNameAndModelName(
                organizationId, provider, model);
        if (modelEntity.isEmpty()) {
            return null;
        }
        if (!modelEntity.get().getEnabled()) {
            log.warn("Model {} is disabled, provider: {}, currentOrganizationId: {}", model, provider, organizationId);
            return null;
        }
        LlmProviderFacade<ModelCredential, ProviderCredential> facade = providerFacades.getProviderFacade(provider);
        String propertiesJson = modelEntity.get().getPropertiesJson();
        String decrypted = decypt(propertiesJson, modelEntity.get().getOrganizationId(), modelEntity.get().getSalt());
        return facade.deserializeModelCredential(decrypted);
    }

    @Transactional(rollbackFor = Exception.class)
    public String setModelCredential(ModelCredentialDto request) {
        LlmProviderFacade<ModelCredential, ProviderCredential> facade =
                providerFacades.getProviderFacade(request.getProvider());
        ModelCredential credential = request.getCredential();
        facade.decryptModelCredential(credential);

        Optional<LlmModelEntity> modelEntity = llmModelRepository.findByOrganizationIdAndProviderNameAndModelName(
                authenticationFacade.currentOrganizationId(), request.getProvider().name(), request.getModel());
        if (modelEntity.isPresent()) {
            String propertiesJson = modelEntity.get().getPropertiesJson();
            String decrypted =
                    decypt(propertiesJson, modelEntity.get().getOrganizationId(), modelEntity.get().getSalt());
            ModelCredential savedCredential = facade.deserializeModelCredential(decrypted);
            credential = facade.generateNewModelCredential(savedCredential, credential);

            if (request.getType() == ModelType.CHAT) {
                validateChat(facade, request.getModel(), credential);
            } else {
                validateEmbedding(facade, request.getModel(), credential);
            }
            log.info("Validate model success, provider: {}, model: {}", request.getProvider().name(),
                    request.getModel());

            String encrypted = encrypt(JsonUtils.toJson(credential),
                    authenticationFacade.currentOrganizationId(), modelEntity.get().getSalt());
            llmModelRepository.updatePropertiesJsonAndDescriptionByOrganizationAndProviderAndModel(
                    authenticationFacade.currentOrganizationId(), request.getProvider().name(), request.getModel(),
                    encrypted, request.getDescription());
        } else {
            if (request.getType() == ModelType.CHAT) {
                validateChat(facade, request.getModel(), credential);
            } else {
                validateEmbedding(facade, request.getModel(), credential);
            }
            log.info("Validate model success, provider: {}, model: {}", request.getProvider().name(),
                    request.getModel());

            LlmModel newModel =
                    buildLlmModel(request.getProvider().name(), request.getModel(), request.getType(),
                            credential, encryptionFacade.generateSalt(), true);
            newModel.setDescription(request.getDescription());
            llmModelRepository.deleteByOrganizationIdAndProviderNameAndModelName(
                    authenticationFacade.currentOrganizationId(), request.getProvider().name(), request.getModel());

            llmModelRepository.save(modelMapper.modelToEntity(newModel));
        }
        return "success";
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteModelCredential(ModelCredentialDto request) {
        llmModelRepository.deleteByOrganizationIdAndProviderNameAndModelName(
                authenticationFacade.currentOrganizationId(), request.getProvider().name(), request.getModel());
        return "success";
    }

    @Transactional(rollbackFor = Exception.class)
    public String setModelEnabled(String provider, String model, boolean enabled) {
        llmModelRepository.findByOrganizationIdAndProviderNameAndModelName(
                authenticationFacade.currentOrganizationId(), provider, model)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_LLM_MODEL, "model", model));
        llmModelRepository.updateEnabledByOrganizationIdAndProviderNameAndModelName(
                authenticationFacade.currentOrganizationId(), provider, model, enabled);
        return "success";
    }

    @Transactional(rollbackFor = Exception.class)
    public String setProviderDescription(String provider, String description) {
        LlmProviderEntity entity = llmProviderRepository.findByOrganizationIdAndName(
                authenticationFacade.currentOrganizationId(), provider)
                .orElse(null);
        if (entity == null) {
            entity = new LlmProviderEntity();
            entity.setCreatorId(authenticationFacade.currentUserId());
            entity.setLastModifierId(authenticationFacade.currentUserId());
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            entity.setName(provider);
            entity.setDescription(description);
            entity.setSalt(encryptionFacade.generateSalt());
            llmProviderRepository.saveAndFlush(entity);
        } else {
            llmProviderRepository.updateDescriptionByOrganizationIdAndName(
                    authenticationFacade.currentOrganizationId(), provider, description);
        }

        return "success";
    }

    private void validateChat(LlmProviderFacade facade, String modelName, ModelCredential credential) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> throwable = new AtomicReference<>();
            StreamingChatModelWrapper chatModel = facade.generateStreamingChatModel(modelName, credential);
            chatModel.model().chat("1+1=?", new SimpleStreamingChatResponseHandler(latch, throwable));
            boolean invokeSucceed = latch.await(30, TimeUnit.SECONDS);
            if (throwable.get() != null) {
                throw throwable.get();
            }
            if (!invokeSucceed) {
                throw new RuntimeException(String.format("Invoke model %s timeout", modelName));
            }
        } catch (Throwable e) {
            log.warn("Fail to validate model, please check the credential", e);
            throw new BadRequestException(
                    "Fail to validate model, please check the credential, reason: " + e.getMessage());
        }
    }

    private void validateEmbedding(LlmProviderFacade facade, String modelName, ModelCredential credential) {
        try {
            EmbeddingModelWrapper embeddingModel = facade.generateEmbeddingModel(modelName, credential);
            Response<Embedding> resp = embeddingModel.model().embed("hello");
            if (log.isDebugEnabled()) {
                log.debug("Response from validate model {}: {}", modelName, resp);
            }
        } catch (Exception e) {
            log.warn("Fail to validate model, please check the credential", e);
            throw new BadRequestException(
                    "Fail to validate model, please check the credential, reason: " + e.getMessage());
        }
    }

    private LlmModel buildLlmModel(String provider, String model, ModelType type, ModelCredential credential,
            String salt, boolean isCustomed) {
        return LlmModel.builder()
                .organizationId(authenticationFacade.currentOrganizationId())
                .creatorId(authenticationFacade.currentUserId())
                .lastModifierId(authenticationFacade.currentUserId())
                .providerName(provider)
                .modelName(model)
                .displayName(model)
                .modelType(type)
                .enabled(true)
                .deprecated(credential.isDeprecated())
                .custom(isCustomed)
                .functionCallingSupport(credential.isSupportFunctionCalling())
                .maxToken(credential.getMaxToken())
                .usedToken(0L)
                .propertiesJson(
                        encrypt(JsonUtils.toJson(credential), authenticationFacade.currentOrganizationId(), salt))
                .salt(salt)
                .build();
    }

    private String encrypt(String content, Long organizationId, String salt) {
        return encryptionFacade.organizationEncryptor(organizationId, salt).encrypt(content);
    }

    private String decypt(String content, Long organizationId, String salt) {
        return encryptionFacade.organizationEncryptor(organizationId, salt).decrypt(content);
    }

    private static class SimpleStreamingChatResponseHandler implements StreamingChatResponseHandler {
        private final CountDownLatch latch;
        private final AtomicReference<Throwable> throwable;

        SimpleStreamingChatResponseHandler(CountDownLatch latch, AtomicReference<Throwable> throwable) {
            this.latch = latch;
            this.throwable = throwable;
        }

        @Override
        public void onPartialResponse(String partialResponse) {
            latch.countDown();
        }

        @Override
        public void onCompleteResponse(ChatResponse completeResponse) {
            latch.countDown();
        }

        @Override
        public void onError(Throwable error) {
            throwable.set(error);
            latch.countDown();
        }
    }

}
