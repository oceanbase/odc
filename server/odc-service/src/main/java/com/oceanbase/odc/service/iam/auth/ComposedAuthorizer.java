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

import org.apache.commons.collections.ListUtils;

import com.oceanbase.odc.core.authority.permission.ComposedPermission;
import com.oceanbase.odc.core.authority.permission.Permission;

/**
 * @Author: Lebie
 * @Date: 2024/11/11 16:05
 * @Description: []
 */
public class ComposedAuthorizer extends BaseAuthorizer {
    private final DefaultAuthorizer defaultAuthorizer;
    private final ResourceRoleAuthorizer resourceRoleAuthorizer;

    public ComposedAuthorizer(DefaultAuthorizer defaultAuthorizer, ResourceRoleAuthorizer resourceRoleAuthorizer) {
        this.defaultAuthorizer = defaultAuthorizer;
        this.resourceRoleAuthorizer = resourceRoleAuthorizer;
    }

    @Override
    protected List<Permission> listPermittedPermissions(Principal principal) {
        return Collections.singletonList(
                new ComposedPermission(ListUtils.union(defaultAuthorizer.listPermittedPermissions(principal),
                        resourceRoleAuthorizer.listPermittedPermissions(principal))));
    }
}
