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

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2022/12/1 19:49
 */
@Component
public class ResourcePermissionAccessor {
    @Autowired
    AuthorizationFacade authorizationFacade;

    @Autowired
    SecurityManager securityManager;

    /**
     * Get permitted resource ids and related actions according to user ID
     * 
     * @param userId Specify user, references {@link User#id()}
     * @param resourceType Specify resource type, references {@link ResourceType}
     * @param predicate Specify predict function, references {@link Predicate}
     * @return Permitted resource ids {@link SecurityResource#resourceId()} and related actions
     */
    public Map<String, Set<String>> permittedResourceActions(@NonNull Long userId,
            @NonNull ResourceType resourceType, @NonNull Predicate<ResourcePermission> predicate) {
        return authorizationFacade.getRelatedResourcesAndActions(User.of(userId)).entrySet().stream().filter(entry -> {
            if (!resourceType.name().equals(entry.getKey().resourceType())) {
                return false;
            }
            ResourcePermission permission = new ResourcePermission(entry.getKey(), String.join(",", entry.getValue()));
            return predicate.test(permission);
        }).collect(Collectors.toMap(key -> key.getKey().resourceId(), Entry::getValue));
    }

    /**
     * Get permitted users and related actions according to authorized resource
     * 
     * @param resourceType Specify resource type, references {@link ResourceType}
     * @param resourceId Specify resource ID, references {@link SecurityResource#resourceId()}
     * @param predicate Specify predict function, references {@link Predicate}
     * @return Permitted users {@link User} and related actions
     */
    public Map<User, Set<String>> permittedUserActions(@NonNull ResourceType resourceType, @NonNull String resourceId,
            @NonNull Predicate<Permission> predicate) {
        Map<User, Set<String>> user2AccessActions;
        if (resourceType == ResourceType.ODC_RESOURCE_GROUP) {
            user2AccessActions = authorizationFacade.getRelatedUsersAndPermittedActions(resourceId);
        } else {
            user2AccessActions = authorizationFacade.getRelatedUsersAndPermittedActions(resourceType, resourceId,
                    PermissionType.PUBLIC_RESOURCE);
        }
        Map<User, Set<String>> user2ManagementActions =
                authorizationFacade.getRelatedUsersAndPermittedActions(resourceType, resourceId, PermissionType.SYSTEM);
        Map<User, Set<String>> permittedUserActions = user2AccessActions;
        for (User user : user2ManagementActions.keySet()) {
            Set<String> actions = permittedUserActions.computeIfAbsent(user, i -> new HashSet<>());
            actions.addAll(user2ManagementActions.get(user));
        }
        return permittedUserActions.entrySet().stream().filter(entry -> {
            Permission permission = securityManager.getPermissionByActions(entry.getKey(), entry.getValue());
            return predicate.test(permission);
        }).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
}
