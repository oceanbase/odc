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
import java.util.HashSet;
import java.util.Set;

import com.oceanbase.odc.core.shared.PermissionConfiguration;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2023/5/4 20:24
 * @Description: []
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResourceRole implements PermissionConfiguration {
    private Long userId;

    private Long resourceId;

    private ResourceType resourceType;

    private ResourceRoleName resourceRole;

    private boolean derivedFromGlobalProjectRole = false;

    public boolean isProjectMember() {
        return this.resourceType == ResourceType.ODC_PROJECT && (this.resourceRole == ResourceRoleName.OWNER
                || this.resourceRole == ResourceRoleName.DBA
                || this.resourceRole == ResourceRoleName.DEVELOPER
                || this.resourceRole == ResourceRoleName.SECURITY_ADMINISTRATOR
                || this.resourceRole == ResourceRoleName.PARTICIPANT);
    }

    public boolean isProjectOwner() {
        return this.resourceType == ResourceType.ODC_PROJECT && this.resourceRole == ResourceRoleName.OWNER;
    }

    @Override
    public String resourceIdentifier() {
        return this.resourceId == null ? null : this.resourceId.toString();
    }

    @Override
    public Set<String> actions() {
        return new HashSet<>(Collections.singletonList(this.resourceRole.name()));
    }
}
