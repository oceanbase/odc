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
package com.oceanbase.odc.service.iam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.ResourceRole;
import com.oceanbase.odc.service.iam.model.UserGlobalResourceRole;
import com.oceanbase.odc.service.iam.model.UserResourceRole;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/4 19:18
 * @Description: []
 */
@Service
@Slf4j
public class ResourceRoleService {

    @Autowired
    private ResourceRoleRepository resourceRoleRepository;

    @Autowired
    private UserResourceRoleRepository userResourceRoleRepository;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private GlobalResourceRoleService globalResourceRoleService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    private final ResourceRoleMapper resourceRoleMapper = ResourceRoleMapper.INSTANCE;

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public List<UserResourceRole> saveAll(List<UserResourceRole> userResourceRoleList) {
        return saveAll(userResourceRoleList, authenticationFacade.currentOrganizationId());
    }

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public List<UserResourceRole> saveAll(List<UserResourceRole> userResourceRoleList, @NonNull Long organizationId) {
        if (CollectionUtils.isEmpty(userResourceRoleList)) {
            return Collections.emptyList();
        }
        List<UserResourceRoleEntity> userResourceRoleEntityList = new ArrayList<>();
        Map<ResourceType, Map<ResourceRoleName, ResourceRoleEntity>> resourceRoleMap = resourceRoleRepository.findAll()
                .stream().collect(Collectors.groupingBy(ResourceRoleEntity::getResourceType,
                        Collectors.toMap(ResourceRoleEntity::getRoleName, v -> v, (v1, v2) -> v2)));
        userResourceRoleList.forEach(e -> {
            Map<ResourceRoleName, ResourceRoleEntity> resourceRoleEntityMap = resourceRoleMap.get(e.getResourceType());
            if (Objects.isNull(resourceRoleEntityMap)) {
                throw new UnexpectedException("resource type has no role, type=" + e.getResourceType());
            }
            ResourceRoleEntity resourceRoleEntity = resourceRoleEntityMap.get(e.getResourceRole());
            if (Objects.isNull(resourceRoleEntity)) {
                throw new UnexpectedException("resource role not found, role=" + e.getResourceRole());
            }
            UserResourceRoleEntity entity = new UserResourceRoleEntity();
            entity.setResourceId(e.getResourceId());
            entity.setUserId(e.getUserId());
            entity.setResourceRoleId(resourceRoleEntity.getId());
            entity.setOrganizationId(organizationId);
            userResourceRoleEntityList.add(entity);
        });
        userResourceRoleRepository.batchCreate(userResourceRoleEntityList);
        return userResourceRoleList;
    }

    @SkipAuthorize("odc internal usage")
    public Set<String> getResourceRoleIdentifiersByUserId(long organizationId, long userId) {
        List<UserResourceRoleEntity> userResourceRoleEntities =
                userResourceRoleRepository.findByOrganizationIdAndUserId(organizationId, userId);
        List<PermissionEntity> globalResourceRolePermissions =
                permissionService.findGlobalResourceRolePermissions(userId, organizationId);

        Set<String> resourceRoleIdentifiers = userResourceRoleEntities.stream()
                .map(i -> StringUtils.join(i.getResourceId(), ":", i.getResourceRoleId()))
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(globalResourceRolePermissions)) {
            return resourceRoleIdentifiers;
        }
        // Has global resource role
        Map<String, Long> resourceRoleName2Id = resourceRoleRepository.findByResourceType(ResourceType.ODC_PROJECT)
                .stream().map(resourceRoleMapper::entityToModel)
                .collect(Collectors.toMap(role -> role.getRoleName().name(), ResourceRole::getId, (v1, v2) -> v2));

        Set<String> derivedFromGlobalResourceRole = globalResourceRolePermissions.stream()
                .map(i -> StringUtils.join("*", ":", resourceRoleName2Id.get(i.getAction())))
                .collect(Collectors.toSet());
        derivedFromGlobalResourceRole.addAll(resourceRoleIdentifiers);
        return derivedFromGlobalResourceRole;
    }

    @SkipAuthorize
    public Map<Long, Set<ResourceRoleName>> getProjectId2ResourceRoleNames() {
        return getProjectId2ResourceRoleNames(authenticationFacade.currentUserId(),
                authenticationFacade.currentOrganizationId());
    }

    @SkipAuthorize
    public Map<Long, Set<ResourceRoleName>> getProjectId2ResourceRoleNames(Long userId, Long organizationId) {
        Map<Long, ResourceRole> id2ResourceRoles = resourceRoleRepository.findByResourceType(ResourceType.ODC_PROJECT)
                .stream().map(resourceRoleMapper::entityToModel)
                .collect(Collectors.toMap(ResourceRole::getId, resourceRole -> resourceRole, (v1, v2) -> v2));
        Map<Long, Set<ResourceRoleName>> result = userResourceRoleRepository
                .findByUserIdAndResourceTypeAndOrganizationId(userId, ResourceType.ODC_PROJECT, organizationId).stream()
                .collect(Collectors.groupingBy(UserResourceRoleEntity::getResourceId, Collectors.mapping(
                        e -> id2ResourceRoles.get(e.getResourceRoleId()).getRoleName(), Collectors.toSet())));
        Set<ResourceRoleName> globalResourceRoles =
                permissionService.findGlobalResourceRolePermissions(userId, organizationId).stream()
                        .map(i -> ResourceRoleName.valueOf(i.getAction())).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(globalResourceRoles)) {
            return result;
        }
        projectRepository.findAllByOrganizationId(organizationId).stream().filter(p -> !p.getArchived())
                .forEach(p -> {
                    if (!result.containsKey(p.getId())) {
                        result.put(p.getId(), globalResourceRoles);
                    } else {
                        result.get(p.getId()).addAll(globalResourceRoles);
                    }
                });
        return result;
    }

    @SkipAuthorize("internal usage")
    public Optional<ResourceRoleEntity> findResourceRoleById(@NonNull Long id) {
        return resourceRoleRepository.findById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        List<UserResourceRole> userResourceRoles =
                fromEntities(userResourceRoleRepository.listByResourceTypeAndId(resourceType, resourceId));
        List<UserGlobalResourceRole> globalResourceRoles =
                globalResourceRoleService
                        .findGlobalResourceRoleUsersByOrganizationId(authenticationFacade.currentOrganizationId());
        if (CollectionUtils.isEmpty(globalResourceRoles)) {
            return userResourceRoles;
        }
        globalResourceRoles.stream().map(
                i -> new UserResourceRole(i.getUserId(), resourceId, ResourceType.ODC_PROJECT, i.getResourceRole(),
                        true))
                .forEach(userResourceRoles::add);
        return userResourceRoles;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByResourceTypeAndResourceIdIn(ResourceType resourceType,
            @NotEmpty Collection<Long> resourceIds) {
        List<UserResourceRole> userResourceRoles =
                fromEntities(userResourceRoleRepository.listByResourceTypeAndIdIn(resourceType, resourceIds));
        List<UserGlobalResourceRole> globalResourceRoles =
                globalResourceRoleService
                        .findGlobalResourceRoleUsersByOrganizationId(authenticationFacade.currentOrganizationId());
        if (CollectionUtils.isEmpty(globalResourceRoles)) {
            return userResourceRoles;
        }
        globalResourceRoles.stream().flatMap(i -> resourceIds.stream().map(
                resourceId -> new UserResourceRole(i.getUserId(), resourceId, ResourceType.ODC_PROJECT,
                        i.getResourceRole(), true)))
                .forEach(userResourceRoles::add);
        return userResourceRoles;
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public int deleteByResourceTypeAndIdIn(@NonNull ResourceType resourceType, @NotEmpty Collection<Long> resourceIds) {
        return userResourceRoleRepository.deleteByResourceTypeAndIdIn(resourceType, resourceIds);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public int deleteByUserIdAndResourceIdAndResourceType(@NonNull Long userId, @NonNull Long resourceId,
            @NonNull ResourceType resourceType) {
        return userResourceRoleRepository.deleteByUserIdAndResourceIdAndResourceType(userId, resourceId, resourceType);
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("internal usage")
    public int deleteByUserIdAndResourceIdInAndResourceType(@NonNull Long userId, @NonNull Collection<Long> resourceIds,
            @NonNull ResourceType resourceType) {
        return userResourceRoleRepository.deleteByUserIdAndResourceIdInAndResourceType(userId, resourceIds,
                resourceType);
    }

    @SkipAuthorize("internal usage")
    public int deleteByUserId(@NonNull Long userId) {
        return userResourceRoleRepository.deleteByUserId(userId);
    }

    @SkipAuthorize("internal usage")
    public List<ResourceRole> listResourceRoles(List<ResourceType> resourceType) {
        return resourceRoleRepository.findByResourceTypeIn(resourceType).stream().map(resourceRoleMapper::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("internal authenticated")
    public List<UserResourceRole> listByOrganizationIdAndUserId(Long organizationId, Long userId) {
        List<UserResourceRole> userResourceRoles =
                fromEntities(userResourceRoleRepository.findByOrganizationIdAndUserId(organizationId, userId));
        List<UserGlobalResourceRole> globalResourceRoles =
                globalResourceRoleService.findGlobalResourceRoleUsersByOrganizationIdAndUserId(organizationId, userId);
        if (CollectionUtils.isEmpty(globalResourceRoles)) {
            return userResourceRoles;
        }
        projectRepository.findAllByOrganizationId(organizationId).stream().filter(p -> !p.getArchived())
                .forEach(p -> globalResourceRoles.stream()
                        .map(i -> new UserResourceRole(i.getUserId(), p.getId(), ResourceType.ODC_PROJECT,
                                i.getResourceRole(), true))
                        .forEach(userResourceRoles::add));
        return userResourceRoles;
    }

    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByUserId(Long userId) {
        List<UserResourceRole> userResourceRoles = fromEntities(userResourceRoleRepository.findByUserId(userId));
        List<UserGlobalResourceRole> globalResourceRoles =
                globalResourceRoleService.findGlobalResourceRoleUsersByOrganizationIdAndUserId(
                        authenticationFacade.currentOrganizationId(),
                        userId);
        if (CollectionUtils.isEmpty(globalResourceRoles)) {
            return userResourceRoles;
        }
        projectRepository.findAllByOrganizationId(authenticationFacade.currentOrganizationId()).stream()
                .filter(p -> !p.getArchived()).forEach(p -> globalResourceRoles.stream()
                        .map(i -> new UserResourceRole(i.getUserId(), p.getId(), ResourceType.ODC_PROJECT,
                                i.getResourceRole(), true))
                        .forEach(userResourceRoles::add));
        return userResourceRoles;
    }

    @SkipAuthorize("internal usage")
    public List<UserResourceRole> listByResourceIdAndTypeAndName(Long resourceId, ResourceType resourceType,
            String roleName) {
        List<UserResourceRole> userResourceRoles = fromEntities(
                userResourceRoleRepository.findByResourceIdAndTypeAndName(resourceId, resourceType, roleName));
        List<UserGlobalResourceRole> globalResourceRoles =
                globalResourceRoleService.findGlobalResourceRoleUsersByOrganizationIdAndRole(
                        authenticationFacade.currentOrganizationId(), resourceType, ResourceRoleName.valueOf(roleName));
        if (CollectionUtils.isEmpty(globalResourceRoles)) {
            return userResourceRoles;
        }
        globalResourceRoles.stream().map(i -> new UserResourceRole(i.getUserId(), resourceId, resourceType,
                i.getResourceRole(), true)).forEach(userResourceRoles::add);
        return userResourceRoles;
    }

    private List<UserResourceRole> fromEntities(Collection<UserResourceRoleEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return new ArrayList<>();
        }
        Map<Long, ResourceRoleEntity> id2ResourceRoleEntities = resourceRoleRepository.findAll().stream()
                .collect(Collectors.toMap(ResourceRoleEntity::getId, v -> v, (v1, v2) -> v2));
        return entities.stream().map(e -> {
            ResourceRoleEntity resourceRoleEntity = id2ResourceRoleEntities.get(e.getResourceRoleId());
            if (Objects.isNull(resourceRoleEntity)) {
                throw new UnexpectedException("resource role not found, id=" + e.getResourceRoleId());
            }
            return fromEntity(e, resourceRoleEntity);
        }).collect(Collectors.toList());
    }

    private UserResourceRole fromEntity(UserResourceRoleEntity entity, ResourceRoleEntity resourceRole) {
        UserResourceRole model = new UserResourceRole();
        model.setResourceRole(resourceRole.getRoleName());
        model.setResourceType(resourceRole.getResourceType());
        model.setResourceId(entity.getResourceId());
        model.setUserId(entity.getUserId());
        return model;
    }

}
