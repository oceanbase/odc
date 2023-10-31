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
package com.oceanbase.odc.service.connection.database.model;

import java.io.Serializable;
import java.util.Date;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/4/12 19:14
 * @Description: []
 */
@Data
public class Database implements SecurityResource, OrganizationIsolated, Serializable {
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    private String databaseId;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean existed;

    @NotBlank
    private String name;

    @JsonProperty(access = Access.READ_ONLY)
    private Project project;

    @JsonProperty(access = Access.READ_ONLY)
    private ConnectionConfig dataSource;

    @JsonProperty(access = Access.READ_ONLY)
    private Environment environment;

    @JsonProperty(access = Access.READ_ONLY)
    private DatabaseSyncStatus syncStatus;

    @JsonProperty(access = Access.READ_ONLY)
    private Date lastSyncTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private String charsetName;

    @JsonProperty(access = Access.READ_ONLY)
    private String collationName;

    @JsonProperty(access = Access.READ_ONLY)
    private Long tableCount;

    @JsonProperty(access = Access.READ_ONLY)
    private boolean lockDatabaseUserRequired;

    @Override
    public String resourceId() {
        return this.id == null ? null : this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_DATABASE.name();
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
