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
import java.sql.Timestamp;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.RoleType;
import com.oceanbase.odc.metadb.iam.RoleEntity;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/7/21
 */

@Data
public class Role implements SecurityResource, OrganizationIsolated, Serializable {
    private static final long serialVersionUID = 1094521546416165353L;
    private Long id;
    private String name;
    private RoleType type;
    private Boolean enabled;
    @JsonProperty(access = Access.READ_ONLY)
    private List<PermissionConfig> resourceManagementPermissions;
    @JsonProperty(access = Access.READ_ONLY)
    private List<PermissionConfig> systemOperationPermissions;

    private Long creatorId;
    private Long organizationId;
    private String description;

    /**
     * Creator username，只读参数
     */
    @JsonProperty(access = Access.READ_ONLY)
    private String creatorName;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtIn;

    @Override
    public String resourceId() {
        return this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_ROLE.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    public Role() {}

    public Role(RoleEntity roleEntity) {
        this.id = roleEntity.getId();
        this.name = roleEntity.getName();
        this.type = roleEntity.getType();
        this.enabled = roleEntity.getEnabled();
        this.creatorId = roleEntity.getCreatorId();
        this.createTime = roleEntity.getUserCreateTime();
        this.updateTime = roleEntity.getUserUpdateTime();
        this.description = roleEntity.getDescription();
        this.builtIn = roleEntity.getBuiltIn();
        this.organizationId = roleEntity.getOrganizationId();
    }
}
