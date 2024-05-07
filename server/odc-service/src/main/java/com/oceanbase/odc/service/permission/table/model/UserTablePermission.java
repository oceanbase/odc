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
package com.oceanbase.odc.service.permission.table.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ClassName: UserTablePermission Package: com.oceanbase.odc.service.permission.table.model.table
 * Description:
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:39
 * @Version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserTablePermission extends UserDatabasePermission {

    @JsonProperty(access = Access.READ_ONLY)
    private String tableId;

    @JsonProperty(access = Access.READ_ONLY)
    private String tableName;

    public static UserTablePermission from(Long permissionId) {
        UserTablePermission permission = new UserTablePermission();
        permission.setId(permissionId);
        return permission;
    }

}
