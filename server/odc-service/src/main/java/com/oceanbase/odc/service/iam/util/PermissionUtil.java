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
package com.oceanbase.odc.service.iam.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.service.iam.model.PermissionConfig;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;

/**
 * @author wenniu.ly
 * @date 2021/7/21
 */
public class PermissionUtil {
    public static List<PermissionConfig> aggregatePermissions(List<PermissionEntity> permissionEntities) {
        List<PermissionConfig> permissionConfigs = new ArrayList<>();
        Map<String, Set<String>> identifier2Actions = new HashMap<>();
        for (PermissionEntity permissionEntity : permissionEntities) {
            if (Objects.nonNull(permissionEntity)) {
                Set<String> action = identifier2Actions.computeIfAbsent(permissionEntity.getResourceIdentifier(),
                        e -> new HashSet<>());
                action.add(permissionEntity.getAction());
            }
        }
        for (Map.Entry<String, Set<String>> entry : identifier2Actions.entrySet()) {
            ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(entry.getKey());
            permissionConfigs.add(new PermissionConfig(resourceContext.getId(),
                    ResourceType.valueOf(resourceContext.getField()), new ArrayList<>(entry.getValue())));
        }
        return permissionConfigs;
    }

    public static List<PermissionConfig> aggregateResourceManagementPermissions(
            List<PermissionEntity> permissionEntities) {
        List<PermissionConfig> permissionConfigs = new ArrayList<>();
        Map<String, Set<String>> identifier2Actions = new HashMap<>();
        for (PermissionEntity permissionEntity : permissionEntities) {
            if (Objects.nonNull(permissionEntity)) {
                Set<String> action = identifier2Actions.computeIfAbsent(permissionEntity.getResourceIdentifier(),
                        e -> new HashSet<>());
                action.add(permissionEntity.getAction());
            }
        }
        for (Map.Entry<String, Set<String>> entry : identifier2Actions.entrySet()) {
            ResourceContext resourceContext = ResourceContextUtil.parseFromResourceIdentifier(entry.getKey());
            // Separately extract the permission configuration with "create" for resource management permissions
            if (entry.getValue().contains("create") && entry.getValue().size() > 1) {
                permissionConfigs.add(new PermissionConfig(resourceContext.getId(),
                        ResourceType.valueOf(resourceContext.getField()), Collections.singletonList("create")));
                entry.getValue().remove("create");
            }
            permissionConfigs.add(new PermissionConfig(resourceContext.getId(),
                    ResourceType.valueOf(resourceContext.getField()), new ArrayList<>(entry.getValue())));
        }
        return permissionConfigs;
    }

    public static boolean isConnectionAccessPermission(PermissionEntity permission) {
        if (Objects.isNull(permission)) {
            return false;
        }
        if (PermissionType.PUBLIC_RESOURCE == permission.getType()) {
            String action = permission.getAction();
            Set<String> validActions = new HashSet<>(Arrays.asList("connect", "readonlyconnect", "apply"));
            return validActions.contains(action);
        }
        return false;
    }

    public static boolean isResourceManagementPermission(PermissionEntity permission) {
        if (Objects.isNull(permission)) {
            return false;
        }
        if (PermissionType.PUBLIC_RESOURCE == permission.getType()) {
            String action = permission.getAction();
            Set<String> invalidActions = new HashSet<>(Arrays.asList("connect", "readonlyconnect", "apply"));
            return !invalidActions.contains(action);
        } else if (PermissionType.SYSTEM == permission.getType()) {
            String resourceIdentifier = permission.getResourceIdentifier();
            return resourceIdentifier.startsWith(ResourceType.ODC_CONNECTION.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_ROLE.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_USER.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_PROJECT.name());
        }
        return false;
    }

    public static boolean isSystemOperationPermission(PermissionEntity permission) {
        if (Objects.isNull(permission)) {
            return false;
        }
        if (PermissionType.SYSTEM == permission.getType()) {
            String resourceIdentifier = permission.getResourceIdentifier();
            return !(resourceIdentifier.startsWith(ResourceType.ODC_CONNECTION.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_RESOURCE_GROUP.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_DATA_MASKING_RULE.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_SYSTEM_CONFIG.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_ROLE.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_USER.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_FLOW_CONFIG.name())
                    || resourceIdentifier.startsWith(ResourceType.ODC_PROJECT.name()));
        }
        return false;
    }

}
