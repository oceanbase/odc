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
package com.oceanbase.odc.service.databasechange.model;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.service.collaboration.environment.model.EnvironmentStyle;
import com.oceanbase.odc.service.common.model.InnerUser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: zijia.cj
 * @date: 2024/5/17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentResp implements Serializable {
    // @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    // @JsonProperty(access = Access.READ_ONLY)
    @Internationalizable
    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private String originalName;

    @Size(max = 2048, message = "Environment description is out of range [0,2048]")
    @Internationalizable
    private String description;

    // @JsonProperty(access = Access.READ_ONLY)
    private EnvironmentStyle style;

    private Long rulesetId;

    @JsonProperty(access = Access.READ_ONLY)
    private String rulesetName;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtIn;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean enabled;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser creator;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser lastModifier;
}
