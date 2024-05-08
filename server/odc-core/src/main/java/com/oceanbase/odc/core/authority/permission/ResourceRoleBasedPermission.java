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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/4/24 17:18
 * @Description: []
 */
@Getter
public class ResourceRoleBasedPermission implements Permission {

    protected final String resourceId;
    protected final String resourceType;
    protected final List<ResourceRoleName> resourceRoles;

    public ResourceRoleBasedPermission(@NonNull SecurityResource resource, @NonNull String resourceRole) {
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        this.resourceRoles = Arrays.stream(StringUtils.split(resourceRole, ","))
                .map(e -> ResourceRoleName.valueOf(e.trim().toUpperCase())).collect(Collectors.toList());
        Validate.notNull(resourceId, "ResourceId can not be null");
        Validate.notNull(resourceType, "ResourceType can not be null");
    }

    public ResourceRoleBasedPermission(@NonNull SecurityResource resource, List<ResourceRoleName> resourceRoles) {
        this.resourceId = resource.resourceId();
        this.resourceType = resource.resourceType();
        this.resourceRoles = resourceRoles;
        Validate.notNull(resourceId, "ResourceId can not be null");
        Validate.notNull(resourceType, "ResourceType can not be null");
        Validate.notEmpty(resourceRoles, "resourceRoles can not be empty");
    }

    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof ResourceRoleBasedPermission)) {
            return false;
        }
        return this.resourceId.equalsIgnoreCase(((ResourceRoleBasedPermission) permission).getResourceId())
                && this.resourceType.equalsIgnoreCase(((ResourceRoleBasedPermission) permission).getResourceType())
                && !Collections.disjoint(((ResourceRoleBasedPermission) permission).getResourceRoles(),
                        this.resourceRoles);
    }

    @Override
    public String toString() {
        String resource = this.resourceType;
        try {
            resource = ResourceType.valueOf(this.resourceType).getLocalizedMessage();
        } catch (Exception e) {
            // eat exception
        }
        StringBuilder res = new StringBuilder(resource + ":" + this.resourceId);
        if (CollectionUtils.isEmpty(this.resourceRoles)) {
            return res.toString();
        }
        return res.append(": ").append(this.resourceRoles.stream().map(
                ResourceRoleName::getLocalizedMessage).collect(Collectors.joining(","))).toString();
    }

}
