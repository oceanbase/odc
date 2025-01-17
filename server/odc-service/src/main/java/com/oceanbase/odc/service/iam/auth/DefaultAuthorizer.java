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

import com.oceanbase.odc.core.authority.auth.Authorizer;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.service.iam.ResourcePermissionExtractor;
import com.oceanbase.odc.service.iam.model.User;

/**
 * Implements for <code>Authorizer</code>, just for ODC application
 *
 * @author yh263208
 * @date 2021-08-02 16:23
 * @since ODC-release_3.2.0
 * @see Authorizer
 */
public class DefaultAuthorizer extends BaseAuthorizer {

    protected final PermissionRepository repository;
    protected final ResourcePermissionExtractor permissionMapper;

    public DefaultAuthorizer(PermissionRepository repository, ResourcePermissionExtractor permissionMapper) {
        this.repository = repository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    protected List<Permission> listPermittedPermissions(Principal principal) {
        User odcUser = (User) principal;
        if (Objects.isNull(odcUser.getId())) {
            return Collections.emptyList();
        }
        List<PermissionEntity> permissionEntities = repository.findByUserIdAndUserStatusAndRoleStatusAndOrganizationId(
                odcUser.getId(), true, true, odcUser.getOrganizationId()).stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        return permissionMapper.getResourcePermissions(permissionEntities);
    }
}
