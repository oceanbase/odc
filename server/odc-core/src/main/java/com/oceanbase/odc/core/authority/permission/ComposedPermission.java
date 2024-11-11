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
package com.oceanbase.odc.core.authority.permission;

import com.oceanbase.odc.core.authority.model.SecurityResource;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/11/11 15:11
 * @Description: []
 */
@Getter
public class ComposedPermission implements Permission {
    private final ResourceRoleBasedPermission resourceRolePermission;
    private final ResourcePermission resourcePermission;
    protected final String resourceId;
    protected final String resourceType;

    public ComposedPermission(@NonNull SecurityResource resource,
            @NonNull ResourceRoleBasedPermission resourceRolePermission,
            @NonNull ResourcePermission resourcePermission) {
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        this.resourceRolePermission = resourceRolePermission;
        this.resourcePermission = resourcePermission;
    }

    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof ComposedPermission)) {
            return false;
        }
        ComposedPermission that = (ComposedPermission) permission;
        return this.resourceId.equalsIgnoreCase(that.getResourceId())
                && this.resourceType.equalsIgnoreCase(that.getResourceType())
                && resourceRolePermission.implies(that.resourceRolePermission)
                && resourcePermission.implies(that.resourcePermission);
    }
}
