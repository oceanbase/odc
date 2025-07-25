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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI configuration model
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIConfig {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    private Long organizationId;

    private Long lastModifierId;

    private Boolean aiEnabled;

    private Boolean chatEnabled;

    private Boolean copilotEnabled;

    private Boolean completionEnabled;

    @Size(max = 256, message = "Default embedding model is out of range [0,256]")
    private String defaultEmbeddingModel;

    @Size(max = 256, message = "Default llm model is out of range [0,256]")
    private String defaultLlmModel;

    @Size(max = 256, message = "Default chat model is out of range [0,256]")
    private String defaultChatModel;

}
