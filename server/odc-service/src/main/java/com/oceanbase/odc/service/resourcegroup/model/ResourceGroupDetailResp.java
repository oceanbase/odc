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
package com.oceanbase.odc.service.resourcegroup.model;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.service.resourcegroup.ResourceGroup;
import com.oceanbase.odc.service.resourcegroup.model.ModifyResourceGroupReq.ConnectionMetaInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Detail message response object for <code>ResourceGroup</code>
 *
 * @author yh263208
 * @date 2021-07-27 18:05
 * @since ODC-release_3.2.0
 */
@Getter
@Setter
@ToString
public class ResourceGroupDetailResp {
    /**
     * UUID for a <code>ResourceGroup</code>, can not be null
     */
    private Long id;
    /**
     * Name for a <code>ResourceGroup</code>, can not be null
     */
    private String name;
    /**
     * UserId for <code>ResourceGroup</code>, can not be null
     */
    private long creatorId;
    /**
     * OrganizationId for <code>ResourceGroup</code>, can not be null
     */
    private long organizationId;
    /**
     * Create time for a <code>ResourceGroup</code>
     */
    private Date createTime;
    /**
     * Update time for a <code>ResourceGroup</code>
     */
    private Date updateTime;
    /**
     * Description for <code>ResourceGroup</code>
     */
    private String description;
    /**
     * Flag to indicate the <code>ResourceGroup</code> is enabled
     */
    private boolean enabled;
    private String creatorName;
    /**
     * Connection id list
     */
    @NotNull
    private List<ConnectionMetaInfo> connections;

    public ResourceGroupDetailResp(ResourceGroup resourceGroup, ConnectionConfigRepository repository) {
        Validate.notNull(repository, "Connection repository can not be null for ResourceGroupDetailResp");
        init(resourceGroup);
        if (resourceGroup.exists()) {
            this.connections = resourceGroup.getRelatedConnections(repository).stream().map(ConnectionMetaInfo::new)
                    .collect(Collectors.toList());
        }
    }

    public ResourceGroupDetailResp(ResourceGroup resourceGroup) {
        init(resourceGroup);
        if (resourceGroup.exists()) {
            this.connections = resourceGroup.getRelatedResources(ResourceType.ODC_CONNECTION).stream()
                    .map(ConnectionMetaInfo::new).collect(Collectors.toList());
        }
    }

    public ResourceGroupDetailResp(ResourceGroup resourceGroup, List<ResourceIdentifier> relatedResources) {
        init(resourceGroup);
        this.connections = relatedResources.stream()
                .filter(identifier -> ResourceType.ODC_CONNECTION == identifier.getResourceType())
                .map(ConnectionMetaInfo::new).collect(Collectors.toList());
    }

    private void init(ResourceGroup resourceGroup) {
        Validate.notNull(resourceGroup, "ResourceGroup can not be null for ResourceGroupDetailResp");
        this.id = resourceGroup.getId();
        this.name = resourceGroup.getName();
        this.creatorId = resourceGroup.getCreatorId();
        this.organizationId = resourceGroup.getOrganizationId();
        this.createTime = resourceGroup.getCreateTime();
        this.updateTime = resourceGroup.getUpdateTime();
        this.description = resourceGroup.getDescription();
        this.enabled = resourceGroup.isEnabled();
        this.creatorName = resourceGroup.getCreatorName();
    }

}
