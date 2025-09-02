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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonBoolean;
import com.openai.core.JsonNumber;
import com.openai.core.JsonValue;

import lombok.Data;

/**
 * AI功能配置类，支持从系统配置中动态读取配置
 */
@Data
@Component
public class AIConfig {
    @Value("${odc.ai.enabled:false}")
    private boolean enabled;

    @Value("${odc.ai.api-key:}")
    private String apiKey;

    @Value("${odc.ai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Value("${odc.ai.model:gpt-3.5-turbo}")
    private String model;

    // 硬编码超时和重试配置
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_RETRIES = 3;

    private Boolean enableThinking = AIParam.DEFAULT_ENABLE_THINKING;

    private Double temperature = AIParam.DEFAULT_TEMPERATURE;

    private Double topP = AIParam.DEFAULT_TOP_P;

    private Integer topK = AIParam.DEFAULT_TOP_K;

    private Integer minP = AIParam.DEFAULT_MIN_P;

    public Map<String, JsonValue> loadAdditionalParams() {
        Map<String, JsonValue> params = new HashMap<>();
        params.put("enable_thinking", JsonBoolean.from(this.enableThinking));
        params.put("top_k", JsonNumber.from(this.topK));
        params.put("min_p", JsonNumber.from(this.minP));
        return params;
    }

    @Bean
    @ConditionalOnProperty(name = "odc.ai.enabled", havingValue = "true")
    public OpenAIClient openAIClient() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new BadRequestException(ErrorCodes.AIConfigurationIncomplete, new Object[]{"API key is not configured"}, "AI service is enabled but API key is not configured. Please set odc.ai.api-key configuration.");
        }
        return OpenAIOkHttpClient.builder()
            .apiKey(this.apiKey)
            .baseUrl(this.baseUrl)
            .build();
    }

    /**
     * 检查AI功能是否可用
     * @return true if AI功能启用且配置完整
     */
    public boolean isAIAvailable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
}
