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
package com.oceanbase.odc.service.llm.provider.doubao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.llm.model.Constants;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.AbstractProviderFacade;
import com.oceanbase.odc.service.llm.provider.ProviderCredential;
import com.oceanbase.odc.service.llm.sdk.ChatModelWrapper;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;
import com.oceanbase.odc.service.llm.util.MaskUtil;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/14
 */
@Component("provider-DOUBAO")
public class DoubaoProviderFacadeImpl extends AbstractProviderFacade<DoubaoModelCredential, ProviderCredential> {
    private static final String TEMPLATE_RESOURCE_PATH = "llm/providers/doubao/provider.yaml";

    @Override
    protected String getTemplateResourcePath() {
        return TEMPLATE_RESOURCE_PATH;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DOUBAO;
    }

    @Override
    public DoubaoModelCredential deserializeModelCredential(String credentialJson) {
        return JsonUtils.fromJson(credentialJson, DoubaoModelCredential.class);
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
    public void maskModelCredentialForDisplay(DoubaoModelCredential modelCredential) {
        modelCredential.setVolcAccessKeyId(MaskUtil.maskApiKey(modelCredential.getVolcAccessKeyId()));
        modelCredential.setVolcSecretAccessKey(MaskUtil.maskApiKey(modelCredential.getVolcSecretAccessKey()));
        modelCredential.setVolcApiKey(MaskUtil.maskApiKey(modelCredential.getVolcApiKey()));
    }

    @Override
    public void decryptModelCredential(DoubaoModelCredential modelCredential) {
        modelCredential.setVolcAccessKeyId(decrypt(modelCredential.getVolcAccessKeyId()));
        modelCredential.setVolcSecretAccessKey(decrypt(modelCredential.getVolcSecretAccessKey()));
        modelCredential.setVolcApiKey(decrypt(modelCredential.getVolcApiKey()));
    }

    @Override
    public void decryptProviderCredential(ProviderCredential providerCredential) {

    }

    @Override
    public List<DoubaoModelCredential> generateModelsByProviderCredential(ProviderCredential credential) {
        return List.of();
    }

    @Override
    public ChatModelWrapper generateChatModel(String modelName, DoubaoModelCredential credential) {
        if (!"api_key".equalsIgnoreCase(credential.getAuthMethod())) {
            throw new UnsupportedOperationException("Only api_key auth method is supported");
        }
        // 优先使用 endpoint_id
        String realModel =
                credential.getEndpointId() != null ? credential.getEndpointId() : modelName;
        OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(credential.getVolcApiKey())
                .modelName(realModel)
                .baseUrl(credential.getApiEndpointHost());
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        return new ChatModelWrapper(builder.build(), credential);
    }

    @Override
    public StreamingChatModelWrapper generateStreamingChatModel(String modelName,
            DoubaoModelCredential credential) {
        if (!"api_key".equalsIgnoreCase(credential.getAuthMethod())) {
            throw new UnsupportedOperationException("Only api_key auth method is supported");
        }
        // 优先使用 endpoint_id
        String realModel =
                credential.getEndpointId() != null ? credential.getEndpointId() : modelName;
        OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(credential.getVolcApiKey())
                .modelName(realModel)
                .baseUrl(credential.getApiEndpointHost());
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        return new StreamingChatModelWrapper(builder.build(), credential);
    }

    @Override
    public EmbeddingModelWrapper generateEmbeddingModel(String modelName, DoubaoModelCredential credential) {
        if (!"api_key".equalsIgnoreCase(credential.getAuthMethod())) {
            throw new UnsupportedOperationException("Only api_key auth method is supported");
        }
        // 优先使用 endpoint_id
        String realModel =
                credential.getEndpointId() != null ? credential.getEndpointId() : modelName;
        return new EmbeddingModelWrapper(OpenAiEmbeddingModel.builder()
                .apiKey(credential.getVolcApiKey())
                .modelName(realModel)
                .dimensions(Constants.DEFAULT_EMBEDDING_DIMENSION)
                .baseUrl(credential.getApiEndpointHost())
                .build(), credential);
    }

    @Override
    public DoubaoModelCredential generateNewModelCredential(DoubaoModelCredential savedCredential,
            DoubaoModelCredential newCredential) {
        if (newCredential.getVolcAccessKeyId() == null) {
            newCredential.setVolcAccessKeyId(savedCredential.getVolcAccessKeyId());
        }
        if (newCredential.getVolcSecretAccessKey() == null) {
            newCredential.setVolcSecretAccessKey(savedCredential.getVolcSecretAccessKey());
        }
        if (newCredential.getVolcApiKey() == null) {
            newCredential.setVolcApiKey(savedCredential.getVolcApiKey());
        }
        return newCredential;
    }

    @Override
    public ProviderCredential generateNewProviderCredential(ProviderCredential savedCredential,
            ProviderCredential newCredential) {
        throw new UnsupportedOperationException();
    }
}
