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
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.iam.model.User;

/**
 * {@link AuthorizationFacade}
 *
 * @author yh263208
 * @date 2021-08-06 10:34
 * @since ODC_release_3.2.0
 */
@Validated
public interface AuthorizationFacade {

    @NotNull
    Set<String> getAllPermittedActions(@NotNull Principal principal, @NotNull ResourceType resourceType,
            @NotNull String resourceId);

    @NotNull
    Map<User, Set<String>> getRelatedUsersAndPermittedActions(@NotNull ResourceType resourceType,
            @NotNull String resourceId, PermissionType type);

    @NotNull
    Map<User, Set<String>> getRelatedUsersAndPermittedActions(@NotNull String resourceGroupId);

    @NotNull
    Map<SecurityResource, Set<String>> getRelatedResourcesAndActions(@NotNull Principal principal);

    boolean isImpliesPermissions(@NotNull Principal principal, @NotNull Collection<Permission> permissions);

}
