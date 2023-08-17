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
package com.oceanbase.odc.service.integration.model;

import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author gaoda.xy
 * @date 2023/3/23 20:46
 */
@Data
@NoArgsConstructor
public class IntegrationConfig implements SecurityResource, OrganizationIsolated {
    private Long id;

    @NotNull
    private IntegrationType type;

    @Size(min = 1, max = 64, message = "Integration name is out of range [1,64]")
    @Name(message = "Integration name cannot start or end with whitespaces")
    private String name;

    @NotBlank
    private String configuration;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Encryption encryption;

    private String description;

    @JsonProperty(access = Access.READ_ONLY)
    private Long creatorId;

    @JsonProperty(access = Access.READ_ONLY)
    private String creatorName;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtin;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    public IntegrationConfig(IntegrationEntity entity) {
        this.id = entity.getId();
        this.type = entity.getType();
        this.name = entity.getName();
        this.configuration = entity.getConfiguration();
        this.enabled = entity.getEnabled();
        this.description = entity.getDescription();
        this.creatorId = entity.getCreatorId();
        this.organizationId = entity.getOrganizationId();
        this.builtin = entity.getBuiltin();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
    }

    @Override
    public String resourceId() {
        return this.id.toString();
    }

    @Override
    public String resourceType() {
        switch (this.type) {
            case APPROVAL:
                return ResourceType.ODC_EXTERNAL_APPROVAL.name();
            case SQL_INTERCEPTOR:
                return ResourceType.ODC_EXTERNAL_SQL_INTERCEPTOR.name();
            default:
                return ResourceType.ODC_INTEGRATION.name();
        }
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
