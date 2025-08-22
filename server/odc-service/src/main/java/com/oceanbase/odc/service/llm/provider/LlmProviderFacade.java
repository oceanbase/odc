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
package com.oceanbase.odc.service.llm.provider;

import java.util.List;

import javax.annotation.Nullable;

import com.oceanbase.odc.service.llm.model.ProviderType;
import com.oceanbase.odc.service.llm.provider.template.ProviderTemplate;
import com.oceanbase.odc.service.llm.sdk.ChatModelWrapper;
import com.oceanbase.odc.service.llm.sdk.EmbeddingModelWrapper;
import com.oceanbase.odc.service.llm.sdk.StreamingChatModelWrapper;

public interface LlmProviderFacade<ModelCredential, ProviderCredential> {

    ProviderType getProviderType();

    ProviderTemplate getProviderTemplate();

    ModelCredential deserializeModelCredential(String credentialJson);

    ProviderCredential deserializeProviderCredential(String credentialJson);

    void maskProviderCredentialForDisplay(ProviderCredential providerCredential);

    void maskModelCredentialForDisplay(ModelCredential modelCredential);

    void decryptModelCredential(ModelCredential modelCredential);

    void decryptProviderCredential(ProviderCredential providerCredential);

    /**
     * 生成新的需要保存的模型凭证
     */
    ModelCredential generateNewModelCredential(ModelCredential savedCredential, ModelCredential newCredential);

    /**
     * 生成新的需要保存的供应商凭证
     */
    ProviderCredential generateNewProviderCredential(ProviderCredential savedCredential,
            ProviderCredential newCredential);

    List<ModelCredential> generateModelsByProviderCredential(@Nullable ProviderCredential credential);

    ChatModelWrapper generateChatModel(String modelName, ModelCredential credential);

    StreamingChatModelWrapper generateStreamingChatModel(String modelName, ModelCredential credential);

    EmbeddingModelWrapper generateEmbeddingModel(String modelName, ModelCredential credential);

}
