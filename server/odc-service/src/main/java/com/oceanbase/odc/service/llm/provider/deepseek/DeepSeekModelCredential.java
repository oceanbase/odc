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
package com.oceanbase.odc.service.llm.provider.deepseek;

import com.oceanbase.odc.service.llm.model.Constants;
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
public class DeepSeekModelCredential implements ModelCredential {

    private String modelName;

    private boolean deprecated;

    private ModelType modelType;

    private String apiKey;

    private String endpointUrl;

    private Integer maxToken;

    private Integer contextSize;

    private String functionCallingType;

    /**
     * 控制生成结果的多样性和随机性 范围: 0.0 - 2.0, 默认: 1.0
     */
    private Float temperature;

    /**
     * 核采样方法概率阈值 范围: 0.01 - 1.00, 默认: 1.0
     */
    private Float topP;

    /**
     * 是否返回所输出 token 的对数概率 默认: false
     */
    private Boolean logprobs;

    /**
     * 每个输出位置返回输出概率 top N 的 token 范围: 0 - 20, 默认: 0
     */
    private Integer topLogprobs;

    /**
     * 频率惩罚 范围: -2.0 - 2.0, 默认: 0
     */
    private Float frequencyPenalty;

    /**
     * 响应格式: text 或 json_object
     */
    private String responseFormat;

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public ModelType getModelType() {
        return modelType;
    }

    @Override
    public boolean isSupportFunctionCalling() {
        return Constants.SUPPORT_FUNCTION_CALLING_TYPE.equals(functionCallingType);
    }

}
