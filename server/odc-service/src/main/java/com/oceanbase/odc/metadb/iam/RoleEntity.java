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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.oceanbase.odc.core.shared.constant.RoleType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wenniu.ly
 * @date 2021/6/25
 */

@Data
@Entity
@Table(name = "iam_role")
@EqualsAndHashCode(exclude = {"updateTime", "createTime", "userCreateTime", "userUpdateTime"})
public class RoleEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RoleType type;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "user_create_time", updatable = false)
    private Timestamp userCreateTime;

    @Column(name = "user_update_time")
    private Timestamp userUpdateTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Timestamp createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Timestamp updateTime;

    @Column(name = "description")
    private String description;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtIn;
}
