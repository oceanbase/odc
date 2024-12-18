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

import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.service.iam.util.GlobalResourceRoleUtil;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/11/19 17:15
 * @Description: []
 */
@Data
public class UserGlobalResourceRole {
    private Long userId;
    private ResourceRoleName resourceRole;

    public UserGlobalResourceRole(Long userId, String globalRoleName) {
        this.userId = userId;
        this.resourceRole = GlobalResourceRoleUtil.getResourceRoleName(globalRoleName);
    }
}
