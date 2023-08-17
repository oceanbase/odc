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

import static com.oceanbase.odc.core.shared.constant.ResourceType.ODC_CONNECTION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionSpecs;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupSpecs;
import com.oceanbase.odc.service.connection.util.ConnectionMapper;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.PermissionService;
import com.oceanbase.odc.service.iam.ResourcePermissionAccessor;
import com.oceanbase.odc.service.iam.VerticalPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.resourcegroup.model.ModifyResourceGroupReq;
import com.oceanbase.odc.service.resourcegroup.model.ModifyResourceGroupReq.ConnectionMetaInfo;
import com.oceanbase.odc.service.resourcegroup.model.QueryResourceGroupReq;
import com.oceanbase.odc.service.resourcegroup.model.ResourceGroupDetailResp;
import com.oceanbase.odc.service.resourcegroup.model.ResourceIdentifier;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Service object for <code>ResourceGroup</code>
 *
 * @author yh263208
 * @date 2021-07-27 16:31
 * @since ODC-release_3.2.0
 */
@Service
@Slf4j
@Validated
@Authenticated
public class ResourceGroupService {

    @Autowired
    private ResourceGroupRepository resourceGroupRepository;
    @Autowired
    private ConnectionConfigRepository connectionRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ResourceGroupConnectionRepository resourceGroupConnectionRepository;
    @Autowired
    private HorizontalDataPermissionValidator horizontalDataPermissionValidator;
    @Autowired
    private ResourcePermissionAccessor resourcePermissionAccessor;
    @Autowired
    private VerticalPermissionValidator verticalPermissionValidator;

    @PreAuthenticate(actions = "read", resourceType = "ODC_RESOURCE_GROUP", indexOfIdParam = 0)
    public ResourceGroup get(@NotNull Long resourceGroupId) {
        Optional<ResourceGroupEntity> optional = resourceGroupRepository.findById(resourceGroupId);
        ResourceGroupEntity entity = optional
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "ID", resourceGroupId));
        ResourceGroup resourceGroup = new ResourceGroup(entity, resourceGroupRepository,
                resourceGroupConnectionRepository, authenticationFacade);
        horizontalDataPermissionValidator.checkCurrentOrganization(resourceGroup);
        Optional<UserEntity> userEntity = userRepository.findById(resourceGroup.getCreatorId());
        resourceGroup.setCreatorName(userEntity.isPresent() ? userEntity.get().getAccountName() : "N/A");
        return resourceGroup;
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_RESOURCE_GROUP", isForAll = true)
    public List<ResourceGroup> getAllRelatedResources() {
        Map<String, Set<String>> permittedResourceGroup2Actions = resourcePermissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_RESOURCE_GROUP, permission -> {
                    ResourcePermission minPermission = new ResourcePermission(permission.getResourceId(),
                            ResourceType.ODC_RESOURCE_GROUP.name(), "update");
                    return permission.implies(minPermission);
                });
        if (permittedResourceGroup2Actions.isEmpty()) {
            return null;
        }
        List<ResourceGroupEntity> resourceGroupEntities;
        if (permittedResourceGroup2Actions.containsKey("*")) {
            resourceGroupEntities = resourceGroupRepository.findByOrganizationId(
                    authenticationFacade.currentOrganizationId());
        } else {
            resourceGroupEntities =
                    resourceGroupRepository.findByIdIn(permittedResourceGroup2Actions.keySet().stream()
                            .map(Long::parseLong).collect(Collectors.toList()));
        }
        List<ResourceGroup> resourceGroups = new ArrayList<>();
        for (ResourceGroupEntity resourceGroupEntity : resourceGroupEntities) {
            ResourceGroup resourceGroup = new ResourceGroup(resourceGroupEntity, resourceGroupRepository,
                    resourceGroupConnectionRepository, authenticationFacade);
            resourceGroups.add(resourceGroup);
        }
        return resourceGroups;
    }

    @PreAuthenticate(actions = "create", resourceType = "ODC_RESOURCE_GROUP", isForAll = true)
    public boolean exists(@NotNull String resourceGroupName) {
        Specification<ResourceGroupEntity> specification = Specification
                .where(ResourceGroupSpecs.nameEqual(resourceGroupName))
                .and(ResourceGroupSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()));
        List<ResourceGroupEntity> entities = resourceGroupRepository.findAll(specification);
        return !entities.isEmpty();
    }

    public Page<ResourceGroup> list(@NotNull QueryResourceGroupReq request, @NotNull Pageable pageable) {
        Map<String, Set<String>> permittedResourceGroup2Actions = resourcePermissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_RESOURCE_GROUP, permission -> {
                    ResourcePermission minPermission = new ResourcePermission(permission.getResourceId(),
                            ResourceType.ODC_RESOURCE_GROUP.name(),
                            MoreObjects.firstNonNull(request.getMinPrivilege(), "read"));
                    return permission.implies(minPermission);
                });
        if (permittedResourceGroup2Actions.isEmpty()) {
            return Page.empty(pageable);
        }
        Specification<ResourceGroupEntity> spec = Specification
                .where(ResourceGroupSpecs.nameLike(request.getFuzzySearchKeyword())
                        .or(ResourceGroupSpecs.idLike(request.getFuzzySearchKeyword())))
                .and(ResourceGroupSpecs.statusIn(request.getStatuses()))
                .and(ResourceGroupSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()));
        if (!permittedResourceGroup2Actions.containsKey("*")) {
            spec = spec.and(ResourceGroupSpecs.idIn(permittedResourceGroup2Actions.keySet().stream()
                    .map(Long::parseLong).collect(Collectors.toList())));
        }
        Page<ResourceGroupEntity> queryResult = resourceGroupRepository.findAll(spec, pageable);
        return queryResult.map(entity -> new ResourceGroup(entity, resourceGroupRepository,
                resourceGroupConnectionRepository, authenticationFacade));
    }

    @SkipAuthorize("odc internal usage")
    public Page<ResourceGroupDetailResp> listWithIdentifiers(@NotNull QueryResourceGroupReq request,
            @NotNull Pageable pageable) {
        Page<ResourceGroup> resourceGroups = list(request, pageable);
        Map<ResourceGroup, List<ResourceIdentifier>> resourceGroup2RelatedResources =
                getRelatedResources(resourceGroups.getContent(),
                        identifier -> ODC_CONNECTION.equals(identifier.getResourceType()));
        return resourceGroups.map(resourceGroup -> {
            List<ResourceIdentifier> identifiers =
                    resourceGroup2RelatedResources.getOrDefault(resourceGroup, new LinkedList<>());
            return new ResourceGroupDetailResp(resourceGroup, identifiers);
        });
    }

    Map<ResourceGroup, List<ResourceIdentifier>> getRelatedResources(
            @NonNull Collection<ResourceGroup> resourceGroups, @NonNull Predicate<ResourceIdentifier> predicate) {
        Map<ResourceGroup, List<ResourceIdentifier>> returnVal = new HashMap<>();
        Specification<ResourceGroupConnectionEntity> specification = Specification.where(ResourceGroupConnectionSpecs
                .resourceGroupIdIn(resourceGroups.stream().map(ResourceGroup::getId).collect(Collectors.toSet())));
        List<ResourceGroupConnectionEntity> entities = resourceGroupConnectionRepository.findAll(specification);
        for (ResourceGroup resourceGroup : resourceGroups) {
            List<ResourceIdentifier> identifiers = returnVal.computeIfAbsent(resourceGroup, k -> new LinkedList<>());
            for (ResourceGroupConnectionEntity entity : entities) {
                ResourceIdentifier identifier = new ResourceIdentifier(
                        entity.getResourceId(), ResourceType.valueOf(entity.getResourceType()));
                if (!predicate.test(identifier)
                        || !Objects.equals(resourceGroup.getId(), entity.getResourceGroupId())) {
                    continue;
                }
                identifiers.add(identifier);
            }
        }
        return returnVal;
    }

    @SkipAuthorize("odc internal usage")
    public List<ResourceGroup> getRelatedResourceGroups(ResourceIdentifier resourceIdentifier) {
        Map<ResourceIdentifier, List<ResourceGroup>> relatedResourceGroups =
                getRelatedResourceGroups(Collections.singletonList(resourceIdentifier));
        return relatedResourceGroups.computeIfAbsent(resourceIdentifier, k -> new ArrayList<>());
    }

    @SkipAuthorize("odc internal usage")
    public Map<ResourceIdentifier, List<ResourceGroup>> getRelatedResourceGroups(
            Collection<ResourceIdentifier> resourceIdentifiers) {
        Map<ResourceIdentifier, List<ResourceGroup>> resourceIdentifierListMap = new HashMap<>();
        Map<ResourceType, Set<Long>> mapByType = new HashMap<>();
        for (ResourceIdentifier identifier : resourceIdentifiers) {
            Set<Long> resourceIds =
                    mapByType.computeIfAbsent(identifier.getResourceType(), resourceType -> new HashSet<>());
            resourceIds.add(identifier.getResourceId());
        }
        Set<Entry<ResourceType, Set<Long>>> entries = mapByType.entrySet();
        for (Entry<ResourceType, Set<Long>> entry : entries) {
            ResourceType resourceType = entry.getKey();
            Set<Long> resourceIds = entry.getValue();
            Specification<ResourceGroupConnectionEntity> specification =
                    Specification.where(ResourceGroupConnectionSpecs.resourceTypeEqual(resourceType))
                            .and(ResourceGroupConnectionSpecs.resourceIdIn(resourceIds));
            List<ResourceGroupConnectionEntity> resourceGroupConnectionEntities =
                    resourceGroupConnectionRepository.findAll(specification);
            for (ResourceGroupConnectionEntity resourceGroupConnectionEntity : resourceGroupConnectionEntities) {
                ResourceIdentifier identifier = new ResourceIdentifier(resourceGroupConnectionEntity.getResourceId(),
                        ResourceType.valueOf(resourceGroupConnectionEntity.getResourceType()));
                List<ResourceGroup> resourceGroups =
                        resourceIdentifierListMap.computeIfAbsent(identifier, resourceIdentifier -> new LinkedList<>());
                resourceGroups.add(new ResourceGroup(resourceGroupConnectionEntity.getResourceGroupId(),
                        resourceGroupRepository, resourceGroupConnectionRepository, authenticationFacade));
            }
        }
        return resourceIdentifierListMap;
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "create", resourceType = "ODC_RESOURCE_GROUP", isForAll = true)
    public ResourceGroup create(@NotNull @Valid ModifyResourceGroupReq request) {
        PreConditions.validNoDuplicated(ResourceType.ODC_RESOURCE_GROUP,
                "name", request.getName(), () -> exists(request.getName()));

        List<SecurityResource> resources = request.getConnections().stream()
                .map(c -> {
                    String resourceId = Objects.nonNull(c.getId()) ? c.getId().toString() : "*";
                    return new DefaultSecurityResource(resourceId, ODC_CONNECTION.name());
                }).collect(Collectors.toList());
        verticalPermissionValidator.checkResourcePermissions(resources, Collections.singletonList("update"));
        ResourceGroup resourceGroup = new ResourceGroup(request.getName(), request.getDescription(),
                request.isEnabled(), resourceGroupRepository, resourceGroupConnectionRepository, authenticationFacade);
        resourceGroup.create();
        log.info("Create a ResourceGroup successfully, resourceGroupId={}", resourceGroup.getId());
        bindConnections(resourceGroup, request.getConnections());
        return resourceGroup;
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_RESOURCE_GROUP", indexOfIdParam = 0)
    public ResourceGroup delete(@NotNull Long resourceGroupId) {
        ResourceGroup resourceGroup = new ResourceGroup(resourceGroupId, resourceGroupRepository,
                resourceGroupConnectionRepository, authenticationFacade);
        horizontalDataPermissionValidator.checkCurrentOrganization(resourceGroup);
        resourceGroup.release();
        resourceGroup.delete();
        log.info("ResourceGroup has been deleted successfully, resourceGroupId={}", resourceGroupId);
        permissionService.deleteResourceRelatedPermissions(resourceGroupId, ResourceType.ODC_RESOURCE_GROUP,
                PermissionType.PUBLIC_RESOURCE);
        permissionService.deleteResourceRelatedPermissions(resourceGroupId, ResourceType.ODC_RESOURCE_GROUP,
                PermissionType.SYSTEM);
        return resourceGroup;
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "update", resourceType = "ODC_RESOURCE_GROUP", indexOfIdParam = 0)
    public ResourceGroup update(@NotNull Long resourceGroupId, @NotNull @Valid ModifyResourceGroupReq request) {
        verticalPermissionValidator.checkResourcePermissions(
                filterExistResources(resourceGroupId, request.getConnections()), Collections.singletonList("update"));
        ResourceGroup resourceGroup = get(resourceGroupId);
        resourceGroup.setDescription(request.getDescription());
        resourceGroup.setName(request.getName());
        resourceGroup.setEnabled(request.isEnabled());
        resourceGroup.update();
        log.info("Update a resource group successfully, resourceGroup={}", request);
        resourceGroup.release();
        log.info("Release all resources successfully, resourceGroup={}", request);
        bindConnections(resourceGroup, request.getConnections());
        return resourceGroup;
    }

    private List<SecurityResource> filterExistResources(Long resourceGroupId, List<ConnectionMetaInfo> existResources) {
        ResourceGroup resourceGroup = get(resourceGroupId);
        List<ResourceIdentifier> identifiers = resourceGroup.getRelatedResources(ResourceType.ODC_CONNECTION);
        return existResources.stream().map(e -> new ResourceIdentifier(e.getId(), ODC_CONNECTION))
                .filter(e -> !identifiers.contains(e))
                .map(e -> new DefaultSecurityResource(e.getResourceId().toString(), e.getResourceType().name()))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "update", resourceType = "ODC_RESOURCE_GROUP", indexOfIdParam = 0)
    public ResourceGroup setEnable(@NotNull Long resourceGroupId, @NotNull Boolean status) {
        ResourceGroup resourceGroup = get(resourceGroupId);
        resourceGroup.setEnabled(status);
        resourceGroup.update();
        log.info("Update the status successfully, resourceGroupId={}, status={}", resourceGroupId, status);
        return resourceGroup;
    }

    @SkipAuthorize("odc internal usage")
    public List<ResourceGroup> batchNullSafeGet(@NonNull Collection<Long> ids) {
        List<ResourceGroupEntity> entities = resourceGroupRepository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(ResourceGroupEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_RESOURCE_GROUP, "id", absentIds);
        }
        return entities.stream().map(e -> new ResourceGroup(e, resourceGroupRepository,
                resourceGroupConnectionRepository, authenticationFacade)).collect(Collectors.toList());
    }

    private void bindConnections(@NonNull ResourceGroup resourceGroup, List<ConnectionMetaInfo> connectionMetaInfos) {
        if (CollectionUtils.isEmpty(connectionMetaInfos)) {
            return;
        }
        Set<ResourceIdentifier> identifiers = new HashSet<>();
        for (ConnectionMetaInfo metaInfo : connectionMetaInfos) {
            if (metaInfo.getId() == null) {
                throw new BadRequestException("Connection Id is null");
            }
            Optional<ConnectionEntity> optional = connectionRepository.findById(metaInfo.getId());
            if (!optional.isPresent()) {
                throw new NotFoundException(ODC_CONNECTION, "ID", metaInfo.getId());
            }
            horizontalDataPermissionValidator
                    .checkCurrentOrganization(ConnectionMapper.INSTANCE.entityToModel(optional.get()));
            identifiers.add(new ResourceIdentifier(metaInfo.getId(), ODC_CONNECTION));
        }
        if (identifiers.isEmpty()) {
            return;
        }
        identifiers = resourceGroup.bind(identifiers);
        log.info("Bind resource to resource group succeed, resourceGroupId={}, resources={}",
                resourceGroup.getId(), identifiers);
    }

}
