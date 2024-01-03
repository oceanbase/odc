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
package com.oceanbase.odc.metadb.iam;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.oceanbase.odc.core.shared.constant.PermissionSourceType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2024/1/3 17:30
 */
@Data
@Entity
@Table(name = "list_user_database_permission_view")
@EqualsAndHashCode(exclude = {"createTime"})
public class UserDatabasePermissionEntity {

    private Long id;

    private Long userId;

    private String action;

    private PermissionSourceType sourceType;

    private Long ticketId;

    private Date createTime;

    private Date expireTime;

    private Long creatorId;

    private Long organizationId;

    private Long projectId;

    private Long databaseId;

    private String databaseName;

    private Long connectionId;

    private String connectionName;

    private Long environmentId;

    private String environmentName;

}
