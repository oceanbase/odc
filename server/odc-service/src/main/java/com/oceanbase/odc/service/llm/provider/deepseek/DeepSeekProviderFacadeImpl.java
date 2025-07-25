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
package com.oceanbase.odc.service.llm.provider.deepseek;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.AbstractProviderFacade;
import com.oceanbase.odc.service.llm.provider.template.ProviderTemplate.Models;
import com.oceanbase.odc.service.llm.util.MaskUtil;
import com.oceanbase.odc.service.llm.util.YamlUtil;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/14
 */
@Component("provider-DEEPSEEK")
public class DeepSeekProviderFacadeImpl
        extends AbstractProviderFacade<DeepSeekModelCredential, DeepSeekProviderCredential> {
    private static final String TEMPLATE_RESOURCE_PATH = "llm/providers/deepseek/provider.yaml";

    @Override
    protected String getTemplateResourcePath() {
        return TEMPLATE_RESOURCE_PATH;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DEEPSEEK;
    }

    @Override
    public DeepSeekModelCredential deserializeModelCredential(String credentialJson) {
        return JsonUtils.fromJson(credentialJson, DeepSeekModelCredential.class);
    }

    @Override
    public DeepSeekProviderCredential deserializeProviderCredential(String credentialJson) {
        return JsonUtils.fromJson(credentialJson, DeepSeekProviderCredential.class);
    }

    @Override
    public void maskProviderCredentialForDisplay(DeepSeekProviderCredential providerCredential) {
        providerCredential.setApiKey(MaskUtil.maskApiKey(providerCredential.getApiKey()));
    }

    @Override
    public void maskModelCredentialForDisplay(DeepSeekModelCredential modelCredential) {
        modelCredential.setApiKey(MaskUtil.maskApiKey(modelCredential.getApiKey()));
    }

    @Override
    public void decryptModelCredential(DeepSeekModelCredential modelCredential) {

    }

    @Override
    public void decryptProviderCredential(DeepSeekProviderCredential providerCredential) {
        providerCredential.setApiKey(decrypt(providerCredential.getApiKey()));
    }

    @Override
    public List<DeepSeekModelCredential> generateModelsByProviderCredential(
            @Nullable DeepSeekProviderCredential credential) {
        Models models = providerTemplate.get().getModels();
        if (models == null) {
            return null;
        }
        // 加载 CHAT 模型
        List<DeepSeekModelCredential> modelCredentials = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(models.getChat())) {
            modelCredentials.addAll(models.getChat().stream()
                    .map(chatModelPath -> YamlUtil.loadAllModelsFromResourceYaml(chatModelPath,
                            DeepSeekModelTemplateConverter::convertToDeepSeekCredential))
                    .flatMap(List::stream)
                    .map(m -> {
                        DeepSeekModelCredential deepSeekModelCredential = (DeepSeekModelCredential) m;
                        if (credential != null) {
                            deepSeekModelCredential.setApiKey(credential.getApiKey());
                            deepSeekModelCredential.setEndpointUrl(credential.getEndpointUrl());
                        }
                        return deepSeekModelCredential;
                    })
                    .toList());
        }
        // 加载 EMBEDDING 模型
        if (CollectionUtils.isNotEmpty(models.getEmbedding())) {
            modelCredentials.addAll(models.getEmbedding().stream()
                    .map(embeddingModelPath -> YamlUtil.loadAllModelsFromResourceYaml(embeddingModelPath,
                            DeepSeekModelTemplateConverter::convertToDeepSeekCredential))
                    .flatMap(List::stream)
                    .map(m -> {
                        DeepSeekModelCredential deepSeekModelCredential = (DeepSeekModelCredential) m;
                        if (credential != null) {
                            deepSeekModelCredential.setApiKey(credential.getApiKey());
                            deepSeekModelCredential.setEndpointUrl(credential.getEndpointUrl());
                        }
                        return deepSeekModelCredential;
                    })
                    .toList());
        }
        return modelCredentials;
    }

    @Override
    public ChatModel generateChatModel(String modelName, DeepSeekModelCredential credential) {
        OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .baseUrl(credential.getEndpointUrl());
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel generateStreamingChatModel(String modelName, DeepSeekModelCredential credential) {
        OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .baseUrl(credential.getEndpointUrl());
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        return builder.build();
    }

    @Override
    public EmbeddingModel generateEmbeddingModel(String modelName, DeepSeekModelCredential credential) {
        return OpenAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .baseUrl(credential.getEndpointUrl())
                .build();
    }

    @Override
    public DeepSeekModelCredential generateNewModelCredential(DeepSeekModelCredential savedCredential,
            DeepSeekModelCredential newCredential) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeepSeekProviderCredential generateNewProviderCredential(DeepSeekProviderCredential savedCredential,
            DeepSeekProviderCredential newCredential) {
        if (newCredential.getApiKey() == null) {
            newCredential.setApiKey(savedCredential.getApiKey());
        }
        return newCredential;
    }
}
