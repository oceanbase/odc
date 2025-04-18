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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.permission.ResourceRoleBasedPermission;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.service.iam.auth.DefaultPermissionProvider;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/4/25 20:36
 * @Description: []
 */
@Component
@Slf4j
public class ResourceRoleBasedPermissionExtractor {
    private final PermissionProvider permissionProvider = new DefaultPermissionProvider();

    @Autowired
    private ResourceRoleRepository repository;

    public List<Permission> getResourcePermissions(List<UserResourceRoleEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.EMPTY_LIST;
        }
        List<Permission> permissions = new ArrayList<>();
        Map<Long, ResourceRoleEntity> id2Role = repository
                .findAllById(entities.stream().map(UserResourceRoleEntity::getResourceRoleId).collect(
                        Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(ResourceRoleEntity::getId, entity -> entity));

        Map<InnerResource, Set<String>> resource2RoleNames = entities.stream()
                .collect(Collectors.groupingBy(entity -> {
                    ResourceRoleEntity role = id2Role.get(entity.getResourceRoleId());
                    if (role == null) {
                        throw new UnexpectedException("resource role not found, id: " + entity.getResourceRoleId());
                    }
                    return new InnerResource(entity.getResourceId(), role.getResourceType());
                }, Collectors.mapping(entity -> id2Role.get(entity.getResourceRoleId()).getRoleName().name(),
                        Collectors.toSet())));

        for (Entry<InnerResource, Set<String>> entry : resource2RoleNames.entrySet()) {
            InnerResource resource = entry.getKey();
            Permission permission =
                    permissionProvider.getPermissionByResourceRoles(
                            new DefaultSecurityResource(String.valueOf(resource.resourceId),
                                    resource.resourceType.name()),
                            entry.getValue());
            if (!(permission instanceof ResourceRoleBasedPermission)) {
                throw new IllegalStateException("Permission's type is illegal " + permission);
            }
            permissions.add(permission);
        }
        return permissions;
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    private class InnerResource {
        Long resourceId;
        ResourceType resourceType;
    }
}
