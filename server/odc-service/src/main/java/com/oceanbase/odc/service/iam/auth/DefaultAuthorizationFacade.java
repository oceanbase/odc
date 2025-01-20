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
package com.oceanbase.odc.service.iam.auth;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.authority.permission.DatabasePermission;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.PrivateConnectionPermission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.permission.ResourceRoleBasedPermission;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.PermissionSpecs;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.ResourcePermissionExtractor;
import com.oceanbase.odc.service.iam.ResourceRoleBasedPermissionExtractor;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.resourcegroup.model.ResourceIdentifier;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class DefaultAuthorizationFacade implements AuthorizationFacade {

    @Autowired
    private ResourcePermissionExtractor permissionMapper;
    @Autowired
    private ResourceRoleBasedPermissionExtractor resourceRoleBasedPermissionExtractor;
    @Autowired
    private PermissionRepository repository;
    @Autowired
    private UserResourceRoleRepository resourceRoleService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    @Qualifier("authorizationFacadeExecutor")
    private ThreadPoolTaskExecutor authorizationFacadeExecutor;

    @Override
    public Set<String> getAllPermittedActions(Principal principal, ResourceType resourceType, String resourceId) {
        List<Permission> permissions = getAllPermissions(principal);
        Set<String> returnVal = new HashSet<>();
        for (Permission permission : permissions) {
            if (permission instanceof ResourcePermission) {
                if (resourceType.equals(ResourceType.valueOf(((ResourcePermission) permission).getResourceType()))
                        && ("*".equals(((ResourcePermission) permission).getResourceId())
                                || resourceId.equals(((ResourcePermission) permission).getResourceId()))) {
                    if (resourceType == ResourceType.ODC_PRIVATE_CONNECTION) {
                        returnVal.addAll(
                                PrivateConnectionPermission.getActionList(((ResourcePermission) permission).getMask()));
                    } else if (resourceType == ResourceType.ODC_CONNECTION) {
                        returnVal.addAll(
                                ConnectionPermission.getActionList(((ResourcePermission) permission).getMask()));
                    } else if (resourceType == ResourceType.ODC_DATABASE) {
                        returnVal.addAll(DatabasePermission.getActionList(((ResourcePermission) permission).getMask()));
                    } else {
                        returnVal.addAll(ResourcePermission.getActionList(((ResourcePermission) permission).getMask()));
                    }
                }
            }
        }
        return returnVal;
    }

    @Override
    public Map<User, Set<String>> getRelatedUsersAndPermittedActions(ResourceType resourceType, String resourceId,
            PermissionType type) {
        Specification<PermissionEntity> specification =
                Specification.where(PermissionSpecs.resourceTypeEquals(resourceType))
                        .and(PermissionSpecs.typeEquals(type));
        Map<ResourceIdentifier, List<ResourceIdentifier>> resourceGroupCache = new ConcurrentHashMap<>();
        List<List<PermissionEntity>> splitedList =
                splitListByLimit(repository.findAll(specification), authorizationFacadeExecutor.getCorePoolSize());
        CompletionService<List<PermissionEntity>> completionService =
                new ExecutorCompletionService<>(authorizationFacadeExecutor);
        for (List<PermissionEntity> subList : splitedList) {
            completionService.submit(
                    new PermissionFilterTask(subList, resourceGroupCache, permissionMapper, resourceType, resourceId));
        }
        List<PermissionEntity> entities = new LinkedList<>();
        int size = splitedList.size();
        for (int i = 0; i < size; i++) {
            try {
                List<PermissionEntity> filteredList = completionService.take().get();
                entities.addAll(filteredList);
            } catch (Exception e) {
                log.warn("Fail to get the PermissionFilterTask's result", e);
            }
        }
        return findActionsByPermissionEntities(entities);
    }

    @Override
    public Map<User, Set<String>> getRelatedUsersAndPermittedActions(String resourceGroupId) {
        Specification<PermissionEntity> specification =
                Specification.where(PermissionSpecs.resourceTypeEquals(ResourceType.ODC_RESOURCE_GROUP))
                        .and(PermissionSpecs.typeEquals(PermissionType.PUBLIC_RESOURCE));
        String stringStartWith = ResourceType.ODC_RESOURCE_GROUP.name() + ":" + resourceGroupId;
        List<PermissionEntity> entities = repository.findAll(specification).stream()
                .filter(permissionEntity -> permissionEntity.getResourceIdentifier().startsWith(stringStartWith))
                .collect(Collectors.toList());
        return findActionsByPermissionEntities(entities);
    }

    @Override
    public Map<SecurityResource, Set<String>> getRelatedResourcesAndActions(Principal principal) {
        List<Permission> permissions = getAllPermissions(principal).stream()
                .filter(permission -> !(permission instanceof ResourceRoleBasedPermission)).collect(
                        Collectors.toList());
        Map<SecurityResource, Set<String>> returnVal = new HashMap<>();
        for (Permission permission : permissions) {
            SecurityResource resource = new DefaultSecurityResource(((ResourcePermission) permission).getResourceId(),
                    ((ResourcePermission) permission).getResourceType());
            Set<String> actions = returnVal.computeIfAbsent(resource, identifier -> new HashSet<>());
            if (ResourceType.ODC_DATABASE.name().equals(resource.resourceType())) {
                actions.addAll(DatabasePermission.getActionList(((ResourcePermission) permission).getMask()));
            } else if (ResourceType.ODC_PRIVATE_CONNECTION.name().equals(resource.resourceType())) {
                actions.addAll(PrivateConnectionPermission.getActionList(((ResourcePermission) permission).getMask()));
            } else if (ResourceType.ODC_CONNECTION.name().equals(resource.resourceType())) {
                actions.addAll(ConnectionPermission.getActionList(((ResourcePermission) permission).getMask()));
            } else {
                actions.addAll(ResourcePermission.getActionList(((ResourcePermission) permission).getMask()));
            }
        }
        return returnVal;
    }

    @Override
    public boolean isImpliesPermissions(@NotNull Principal principal, @NotNull Collection<Permission> permissions) {
        List<Permission> permittedPermissions = getAllPermissions(principal);
        for (Permission permission : permissions) {
            boolean implies = false;
            for (Permission permittedPermission : permittedPermissions) {
                if (permittedPermission.implies(permission)) {
                    implies = true;
                    break;
                }
            }
            if (!implies) {
                return false;
            }
        }
        return true;
    }

    private User entityToUser(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setType(entity.getType());
        user.setName(entity.getName());
        user.setAccountName(entity.getAccountName());
        user.setOrganizationId(entity.getOrganizationId());
        user.setEnabled(entity.isEnabled());
        user.setActive(entity.isActive());
        return user;
    }

    private List<Permission> getAllPermissions(Principal principal) {
        if (!(principal instanceof User)) {
            throw new InternalServerError("Principal has to be an instance of User");
        }
        User odcUser = (User) principal;
        List<PermissionEntity> permissionEntityList = repository
                .findByUserIdAndRoleStatusAndOrganizationId(odcUser.getId(), true,
                        authenticationFacade.currentOrganizationId())
                .stream().filter(permission -> !Objects.isNull(permission)).collect(Collectors.toList());
        List<UserResourceRoleEntity> resourceRoles =
                resourceRoleService
                        .findByOrganizationIdAndUserId(authenticationFacade.currentOrganizationId(), odcUser.getId())
                        .stream()
                        .filter(Objects::nonNull).collect(Collectors.toList());
        return ListUtils.union(permissionMapper.getResourcePermissions(permissionEntityList),
                resourceRoleBasedPermissionExtractor.getResourcePermissions(resourceRoles));
    }

    private Map<User, Set<String>> findActionsByPermissionEntities(List<PermissionEntity> entities) {
        Map<User, Set<String>> returnVal = new HashMap<>();
        entities.forEach(permissionEntity -> {
            List<User> users = userRepository
                    .findByPermissionIdAndOrganizationId(permissionEntity.getId(),
                            authenticationFacade.currentOrganizationId(), true)
                    .stream()
                    .map(this::entityToUser).collect(Collectors.toList());
            for (User user : users) {
                Set<String> actions = returnVal.computeIfAbsent(user, user1 -> new HashSet<>());
                actions.add(permissionEntity.getAction());
            }
        });
        return returnVal;
    }

    private List<List<PermissionEntity>> groupByResourceIdentifier(List<PermissionEntity> entities) {
        Map<String, List<PermissionEntity>> identifier2Permissions = new HashMap<>();
        for (PermissionEntity entity : entities) {
            List<PermissionEntity> entities1 =
                    identifier2Permissions.computeIfAbsent(entity.getResourceIdentifier(), s -> new LinkedList<>());
            entities1.add(entity);
        }
        return new ArrayList<>(identifier2Permissions.values());
    }

    private List<List<PermissionEntity>> splitListByLimit(@NonNull List<PermissionEntity> list, int limit) {
        Validate.isTrue(limit > 0, "Count can not be zero or negative");
        List<List<PermissionEntity>> returnVal = groupByResourceIdentifier(list);
        if (returnVal.size() <= limit) {
            return returnVal;
        }
        Map<Integer, Integer> index2ListSize = new HashMap<>();
        int retrunValSize = returnVal.size();
        for (int i = 0; i < retrunValSize; i++) {
            List<PermissionEntity> subList = returnVal.get(i);
            index2ListSize.putIfAbsent(i, subList.size());
        }
        List<Map.Entry<Integer, Integer>> sortedList = new ArrayList<>(index2ListSize.entrySet());
        sortedList.sort(Entry.comparingByValue());
        int interval = returnVal.size() - limit;
        List<Integer> deleteIndex = new LinkedList<>();
        for (int i = 0, j = 0; i < interval; i++, j++) {
            int currentIndex = sortedList.get(i).getKey();
            deleteIndex.add(currentIndex);
            if (interval + j >= sortedList.size()) {
                j = 0;
            }
            int targetIndex = sortedList.get(interval + j).getKey();
            List<PermissionEntity> currentEntities = returnVal.get(currentIndex);
            List<PermissionEntity> targetEntities = returnVal.get(targetIndex);
            targetEntities.addAll(currentEntities);
        }
        deleteIndex.sort(Comparator.naturalOrder());
        for (int i = 0; i < interval; i++) {
            int index = deleteIndex.get(i) - i;
            returnVal.remove(index);
        }
        return returnVal;
    }

    private static class PermissionFilterTask implements Callable<List<PermissionEntity>> {
        private final List<PermissionEntity> targetList;
        private final ResourcePermissionExtractor permissionMapper;
        private final ResourceType resourceType;
        private final String resourceId;
        private final Map<ResourceIdentifier, List<ResourceIdentifier>> resourceGroupCache;

        public PermissionFilterTask(List<PermissionEntity> targetList,
                Map<ResourceIdentifier, List<ResourceIdentifier>> resourceGroupCache,
                ResourcePermissionExtractor permissionMapper, ResourceType resourceType,
                String resourceId) {
            this.targetList = targetList;
            this.permissionMapper = permissionMapper;
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.resourceGroupCache = resourceGroupCache;
        }

        @Override
        public List<PermissionEntity> call() {
            return targetList.stream().filter(permissionEntity -> {
                List<SecurityResource> resources = permissionMapper.getResourcesByIdentifier(
                        permissionEntity.getResourceIdentifier(), resourceGroupCache);
                for (SecurityResource resource : resources) {
                    String targetId = resource.resourceId();
                    if (resourceType.name().equals(resource.resourceType())
                            && ("*".equals(targetId) || resourceId.equals(targetId))) {
                        return true;
                    }
                }
                return false;
            }).collect(Collectors.toList());
        }
    }

}
