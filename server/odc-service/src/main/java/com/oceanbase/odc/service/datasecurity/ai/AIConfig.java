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

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

import lombok.Data;

/**
 * 后期需要弄成动态配置
 */
@Data
@Component
// @ConfigurationProperties(prefix = "datasecurity.ai")
public class AIConfig {
    /**
     * 是否启用AI服务
     */
    private boolean enabled = true;;

    /**
     * API 密钥
     */
    //private String apiKey = "sk-c6bbbbde1b7e420b897d0662301c6d7c";
    private String apiKey = "token-abc123";

    /**
     * API 的基础URL
     */
    //private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String baseUrl = "http://172.25.17.78:8000/v1";

    /**
     * 默认使用的模型名称
     */
    //private String model = "qwen2.5-3b-instruct";
    private String model = "nlora";

    @Bean
    public OpenAIClient openAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(this.apiKey)
                .baseUrl(this.baseUrl)
                .build();
    }


}
