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

    private final AIConfig aiConfig;
    private final OpenAIClient openAIClient;

    public AIInferenceService(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
        // 根据AIConfig创建OpenAIClient
        this.openAIClient = aiConfig.openAIClient();
    }

    public ChatCompletion chat(String prompt) {
        //Map<String, JsonValue> bodyParams = new HashMap<>();
        //bodyParams.put("enable_thinking", JsonBoolean.from(false));
        String model = aiConfig.getModel();
        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)
                    .model(model)
                    //.additionalBodyProperties(bodyParams)
                    .build();
            return openAIClient.chat().completions().create(params);
        } catch (Exception e) {
            throw new RuntimeException("调用阿里云AI服务失败: " + e.getMessage(), e);
        }
    }
}
