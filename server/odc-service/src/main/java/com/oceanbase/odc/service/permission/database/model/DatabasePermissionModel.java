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
package com.oceanbase.odc.service.permission.database.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.core.shared.constant.PermissionSourceType;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/1/4 11:31
 */
@Data
public class DatabasePermissionModel {

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;

    @JsonProperty(access = Access.READ_ONLY)
    private Long userId;

    @JsonProperty(access = Access.READ_ONLY)
    private DatabasePermissionType permissionType;

    @JsonProperty(access = Access.READ_ONLY)
    private PermissionSourceType sourceType;

    @JsonProperty(access = Access.READ_ONLY)
    private Long ticketId;

    @JsonProperty(access = Access.READ_ONLY)
    private Date createTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Date expireTime;

    @JsonProperty(access = Access.READ_ONLY)
    private Long creatorId;

    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private Long projectId;

    @JsonProperty(access = Access.READ_ONLY)
    private Long databaseId;

    @JsonProperty(access = Access.READ_ONLY)
    private String databaseName;

    @JsonProperty(access = Access.READ_ONLY)
    private Long datasourceId;

    @JsonProperty(access = Access.READ_ONLY)
    private String datasourceName;

    @JsonProperty(access = Access.READ_ONLY)
    private Long environmentId;

    @JsonProperty(access = Access.READ_ONLY)
    private String environmentName;

}
