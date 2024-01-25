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

import java.util.Collection;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.authority.permission.ConnectionPermission;
import com.oceanbase.odc.core.authority.permission.DatabasePermission;
import com.oceanbase.odc.core.authority.permission.Permission;
import com.oceanbase.odc.core.authority.permission.PermissionProvider;
import com.oceanbase.odc.core.authority.permission.PrivateConnectionPermission;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.permission.ResourceRoleBasedPermission;
import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Implements for <code>PermissionProvider</code>, just for ODC application
 *
 * @author yh263208
 * @date 2021-08-03 15:43
 * @since ODC_release_3.2.0
 * @see PermissionProvider
 */
public class DefaultPermissionProvider implements PermissionProvider {

    @Override
    public Permission getPermissionByActions(SecurityResource resource, Collection<String> actions) {
        if (ResourceType.ODC_CONNECTION.name().equals(resource.resourceType())) {
            return new ConnectionPermission(resource.resourceId(), String.join(",", actions));
        } else if (ResourceType.ODC_PRIVATE_CONNECTION.name().equals(resource.resourceType())) {
            return new PrivateConnectionPermission(resource.resourceId(), String.join(",", actions));
        } else if (ResourceType.ODC_DATABASE.name().equals(resource.resourceType())) {
            return new DatabasePermission(resource.resourceId(), String.join(",", actions));
        }
        return new ResourcePermission(resource, String.join(",", actions));
    }

    @Override
    public Permission getPermissionByResourceRoles(SecurityResource resource, Collection<String> resourceRoles) {
        return new ResourceRoleBasedPermission(resource, String.join(",", resourceRoles));
    }

}
