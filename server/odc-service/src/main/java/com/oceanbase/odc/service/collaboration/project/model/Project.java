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
package com.oceanbase.odc.service.collaboration.project.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.common.model.InnerUser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2023/4/11 16:11
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Project implements SecurityResource, OrganizationIsolated, Serializable {
    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @Size(min = 1, max = 256, message = "Project name is out of range [1,256]")
    @Name(message = "Project name cannot start or end with whitespaces")
    @Internationalizable
    private String name;

    @Size(max = 2048, message = "Project description is out of range [0,2048]")
    @Internationalizable
    private String description;

    /**
     * flag if the project is archived
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Boolean archived;

    /**
     * project members, may developer, dba, owner, etc
     */
    private List<ProjectMember> members;

    @JsonProperty(access = Access.READ_ONLY)
    private Set<ResourceRoleName> currentUserResourceRoles;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtin;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser creator;

    @JsonProperty(access = Access.READ_ONLY)
    private InnerUser lastModifier;

    @JsonProperty(access = Access.READ_ONLY)
    private Date dbObjectLastSyncTime;
    private String uniqueIdentifier;

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_PROJECT.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectMember {
        private Long id;

        @JsonProperty(access = Access.READ_ONLY)
        private String accountName;

        @JsonProperty(access = Access.READ_ONLY)
        private String name;

        private ResourceRoleName role;
    }
}
