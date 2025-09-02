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

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

@Service
public class AIInferenceService {

    private final AIConfig aiConfig;
    private final Optional<OpenAIClient> openAIClient;

    // 注入 AIConfig 和可选的 OpenAIClient Bean
    public AIInferenceService(AIConfig aiConfig, Optional<OpenAIClient> openAIClient) {
        this.aiConfig = aiConfig;
        this.openAIClient = openAIClient;
    }

    /**
     * 检查AI功能是否可用
     * @throws IllegalStateException 如果AI功能不可用
     */
    private void checkAIAvailability() {
        if (!aiConfig.isEnabled()) {
            throw new IllegalStateException("AI功能未启用。请联系管理员启用AI功能。");
        }
        if (!aiConfig.isAIAvailable()) {
            throw new IllegalStateException("AI功能配置不完整。请联系管理员配置AI相关参数。");
        }
        if (!openAIClient.isPresent()) {
            throw new IllegalStateException("AI客户端未初始化。请检查AI配置并重启服务。");
        }
    }

    /**
     * 使用系统提示词和用户提示词分别调用AI服务
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return AI响应
     * @throws IllegalStateException 如果AI功能不可用
     */
    public ChatCompletion chat(String systemPrompt, String userPrompt) {
        // 检查AI功能可用性
        checkAIAvailability();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .model(aiConfig.getModel())
                .temperature(aiConfig.getTemperature())
                .topP(aiConfig.getTopP())
                .additionalBodyProperties(aiConfig.loadAdditionalParams())
                .build();
            return openAIClient.get().chat().completions().create(params);
        } catch (Exception e) {
            throw new RuntimeException("调用AI服务失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查AI功能是否可用（不抛出异常）
     * @return true if AI功能可用
     */
    public boolean isAIAvailable() {
        return aiConfig.isEnabled() && aiConfig.isAIAvailable() && openAIClient.isPresent();
    }
}