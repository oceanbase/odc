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

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * @author fenyf
 * @date 2025/8/10 12:41
 */
@Service
public class AIInferenceService {

    private final AIConfig aiConfig;
    private final Optional<OpenAIClient> openAIClient;

    public AIInferenceService(AIConfig aiConfig, Optional<OpenAIClient> openAIClient) {
        this.aiConfig = aiConfig;
        this.openAIClient = openAIClient;
    }

    private void checkAIAvailability() {
        if (!aiConfig.isEnabled()) {
            throw new BadRequestException(ErrorCodes.AIServiceNotAvailable, new Object[] {"AI service is not enabled"},
                    "AI service is not enabled. Please contact administrator to enable AI service.");
        }
        if (!aiConfig.isAIAvailable()) {
            throw new BadRequestException(ErrorCodes.AIConfigurationIncomplete,
                    new Object[] {"AI configuration is incomplete"},
                    "AI configuration is incomplete. Please contact administrator to configure AI parameters.");
        }
        if (!openAIClient.isPresent()) {
            throw new BadRequestException(ErrorCodes.AIClientNotInitialized,
                    new Object[] {"AI client is not initialized"},
                    "AI client is not initialized. Please check AI configuration and restart service.");
        }
    }

    public ChatCompletion chat(String systemPrompt, String userPrompt) {
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
            throw new BadRequestException(ErrorCodes.AIInferenceServiceError, new Object[] {e.getMessage()},
                    "Failed to call AI inference service: " + e.getMessage(), e);
        }
    }

    public boolean isAIAvailable() {
        return aiConfig.isEnabled() && aiConfig.isAIAvailable() && openAIClient.isPresent();
    }
}
