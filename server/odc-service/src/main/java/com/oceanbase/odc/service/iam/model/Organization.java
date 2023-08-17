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
package com.oceanbase.odc.service.iam.model;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.OrganizationEntity;

import lombok.Data;
import lombok.ToString;

/**
 * @author yizhou.xw
 * @version : Organization.java, v 0.1 2021-08-02 22:06
 */
@Data
@ToString(exclude = "secret")
public class Organization implements Serializable, SecurityResource, OrganizationIsolated {
    @JsonProperty(access = Access.READ_ONLY)
    private Long id;
    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;
    private String uniqueIdentifier;
    @NotBlank
    private String name;
    @Internationalizable
    private String displayName;
    @JsonIgnore
    private String secret;
    @Internationalizable
    private String description;
    private Boolean builtin;
    private OrganizationType type;

    public static Organization ofEntity(OrganizationEntity entity) {
        PreConditions.notNull(entity, "entity");
        Organization organization = new Organization();
        organization.setId(entity.getId());
        organization.setDisplayName(entity.getDisplayName());
        organization.setCreateTime(entity.getCreateTime());
        organization.setUpdateTime(entity.getUpdateTime());
        organization.setUniqueIdentifier(entity.getUniqueIdentifier());
        organization.setName(entity.getName());
        organization.setSecret(entity.getSecret());
        organization.setDescription(entity.getDescription());
        organization.setBuiltin(entity.getBuiltIn());
        organization.setType(entity.getType());
        return organization;
    }

    public OrganizationEntity toEntity() {
        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(this.getId());
        entity.setDisplayName(this.getDisplayName());
        entity.setCreateTime(this.getCreateTime());
        entity.setUpdateTime(this.getUpdateTime());
        entity.setUniqueIdentifier(this.getUniqueIdentifier());
        entity.setName(this.getName());
        entity.setSecret(this.getSecret());
        entity.setDescription(this.getDescription());
        entity.setBuiltIn(this.builtin);
        entity.setType(this.type);
        return entity;
    }

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        if (type == OrganizationType.INDIVIDUAL) {
            return ResourceType.ODC_INDIVIDUAL_ORGANIZATION.name();
        }
        return ResourceType.ODC_TEAM_ORGANIZATION.name();
    }

    @Override
    public Long organizationId() {
        return this.id;
    }

    @Override
    public Long id() {
        return this.id;
    }
}
