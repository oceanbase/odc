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
package com.oceanbase.odc.service.llm.provider.openaiapicompatible;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.service.llm.model.ModelType;
import com.oceanbase.odc.service.llm.provider.ModelCredential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/14
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenAIApiCompatibleModelCredential implements ModelCredential {

    private String modelName;

    private boolean deprecated;

    private ModelType modelType;

    /**
     * 模型显示名称
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * API Key
     */
    @JsonProperty("api_key")
    private String apiKey;

    /**
     * API endpoint URL
     */
    @JsonProperty("endpoint_url")
    private String endpointUrl;

    /**
     * API endpoint中的模型名称
     */
    @JsonProperty("endpoint_model_name")
    private String endpointModelName;

    /**
     * 模型上下文长度
     */
    @JsonProperty("context_size")
    private Integer contextSize = 4096;

    /**
     * 最大 token 上限
     */
    @JsonProperty("max_tokens_to_sample")
    private Integer maxTokensToSample = 4096;

    /**
     * 函数调用类型 (tool_call, no_call)
     */
    @JsonProperty("function_calling_type")
    private String functionCallingType = "tool_call";

    @Override
    public Integer getMaxToken() {
        return maxTokensToSample;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public boolean isSupportFunctionCalling() {
        return "function_call".equals(functionCallingType) || "tool_call".equals(functionCallingType);
    }

}
