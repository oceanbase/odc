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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleEntity;
import com.oceanbase.odc.metadb.iam.resourcerole.UserResourceRoleRepository;
import com.oceanbase.odc.service.iam.ResourceRoleBasedPermissionExtractor;
import com.oceanbase.odc.service.iam.model.User;

/**
 * @Author: Lebie
 * @Date: 2023/4/24 17:35
 * @Description: []
 */
public class ResourceRoleAuthorizer extends BaseAuthorizer {
    protected final UserResourceRoleRepository repository;
    protected final ResourceRoleBasedPermissionExtractor permissionMapper;

    public ResourceRoleAuthorizer(UserResourceRoleRepository repository,
            ResourceRoleBasedPermissionExtractor permissionMapper) {
        this.repository = repository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    protected List<Permission> listPermittedPermissions(Principal principal) {
        User odcUser = (User) principal;
        if (Objects.isNull(odcUser.getId())) {
            return Collections.emptyList();
        }
        /**
         * find all user-related resource role, and implies with permissions respectively
         */
        List<UserResourceRoleEntity> resourceRoles =
                repository.findByUserId(odcUser.getId()).stream()
                        .filter(Objects::nonNull).collect(Collectors.toList());
        if (resourceRoles.isEmpty()) {
            return Collections.emptyList();
        }
        return permissionMapper.getResourcePermissions(resourceRoles);
    }
}
