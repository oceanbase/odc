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
import com.oceanbase.odc.service.resourcegroup.model.ResourceIdentifier;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Association objects, used to associate <code>ConnectionEntity</code> objects and
 * <code>ResourceGroupEntity</code> objects
 *
 * @author yh263208
 * @date 2021-07-27
 * @since ODC_release_3.2.0
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "iam_resource_group_resource")
public class ResourceGroupConnectionEntity {
    /**
     * Id for <code>ResourceGroupConnectionEntity</code>
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Id for <code>ConnectionEntity</code>
     */
    @Column(name = "resource_id", nullable = false, updatable = false)
    private Long resourceId;
    /**
     * Id for <code>ConnectionEntity</code>
     */
    @Column(name = "resource_type", nullable = false, updatable = false)
    private String resourceType;
    /**
     * Id for <code>ResourceGroupEntity</code>
     */
    @Column(name = "resource_group_id", nullable = false, updatable = false)
    private Long resourceGroupId;
    /**
     * Id for Creator
     */
    @Column(name = "creator_id", updatable = false)
    private Long creatorId;
    /**
     * Record insertion time
     */
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    /**
     * Record modification time
     */
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    public ResourceGroupConnectionEntity() {}

    public ResourceGroupConnectionEntity(ResourceGroup resourceGroup, ResourceIdentifier resourceIdentifier,
            Long creatorId) {
        Validate.notNull(resourceGroup, "ResourceGroup can not be null for ResourceGroupConnectionEntity");
        Validate.notNull(resourceIdentifier, "Resource can not be null for ResourceGroupConnectionEntity");
        this.resourceId = resourceIdentifier.getResourceId();
        this.resourceType = resourceIdentifier.getResourceType().name();
        this.resourceGroupId = resourceGroup.getId();
        this.creatorId = creatorId;
    }

}
