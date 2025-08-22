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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.service.llm.model.Constants;
import com.oceanbase.odc.service.llm.model.ModelType;
import com.oceanbase.odc.service.llm.provider.ModelCredential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TongyiModelCredential implements ModelCredential {

    private String modelName;

    private boolean deprecated;

    private ModelType modelType;

    @JsonProperty("dashscope_api_key")
    private String apiKey;

    @JsonProperty("max_tokens")
    private Integer maxToken = 4096;

    @JsonProperty("context_size")
    private Integer contextSize = 4096;

    @JsonProperty("function_calling_type")
    private String functionCallingType;

    /**
     * 控制随机性和多样性的程度 范围: 0.0 - 2.0, 默认: 0.3
     */
    private Float temperature;

    /**
     * 核采样方法概率阈值 范围: 0.1 - 0.9, 默认: 0.8
     */
    private Float topP;

    /**
     * 采样候选集的大小 范围: 0 - 99
     */
    private Integer topK;

    /**
     * 随机数种子 默认: 1234
     */
    private Integer seed;

    /**
     * 重复惩罚 默认: 1.1
     */
    private Float repetitionPenalty;

    /**
     * 是否启用联网搜索 默认: false
     */
    private Boolean enableSearch = false;

    /**
     * 响应格式
     */
    private String responseFormat;

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public boolean isSupportFunctionCalling() {
        return Constants.SUPPORT_FUNCTION_CALLING_TYPE.equals(functionCallingType);
    }

}
