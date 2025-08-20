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
package com.oceanbase.odc.service.llm.provider.tongyi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.llm.model.Constants;
import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.AbstractProviderFacade;
import com.oceanbase.odc.service.llm.provider.template.ProviderTemplate.Models;
import com.oceanbase.odc.service.llm.sdk.ChatModelWrapper;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;
import com.oceanbase.odc.service.llm.util.MaskUtil;
import com.oceanbase.odc.service.llm.util.YamlUtil;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder;
import groovy.util.logging.Slf4j;

@Slf4j
@Component("provider-TONGYI")
public class TongyiProviderFacadeImpl extends AbstractProviderFacade<TongyiModelCredential, TongyiProviderCredential> {

    private static final String TEMPLATE_RESOURCE_PATH = "llm/providers/tongyi/provider.yaml";
    private static final String BAILIAN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    @Override
    protected String getTemplateResourcePath() {
        return TEMPLATE_RESOURCE_PATH;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.TONGYI;
    }

    @Override
    public TongyiModelCredential deserializeModelCredential(String credentialJson) {
        return JsonUtils.fromJson(credentialJson, TongyiModelCredential.class);
    }

    @Override
    public TongyiProviderCredential deserializeProviderCredential(String credentialJson) {
        return JsonUtils.fromJson(credentialJson, TongyiProviderCredential.class);
    }

    @Override
    public void maskProviderCredentialForDisplay(TongyiProviderCredential providerCredential) {
        providerCredential.setApiKey(MaskUtil.maskApiKey(providerCredential.getApiKey()));
    }

    @Override
    public void maskModelCredentialForDisplay(TongyiModelCredential modelCredential) {
        modelCredential.setApiKey(MaskUtil.maskApiKey(modelCredential.getApiKey()));
    }

    @Override
    public void decryptModelCredential(TongyiModelCredential modelCredential) {
        modelCredential.setApiKey(decrypt(modelCredential.getApiKey()));
    }

    @Override
    public void decryptProviderCredential(TongyiProviderCredential providerCredential) {
        providerCredential.setApiKey(decrypt(providerCredential.getApiKey()));
    }

    @Override
    public TongyiModelCredential generateNewModelCredential(TongyiModelCredential savedCredential,
            TongyiModelCredential newCredential) {
        if (newCredential.getApiKey() == null) {
            newCredential.setApiKey(savedCredential.getApiKey());
        }
        return newCredential;
    }

    @Override
    public TongyiProviderCredential generateNewProviderCredential(TongyiProviderCredential savedCredential,
            TongyiProviderCredential newCredential) {
        if (newCredential.getApiKey() == null) {
            newCredential.setApiKey(savedCredential.getApiKey());
        }
        return newCredential;
    }

    @Override
    public List<TongyiModelCredential> generateModelsByProviderCredential(
            @Nullable TongyiProviderCredential credential) {
        Models models = providerTemplate.get().getModels();
        if (models == null) {
            return null;
        }
        // 加载 CHAT 模型
        List<TongyiModelCredential> modelCredentials = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(models.getChat())) {
            modelCredentials.addAll(models.getChat().stream()
                    .map(chatModelPath -> YamlUtil.loadAllModelsFromResourceYaml(chatModelPath,
                            TongyiModelTemplateConverter::convertToTongyiCredential))
                    .flatMap(List::stream)
                    .map(m -> {
                        TongyiModelCredential tongyiModelCredential = (TongyiModelCredential) m;
                        if (credential != null) {
                            tongyiModelCredential.setApiKey(credential.getApiKey());
                        }
                        return tongyiModelCredential;
                    })
                    .toList());
        }
        // 加载 EMBEDDING 模型
        if (CollectionUtils.isNotEmpty(models.getEmbedding())) {
            modelCredentials.addAll(models.getEmbedding().stream()
                    .map(embeddingModelPath -> YamlUtil.loadAllModelsFromResourceYaml(embeddingModelPath,
                            TongyiModelTemplateConverter::convertToTongyiCredential))
                    .flatMap(List::stream)
                    .map(m -> {
                        TongyiModelCredential tongyiModelCredential = (TongyiModelCredential) m;
                        if (credential != null) {
                            tongyiModelCredential.setApiKey(credential.getApiKey());
                        }
                        return tongyiModelCredential;
                    })
                    .toList());
        }
        return modelCredentials;
    }

    @Override
    public ChatModelWrapper generateChatModel(String modelName, TongyiModelCredential credential) {
        OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .baseUrl(BAILIAN_API_URL);
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        if (credential.getTemperature() != null) {
            builder.temperature(Double.valueOf(credential.getTemperature()));
        }
        if (credential.getTopP() != null) {
            builder.topP(Double.valueOf(credential.getTopP()));
        }
        if (credential.getSeed() != null) {
            builder.seed(credential.getSeed());
        }
        if (credential.getRepetitionPenalty() != null) {
            builder.frequencyPenalty(Double.valueOf(credential.getRepetitionPenalty()));
        }
        return new ChatModelWrapper(builder.build(), credential);
    }

    @Override
    public StreamingChatModelWrapper generateStreamingChatModel(String modelName, TongyiModelCredential credential) {
        OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(modelName)
                .defaultRequestParameters(OpenAiChatRequestParameters.builder()
                        .customParameters(Map.of("enable_thinking", false)).build())
                .baseUrl(BAILIAN_API_URL);
        if (credential.getMaxToken() != null) {
            builder.maxTokens(credential.getMaxToken());
        }
        if (credential.getTemperature() != null) {
            builder.temperature(Double.valueOf(credential.getTemperature()));
        }
        if (credential.getTopP() != null) {
            builder.topP(Double.valueOf(credential.getTopP()));
        }
        if (credential.getSeed() != null) {
            builder.seed(credential.getSeed());
        }
        if (credential.getRepetitionPenalty() != null) {
            builder.frequencyPenalty(Double.valueOf(credential.getRepetitionPenalty()));
        }
        return new StreamingChatModelWrapper(builder.build(), credential);
    }

    @Override
    public EmbeddingModelWrapper generateEmbeddingModel(String model, TongyiModelCredential credential) {
        OpenAiEmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(credential.getApiKey())
                .modelName(model)
                .dimensions(Constants.DEFAULT_EMBEDDING_DIMENSION)
                .baseUrl(BAILIAN_API_URL)
                .build();
        return new EmbeddingModelWrapper(embeddingModel, credential);
    }

}
