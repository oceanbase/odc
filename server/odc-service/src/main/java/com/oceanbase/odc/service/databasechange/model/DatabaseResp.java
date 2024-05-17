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
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;

import lombok.Data;

/**
 * @author: zijia.cj
 * @date: 2024/5/17
 */
@Data
public class DatabaseResp implements Serializable {

    private static final long serialVersionUID = -5013749085190365604L;

    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    private String databaseId;

    @JsonProperty(access = Access.READ_ONLY)
    private Boolean existed;

    @NotBlank
    private String name;

    private Project project;

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

    @JsonProperty(access = Access.READ_ONLY)
    private Set<DatabasePermissionType> authorizedPermissionTypes;

    @JsonProperty(access = Access.READ_ONLY)
    private DBObjectSyncStatus objectSyncStatus;

    @JsonProperty(access = Access.READ_ONLY)
    private Date objectLastSyncTime;
    @JsonProperty(access = Access.READ_ONLY)
    private List<InnerUser> owners;
}
