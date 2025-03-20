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
package com.oceanbase.odc.metadb.config;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yizhuo
 * @date 2025/2/12 11:24
 * @description organization config data object
 * @since 4.3.4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "config_organization_configuration",
    uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "key"}))
public class OrganizationConfigEntity extends ConfigEntity {
    /**
     * organization id of this organization configuration
     */
    @NotNull
    @Column(name = "organization_id")
    private Long organizationId;
    /**
     * creator id of this organization configuration
     */
    @Column(name = "creator_id", updatable = false)
    private Long creatorId;
    /**
     * last modifier id of this organization configuration
     */
    @Column(name = "last_modifier_id")
    private Long lastModifierId;
}
