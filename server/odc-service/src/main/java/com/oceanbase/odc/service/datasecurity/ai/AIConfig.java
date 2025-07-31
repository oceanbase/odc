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

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonBoolean;
import com.openai.core.JsonNumber;
import com.openai.core.JsonValue;

import lombok.Data;

/**
 * 后期需要弄成动态配置
 */
@Data
@Component
// @ConfigurationProperties(prefix = "datasecurity.ai")
public class AIConfig {
    private boolean enabled = true;

    //private String apiKey = "sk-c6bbbbde1b7e420b897d0662301c6d7c";
    private String apiKey = "token-abc123";

    //private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String baseUrl = "http://172.25.17.78:8000/v1";

    //private String model = "qwen3-8b";
    private String model = "nlora";

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
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
            .apiKey(this.apiKey)
            .baseUrl(this.baseUrl)
            .build();
    }
}
