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
package com.oceanbase.odc.service.connection.table.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.tools.dbbrowser.model.DBTable;

import lombok.Data;

/**
 * ClassName: Table Package: com.oceanbase.odc.service.connection.table.model Description:
 *
 * @Author: fenghao
 * @Create 2024/3/12 21:22
 * @Version 1.0
 */
@Data
public class Table extends DBTable implements SecurityResource, OrganizationIsolated, Serializable {
    private Long id;

    /**
     * actual id in specific database instance, probably fetched from dictionary tables
     */
    @NotEmpty
    private Long databaseId;

    private String databaseName;

    private Long dataSourceId;

    private String dataSourceName;

    private Long projectId;

    private String projectName;

    @NotEmpty
    @NotBlank
    private String name;

    @NotEmpty
    @NotBlank
    private String tableName;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date updateTime;

    @JsonProperty(access = Access.READ_ONLY)
    private DatabaseSyncStatus syncStatus;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Set<DatabasePermissionType> authorizedPermissionTypes;

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
