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

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.core.shared.constant.UserType;

import lombok.Data;

@Table(name = "iam_user_login_time_view")
@Entity
@Data
public class UserLoginViewEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private UserType type;

    private String name;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    private String description;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "user_create_time", updatable = false)
    private Timestamp userCreateTime;

    @Column(name = "user_update_time")
    private Timestamp userUpdateTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Timestamp createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Timestamp updateTime;

    @Column(name = "last_login_time")
    private Timestamp lastLoginTime;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtIn;

    @Column(name = "extra_properties_json")
    private String extraPropertiesJson;

}
