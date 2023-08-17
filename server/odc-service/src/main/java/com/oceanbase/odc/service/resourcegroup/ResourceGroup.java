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
package com.oceanbase.odc.service.resourcegroup;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionSpecs;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.resourcegroup.model.ResourceIdentifier;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Packaged object for resource <code>ResourceGroup</code>
 *
 * @author yh263208
 * @date 2021-07-27 16:28
 * @since ODC_release_3.2.0
 */
@Getter
@ToString(exclude = {"resourceGroupRepository", "resourceGroupConnectionRepository", "authenticationFacade"})
@EqualsAndHashCode(exclude = {"resourceGroupRepository", "resourceGroupConnectionRepository",
        "authenticationFacade", "createTime", "updateTime"})
public class ResourceGroup implements SecurityResource, OrganizationIsolated {

    private Long id;
    @Setter
    private String name;
    private long creatorId;
    private long organizationId;
    private long lastModifierId;
    private Date createTime;
    private Date updateTime;
    @Setter
    private String description;
    @Setter
    private boolean enabled;
    @Setter
    private String creatorName;
    @Getter(AccessLevel.NONE)
    private final ResourceGroupRepository resourceGroupRepository;
    @Getter(AccessLevel.NONE)
    private final ResourceGroupConnectionRepository resourceGroupConnectionRepository;
    @Getter(AccessLevel.NONE)
    private final AuthenticationFacade authenticationFacade;

    public ResourceGroup(@NonNull ResourceGroupEntity entity,
            @NonNull ResourceGroupRepository resourceGroupRepository,
            @NonNull ResourceGroupConnectionRepository resourceGroupConnectionRepository,
            @NonNull AuthenticationFacade authenticationFacade) {
        this.id = entity.getId();
        this.resourceGroupRepository = resourceGroupRepository;
        this.resourceGroupConnectionRepository = resourceGroupConnectionRepository;
        this.authenticationFacade = authenticationFacade;
        initResourceGroup(entity);
    }

    public ResourceGroup(long resourceGroupId,
            @NonNull ResourceGroupRepository resourceGroupRepository,
            @NonNull ResourceGroupConnectionRepository resourceGroupConnectionRepository,
            @NonNull AuthenticationFacade authenticationFacade) {
        Validate.isTrue(resourceGroupId > 0, "ResourceGroupId can not be negative");
        this.id = resourceGroupId;
        this.resourceGroupRepository = resourceGroupRepository;
        this.resourceGroupConnectionRepository = resourceGroupConnectionRepository;
        this.authenticationFacade = authenticationFacade;
        Optional<ResourceGroupEntity> optional = resourceGroupRepository.findById(resourceGroupId);
        if (!optional.isPresent()) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"ResourceGroup", "ID", this.id},
                    "ResourceGroup: " + this.id + " does not exist");
        }
        initResourceGroup(optional.get());
    }

    public ResourceGroup(@NonNull String name, String description, boolean enabled,
            @NonNull ResourceGroupRepository resourceGroupRepository,
            @NonNull ResourceGroupConnectionRepository resourceGroupConnectionRepository,
            @NonNull AuthenticationFacade authenticationFacade) {
        this.resourceGroupConnectionRepository = resourceGroupConnectionRepository;
        this.resourceGroupRepository = resourceGroupRepository;
        this.authenticationFacade = authenticationFacade;
        this.name = name;
        this.creatorId = authenticationFacade.currentUserId();
        this.organizationId = authenticationFacade.currentOrganizationId();
        this.createTime = new Date();
        this.updateTime = this.createTime;
        this.description = description;
        this.enabled = enabled;
        this.lastModifierId = creatorId;
    }

    public List<ConnectionConfig> getRelatedConnections(@NonNull ConnectionConfigRepository repository) {
        List<ResourceIdentifier> identifiers = getRelatedResources(ResourceType.ODC_CONNECTION);
        return identifiers.stream().map(resourceIdentifier -> {
            Optional<ConnectionEntity> optional = repository.findById(resourceIdentifier.getResourceId());
            ConnectionEntity connectionEntity =
                    optional.orElseThrow(() -> new NotFoundException(ResourceType.ODC_CONNECTION, "ID",
                            resourceIdentifier.getResourceId()));
            return ConnectionMapper.INSTANCE.entityToModel(connectionEntity);
        }).collect(Collectors.toList());
    }

    public List<ResourceIdentifier> getRelatedResources(ResourceType resourceType) {
        if (this.id == null) {
            throw new BadRequestException("Resource group id is null");
        }
        if (!exists()) {
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", this.id);
        }
        /**
         * select * from iam_resource_group_resource where resource_group_id=?;
         */
        Specification<ResourceGroupConnectionEntity> specification =
                Specification.where(ResourceGroupConnectionSpecs.resourceGroupIdEqual(this.id))
                        .and(ResourceGroupConnectionSpecs.resourceGroupIdEqual(this.id))
                        .and(ResourceGroupConnectionSpecs.resourceTypeEqual(resourceType));
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll(specification);
        return entities.stream()
                .map(resourceGroupConnectionEntity -> new ResourceIdentifier(
                        resourceGroupConnectionEntity.getResourceId(),
                        ResourceType.valueOf(resourceGroupConnectionEntity.getResourceType())))
                .collect(Collectors.toList());
    }

    public boolean exists() {
        if (this.id == null) {
            throw new BadRequestException("Resource group id is null");
        }
        return resourceGroupRepository.existsById(this.id);
    }

    public ResourceGroupEntity create() {
        if (this.id != null && exists()) {
            throw new IllegalStateException("ResourceGroup: " + this.id + " already exists");
        }
        ResourceGroupEntity entity = resourceGroupRepository.save(new ResourceGroupEntity(this));
        this.id = entity.getId();
        return entity;
    }

    public int update() {
        if (!exists()) {
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", this.id);
        }
        this.lastModifierId = authenticationFacade.currentUserId();
        return resourceGroupRepository.updateById(new ResourceGroupEntity(this));
    }

    public void delete() {
        if (!exists()) {
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", this.id);
        }
        resourceGroupRepository.deleteById(this.id);
    }

    public void release() {
        if (!exists()) {
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", this.id);
        }
        resourceGroupConnectionRepository.deleteByResourceGroupId(this.id);
    }

    public void bind(@NonNull ResourceIdentifier resourceIdentifier) {
        Verify.notNull(this.id, "ResourceGroupId");
        if (!exists()) {
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", this.id);
        }
        /**
         * select * from iam_resource_group_resource where resource_id=? and resource_type=? and
         * resource_group_id=?;
         */
        Specification<ResourceGroupConnectionEntity> specification =
                Specification.where(ResourceGroupConnectionSpecs.resourceGroupIdEqual(this.getId()))
                        .and(ResourceGroupConnectionSpecs.resourceTypeEqual(resourceIdentifier.getResourceType()))
                        .and(ResourceGroupConnectionSpecs.resourceIdEqual(resourceIdentifier.getResourceId()));
        Optional<ResourceGroupConnectionEntity> optional = resourceGroupConnectionRepository.findOne(specification);
        if (optional.isPresent()) {
            String errMsg =
                    String.format("%s with Id %s has been already bound to the ResourceGroup with Id %s",
                            resourceIdentifier.getResourceType(),
                            resourceIdentifier.getResourceId(), this.getId());
            throw new BadArgumentException(ErrorCodes.BadArgument, new Object[] {"Failed to a bind Connection"},
                    errMsg);
        }
        ResourceGroupConnectionEntity entity =
                new ResourceGroupConnectionEntity(this, resourceIdentifier, authenticationFacade.currentUserId());
        resourceGroupConnectionRepository.save(entity);
    }

    public Set<ResourceIdentifier> bind(@NonNull Set<ResourceIdentifier> identifiers) {
        Verify.notNull(this.id, "ResourceGroupId");
        if (!exists()) {
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", this.id);
        }
        Map<ResourceType, Set<Long>> resourceTypeSetMap = mapByResourceType(identifiers);
        Set<ResourceIdentifier> returnVal = new HashSet<>();
        for (Entry<ResourceType, Set<Long>> entry : resourceTypeSetMap.entrySet()) {
            /**
             * select * from iam_resource_group_resource where resource_group_id=? and resource_type=? and
             * resource_id in ?;
             */
            ResourceType resourceType = entry.getKey();
            Set<Long> resourceIds = entry.getValue();
            Specification<ResourceGroupConnectionEntity> specification =
                    Specification.where(ResourceGroupConnectionSpecs.resourceGroupIdEqual(this.getId()))
                            .and(ResourceGroupConnectionSpecs.resourceTypeEqual(resourceType))
                            .and(ResourceGroupConnectionSpecs.resourceIdIn(resourceIds));
            List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll(specification);
            List<Long> resourceTobeInserted = resourceIds.stream().filter(id -> {
                for (ResourceGroupConnectionEntity entity : entities) {
                    if (id.equals(entity.getResourceId())) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            List<ResourceGroupConnectionEntity> entityList = new LinkedList<>();
            for (Long id : resourceTobeInserted) {
                ResourceGroupConnectionEntity entity =
                        new ResourceGroupConnectionEntity(this, new ResourceIdentifier(id, resourceType),
                                authenticationFacade.currentUserId());
                entityList.add(entity);
                returnVal.add(new ResourceIdentifier(id, resourceType));
            }
            resourceGroupConnectionRepository.saveAll(entityList);
        }
        return returnVal;
    }

    public void release(@NonNull Set<ResourceIdentifier> identifiers) {
        for (ResourceIdentifier identifier : identifiers) {
            resourceGroupConnectionRepository.deleteByResourceGroupIdInAndResourceTypeAndResourceId(
                    Collections.singletonList(this.id), identifier.getResourceType().name(),
                    identifier.getResourceId());
        }
    }

    @Override
    public String resourceId() {
        return this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_RESOURCE_GROUP.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    private void initResourceGroup(ResourceGroupEntity entity) {
        this.name = entity.getName();
        this.creatorId = entity.getCreatorId();
        this.organizationId = entity.getOrganizationId();
        this.createTime = entity.getCreateTime();
        this.updateTime = entity.getUpdateTime();
        this.description = entity.getDescription();
        this.enabled = entity.isEnabled();
        this.lastModifierId = entity.getLastModifierId();
    }

    private Map<ResourceType, Set<Long>> mapByResourceType(@NonNull Collection<ResourceIdentifier> identifiers) {
        Map<ResourceType, Set<Long>> returnVal = new HashMap<>();
        for (ResourceIdentifier identifier : identifiers) {
            Set<Long> ids = returnVal.computeIfAbsent(identifier.getResourceType(), resourceType -> new HashSet<>());
            ids.add(identifier.getResourceId());
        }
        return returnVal;
    }

}
