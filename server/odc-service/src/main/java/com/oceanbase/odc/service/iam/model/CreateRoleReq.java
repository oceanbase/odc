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
package com.oceanbase.odc.service.iam.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.constraints.Size;

import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2021/6/28
 */

@Data
public class CreateRoleReq {
    @Size(min = 1, max = 64, message = "Role name is out of range [1,64]")
    @Name(message = "Role name cannot start or end with whitespaces")
    private String name;
    private boolean enabled;
    private List<PermissionConfig> connectionAccessPermissions;
    private List<PermissionConfig> resourceManagementPermissions;
    private List<PermissionConfig> systemOperationPermissions;
    private String description;

    public static void requestParamFilter(CreateRoleReq req) {
        if (Objects.nonNull(req.systemOperationPermissions)) {
            for (PermissionConfig config : req.systemOperationPermissions) {
                if (config.getResourceType() != ResourceType.ODC_PRIVATE_CONNECTION
                        && config.getActions().contains("use")) {
                    config.getActions().remove("use");
                }
            }
        }
    }

    public List<PermissionConfig> nullSafeGetConnectionAccessPermissions() {
        return nullSafeGet(connectionAccessPermissions);
    }

    public List<PermissionConfig> nullSafeGetResourceManagementPermissions() {
        return nullSafeGet(resourceManagementPermissions);
    }

    public List<PermissionConfig> nullSafeGetSystemOperationPermissions() {
        return nullSafeGet(systemOperationPermissions);
    }

    public static List<PermissionConfig> nullSafeGet(List<PermissionConfig> permissionConfigs) {
        return MoreObjects.firstNonNull(permissionConfigs, Collections.emptyList());
    }
}


