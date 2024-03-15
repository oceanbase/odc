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
package com.oceanbase.odc.service.collaboration.environment.model;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.common.model.InnerUser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/4/12 15:39
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Environment implements SecurityResource, OrganizationIsolated, Serializable {
    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    @Internationalizable
    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private String originalName;

    @Size(max = 2048, message = "Environment description is out of range [0,2048]")
    @Internationalizable
    private String description;

    @JsonProperty(access = Access.READ_ONLY)
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

    public Environment(@NonNull Long id, @NonNull String name, @NonNull EnvironmentStyle style) {
        this.id = id;
        this.name = name;
        this.style = style;
    }

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_ENVIRONMENT.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }
}
