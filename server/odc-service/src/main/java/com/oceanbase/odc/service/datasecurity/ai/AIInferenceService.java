/*
 * Copyright (c) 2025 OceanBase.
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
package com.oceanbase.odc.service.datasecurity.ai;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonBoolean;
import com.openai.core.JsonValue;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

@Service
public class AIInferenceService {

    private final AIConfig     aiConfig;
    private final OpenAIClient openAIClient;

    // 直接注入 AIConfig 和 OpenAIClient 两个Bean
    public AIInferenceService(AIConfig aiConfig, OpenAIClient openAIClient) {
        this.aiConfig = aiConfig;
        this.openAIClient = openAIClient;
    }

    /**
     * 使用系统提示词和用户提示词分别调用AI服务
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI响应
     */
    public ChatCompletion chat(String systemPrompt, String userPrompt) {

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .model(aiConfig.getModel())
                .temperature(aiConfig.getTemperature())
                .topP(aiConfig.getTopP())
                .additionalBodyProperties(aiConfig.loadAdditionalParams())
                .build();
            return openAIClient.chat().completions().create(params);
        } catch (Exception e) {
            throw new RuntimeException("调用AI服务失败: " + e.getMessage(), e);
        }
    }
}