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
package com.oceanbase.odc.service.llm.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM integration model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmModel {

    private Long id;

    private Date createTime;

    private Date updateTime;

    @NotNull
    private Long organizationId;

    @NotNull
    private Long creatorId;

    @NotNull
    private Long lastModifierId;

    @NotNull
    @Size(max = 128, message = "Provider name is out of range [1,128]")
    private String providerName;

    @NotNull
    @Size(max = 128, message = "Model name is out of range [1,128]")
    private String modelName;

    @Size(max = 128, message = "Model display name is out of range [0,128]")
    private String displayName;

    @NotNull
    private ModelType modelType;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Boolean deprecated;

    @NotNull
    private Boolean custom;

    @NotNull
    private Boolean functionCallingSupport;

    /**
     * Avoid serialization
     */
    @JsonIgnore
    private String propertiesJson;

    /**
     * Avoid serialization
     */
    @JsonIgnore
    private String salt;

    private Integer maxToken;

    private Long usedToken;

    private Integer contextSize;

    private String description;

}
