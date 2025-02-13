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

import java.util.Collection;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;

/**
 * Permission provides an interface, which is mainly used to map out corresponding permissions based
 * on one or a group of operations of the user
 *
 * @author yh263208
 * @date 2021-07-12 20:01
 * @since ODC_release_3.2.0
 */
public interface PermissionProvider {
    /**
     * Get permission interface, the caller obtains the permission corresponding to a set of operations
     * through this interface
     *
     * @param resource {@link SecurityResource}
     * @param actions action collection
     * @return permission collection
     */
    Permission getPermissionByActions(SecurityResource resource, Collection<String> actions);

    /**
     * get permission by resource role
     *
     * @param resource {@link SecurityResource}
     * @param resourceRoles, see {@link ResourceRoleName} enums
     * @return permission collection
     */
    Permission getPermissionByResourceRoles(SecurityResource resource, Collection<String> resourceRoles);

    /**
     * @param resource {@link SecurityResource}
     * @param actions action collection
     * @param resourceRoles, see {@link ResourceRoleName} enums
     * @return permission collection
     */
    Permission getPermissionByActionsAndResourceRoles(SecurityResource resource, Collection<String> actions,
            Collection<String> resourceRoles);

}
