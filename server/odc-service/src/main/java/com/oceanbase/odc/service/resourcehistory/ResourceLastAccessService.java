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
package com.oceanbase.odc.service.resourcehistory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.resourcehistory.ResourceLastAccessEntity;
import com.oceanbase.odc.metadb.resourcehistory.ResourceLastAccessRepository;

@Service
public class ResourceLastAccessService {

    private final ResourceLastAccessRepository resourceLastAccessRepository;

    @Autowired
    public ResourceLastAccessService(ResourceLastAccessRepository resourceLastAccessRepository) {
        this.resourceLastAccessRepository = resourceLastAccessRepository;
    }

    public int batchAdd(List<ResourceLastAccessEntity> entities) {
        PreConditions.notEmpty(entities, "entities");
        return resourceLastAccessRepository.batchUpsert(entities);
    }

    public ResourceLastAccessEntity add(Long organizationId, Long projectId, Long userId, ResourceType resourceType,
            Long itemId, Date accessTime) {
        PreConditions.notNull(organizationId, "organizationId");
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(userId, "userId");
        PreConditions.notNull(resourceType, "resourceType");
        PreConditions.notNull(itemId, "itemId");
        Optional<ResourceLastAccessEntity> entityOptional =
                resourceLastAccessRepository.findByOrganizationIdAndProjectIdAndUserIdAndResourceTypeAndResourceId(
                        organizationId, projectId, userId,
                        resourceType.name(), itemId);
        ResourceLastAccessEntity entity;
        if (!entityOptional.isPresent()) {
            entity = ResourceLastAccessEntity.builder().organizationId(organizationId).projectId(projectId)
                    .userId(userId).resourceType(resourceType.name()).resourceId(itemId)
                    .lastAccessTime(accessTime).build();
        } else {
            entity = entityOptional.get();
            entity.setLastAccessTime(accessTime);
        }
        resourceLastAccessRepository.save(entity);
        return entity;
    }

    public int batchDeleteResourceIds(Long organizationId, Long projectId, ResourceType resourceType,
            Collection<Long> resourceIds) {
        PreConditions.notNull(organizationId, "organizationId");
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(resourceType, "resourceType");
        PreConditions.notEmpty(resourceIds, "resourceIds");
        return resourceLastAccessRepository.deleteByOrganizationIdAndProjectIdAndResourceTypeAndResourceIdIn(
                organizationId, projectId,
                resourceType.name(),
                resourceIds);
    }

    public Optional<ResourceLastAccessEntity> detail(Long organizationId, Long projectId,
            Long userId, ResourceType resourceType, Long resourceId) {
        PreConditions.notNull(organizationId, "organizationId");
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(userId, "userId");
        PreConditions.notNull(resourceType, "resourceType");
        PreConditions.notNull(resourceId, "resourceId");
        return resourceLastAccessRepository.findByOrganizationIdAndProjectIdAndUserIdAndResourceTypeAndResourceId(
                organizationId, projectId, userId, resourceType.name(), resourceId);
    }

    public Page<ResourceLastAccessEntity> listLastAccessesOfUser(Long organizationId, Long projectId, Long userId,
            ResourceType resourceType, Pageable pageable) {
        PreConditions.notNull(organizationId, "organizationId");
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(userId, "userId");
        PreConditions.notNull(resourceType, "resourceType");
        PreConditions.notNull(pageable, "pageable");

        return resourceLastAccessRepository.findByOrganizationIdAndProjectIdAndUserIdAndResourceType(organizationId,
                projectId, userId, resourceType.name(), pageable);
    }
}
