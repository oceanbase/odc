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
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.SecurityManager;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.auth.AuthorizationFacade;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2023/5/4 15:17
 */
@Component
public class VerticalPermissionValidator {
    @Autowired
    private AuthorizationFacade authorizationFacade;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private SecurityManager securityManager;
    @Autowired
    private PermissionRepository repository;
    @Autowired
    private ResourcePermissionExtractor permissionMapper;

    @SkipAuthorize("internal usage only")
    public final <T extends SecurityResource> void checkResourcePermissions(T resource, List<String> actions) {
        checkResourcePermissions(Collections.singletonList(resource), actions);
    }

    @SkipAuthorize("internal usage only")
    public void checkResourcePermissions(List<SecurityResource> resources, List<String> actions) {
        Validate.notNull(actions, "Resources can not be null for checking resource permissions");
        Validate.notEmpty(actions, "Actions can not be empty for checking resource permissions");
        if (CollectionUtils.isEmpty(resources)) {
            return;
        }
        List<Permission> permissions = new ArrayList<>();
        for (SecurityResource resource : resources) {
            permissions.add(securityManager.getPermissionByActions(resource, actions));
        }
        boolean checkResult = authorizationFacade.isImpliesPermissions(authenticationFacade.currentUser(), permissions);
        if (!checkResult) {
            String errMsg = "Cannot grant permissions that the current user does not have";
            throw new BadRequestException(ErrorCodes.GrantPermissionFailed, new Object[] {errMsg}, errMsg);
        }
    }

    @SkipAuthorize("internal usage only")
    public final <T extends SecurityResource> boolean implies(T resource, List<String> actions,
            @NonNull Long organizationId) {
        Validate.notNull(actions, "Resources can not be null for checking resource permissions");
        Validate.notEmpty(actions, "Actions can not be empty for checking resource permissions");
        List<Permission> permittedPermissions = getAllPermissions(authenticationFacade.currentUserId(), organizationId);
        Permission actualPermission = securityManager.getPermissionByActions(resource, actions);
        for (Permission permittedPermission : permittedPermissions) {
            if (permittedPermission.implies(actualPermission)) {
                return true;
            }
        }
        return false;
    }

    private List<Permission> getAllPermissions(Long userId, Long organizationId) {
        List<PermissionEntity> permissionEntityList = repository
                .findByUserIdAndRoleStatusAndOrganizationId(userId, true, organizationId)
                .stream().filter(permission -> !Objects.isNull(permission)).collect(Collectors.toList());
        return permissionMapper.getResourcePermissions(permissionEntityList);
    }
}
