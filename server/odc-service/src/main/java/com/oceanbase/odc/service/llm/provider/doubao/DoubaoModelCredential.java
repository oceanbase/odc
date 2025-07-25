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
package com.oceanbase.odc.service.llm.provider.doubao;

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
public class DoubaoModelCredential implements ModelCredential {

    private String modelName;

    private boolean deprecated;

    private ModelType modelType;

    /**
     * 鉴权方式 (aksk 或 api_key)
     */
    @JsonProperty("auth_method")
    private String authMethod = "aksk";

    /**
     * Access Key (用于 aksk 鉴权方式)
     */
    @JsonProperty("volc_access_key_id")
    private String volcAccessKeyId;

    /**
     * Secret Access Key (用于 aksk 鉴权方式)
     */
    @JsonProperty("volc_secret_access_key")
    private String volcSecretAccessKey;

    /**
     * API Key (用于 api_key 鉴权方式)
     */
    @JsonProperty("volc_api_key")
    private String volcApiKey;

    /**
     * 火山引擎地域
     */
    @JsonProperty("volc_region")
    private String volcRegion = "cn-beijing";

    /**
     * API Endpoint Host
     */
    @JsonProperty("api_endpoint_host")
    private String apiEndpointHost = "https://ark.cn-beijing.volces.com/api/v3";

    /**
     * Endpoint ID
     */
    @JsonProperty("endpoint_id")
    private String endpointId;

    /**
     * 基础模型名称
     */
    @JsonProperty("base_model_name")
    private String baseModelName;

    /**
     * 最大 token 上限
     */
    @JsonProperty("max_tokens")
    private Integer maxToken = 4096;

    /**
     * 模型上下文长度
     */
    @JsonProperty("context_size")
    private Integer contextSize = 4096;

    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    @Override
    public boolean isSupportFunctionCalling() {
        return true;
    }

}
