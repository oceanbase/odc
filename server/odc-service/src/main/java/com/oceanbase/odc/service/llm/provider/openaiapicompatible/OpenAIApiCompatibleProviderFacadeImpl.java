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
package com.oceanbase.odc.service.llm.provider.openaiapicompatible;

import java.util.List;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.AbstractProviderFacade;
import com.oceanbase.odc.service.llm.provider.ProviderCredential;
import com.oceanbase.odc.service.llm.util.MaskUtil;

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
@Component("provider-OPENAI_API_COMPATIBLE")
public class OpenAIApiCompatibleProviderFacadeImpl
        extends AbstractProviderFacade<OpenAIApiCompatibleModelCredential, ProviderCredential> {
    private static final String TEMPLATE_RESOURCE_PATH = "llm/providers/openai_api_compatible/provider.yaml";

    @Override
    protected String getTemplateResourcePath() {
        return TEMPLATE_RESOURCE_PATH;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.OPENAI_API_COMPATIBLE;
    }

    @Override
    public OpenAIApiCompatibleModelCredential deserializeModelCredential(String credentialJson) {
        return JsonUtils.fromJson(credentialJson, OpenAIApiCompatibleModelCredential.class);
    }

    @Override
    public ProviderCredential deserializeProviderCredential(String credentialJson) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void maskProviderCredentialForDisplay(ProviderCredential providerCredential) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void maskModelCredentialForDisplay(OpenAIApiCompatibleModelCredential modelCredential) {
        modelCredential.setApiKey(MaskUtil.maskApiKey(modelCredential.getApiKey()));
    }

    @Override
    public void decryptModelCredential(OpenAIApiCompatibleModelCredential modelCredential) {
        modelCredential.setApiKey(decrypt(modelCredential.getApiKey()));
    }

    @Override
    public void decryptProviderCredential(ProviderCredential providerCredential) {

    }

    @Override
    public OpenAIApiCompatibleModelCredential generateNewModelCredential(
            OpenAIApiCompatibleModelCredential savedCredential, OpenAIApiCompatibleModelCredential newCredential) {
        if (newCredential.getApiKey() == null) {
            newCredential.setApiKey(savedCredential.getApiKey());
        }
        return newCredential;
    }

    @Override
    public ProviderCredential generateNewProviderCredential(ProviderCredential savedCredential,
            ProviderCredential newCredential) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<OpenAIApiCompatibleModelCredential> generateModelsByProviderCredential(ProviderCredential credential) {
        return List.of();
    }

    @Override
    public ChatModel generateChatModel(String modelName, OpenAIApiCompatibleModelCredential credential) {
        String realModel = credential.getEndpointModelName() != null ? credential.getEndpointModelName() : modelName;
        OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(realModel)
                .baseUrl(credential.getEndpointUrl());
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        return builder.build();
    }

    @Override
    public StreamingChatModel generateStreamingChatModel(String modelName,
            OpenAIApiCompatibleModelCredential credential) {
        String realModel = credential.getEndpointModelName() != null ? credential.getEndpointModelName() : modelName;
        OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(realModel)
                .baseUrl(credential.getEndpointUrl());
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        return builder.build();
    }

    @Override
    public EmbeddingModel generateEmbeddingModel(String modelName, OpenAIApiCompatibleModelCredential credential) {
        String realModel = credential.getEndpointModelName() != null ? credential.getEndpointModelName() : modelName;
        return OpenAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(realModel)
                .baseUrl(credential.getEndpointUrl())
                .build();
    }

}
