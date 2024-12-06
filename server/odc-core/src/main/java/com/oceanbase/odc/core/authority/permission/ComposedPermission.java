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

import java.util.List;

import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2024/11/11 15:11
 * @Description: []
 */
@Getter
public class ComposedPermission implements Permission {
    private final List<Permission> permissions;

    public ComposedPermission(@NonNull List<Permission> permissions) {
        this.permissions = permissions;
    }

    @Override
    public boolean implies(Permission permission) {
        if (!(permission instanceof ComposedPermission)) {
            return false;
        }
        ComposedPermission composedPermission = (ComposedPermission) permission;
        if (CollectionUtils.isEmpty(composedPermission.getPermissions())) {
            return true;
        }
        if (CollectionUtils.isEmpty(this.permissions)) {
            return false;
        }
        for (Permission thatPermission : composedPermission.getPermissions()) {
            for (Permission thisPermission : this.permissions) {
                if (thisPermission.implies(thatPermission)) {
                    return true;
                }
            }
        }
        return false;
    }
}
