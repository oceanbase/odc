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
package com.oceanbase.odc.metadb.resourcegroup;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.service.resourcegroup.ResourceGroup;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Entity for <code>ResourceGroup</code> object
 *
 * @author yh263208
 * @date 2021-07-26 11:26
 * @since ODC-release_3.2.0
 */
@Getter
@Setter
@ToString
@Entity
@EqualsAndHashCode(exclude = {"updateTime", "createTime"})
@Table(name = "iam_resource_group")
public class ResourceGroupEntity {
    /**
     * ID for a <code>ResourceGroupEntity</code>, can not be null
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Name for a <code>ResourceGroupEntity</code>, can not be null
     */
    @Column(name = "name", nullable = false)
    private String name;
    /**
     * UserId for <code>ResourceGroupEntity</code>, can not be null
     */
    @Column(name = "creator_id", nullable = false)
    private long creatorId;
    /**
     * OrganizationId for <code>ResourceGroupEntity</code>, can not be null
     */
    @Column(name = "organization_id", nullable = false)
    private long organizationId;
    @Column(name = "last_modifier_id")
    private long lastModifierId;
    /**
     * Create time for a <code>ResourceGroupEntity</code>
     */
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Update time for a <code>ResourceGroupEntity</code>
     */
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
    /**
     * Description for <code>ResourceGroupEntity</code>
     */
    @Column(name = "description")
    private String description;
    /**
     * Flag to indicate the <code>ResourceGroupEntity</code> is enabled
     */
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    public ResourceGroupEntity() {}

    public ResourceGroupEntity(ResourceGroup resourceGroup) {
        Validate.notNull(resourceGroup, "ResourceGroup can not be null for ResourceGroupEntity");
        this.id = resourceGroup.getId();
        this.name = resourceGroup.getName();
        this.creatorId = resourceGroup.getCreatorId();
        this.organizationId = resourceGroup.getOrganizationId();
        this.createTime = resourceGroup.getCreateTime();
        this.updateTime = resourceGroup.getUpdateTime();
        this.description = resourceGroup.getDescription();
        this.enabled = resourceGroup.isEnabled();
        this.lastModifierId = resourceGroup.getLastModifierId();
    }

}
