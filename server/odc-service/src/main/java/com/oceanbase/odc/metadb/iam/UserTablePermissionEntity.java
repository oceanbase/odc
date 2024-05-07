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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.core.shared.constant.AuthorizationType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:26
 * @Version 1.0
 */
@Data
@Entity
@Table(name = "list_user_table_permission_view")
@EqualsAndHashCode(exclude = {"createTime"})
public class UserTablePermissionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "action")
    private String action;

    @Column(name = "authorization_type")
    @Enumerated(EnumType.STRING)
    private AuthorizationType authorizationType;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "expire_time")
    private Date expireTime;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "database_id")
    private Long databaseId;

    @Column(name = "database_name")
    private String databaseName;

    @Column(name = "data_source_id")
    private Long dataSourceId;

    @Column(name = "data_source_name")
    private String dataSourceName;

    @Column(name = "table_id")
    private Long tableId;

    @Column(name = "table_name")
    private String tableName;

}
