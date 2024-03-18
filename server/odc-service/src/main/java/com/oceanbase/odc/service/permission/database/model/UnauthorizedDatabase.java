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
package com.oceanbase.odc.service.permission.database.model;

import java.util.Set;

import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author gaoda.xy
 * @date 2024/1/4 17:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnauthorizedDatabase extends Database {

    private static final long serialVersionUID = 2659094834615671659L;

    private Set<DatabasePermissionType> unauthorizedPermissionTypes;

    private Boolean applicable;

    public static UnauthorizedDatabase from(Database database, Set<DatabasePermissionType> types, boolean applicable) {
        UnauthorizedDatabase obj = new UnauthorizedDatabase();
        obj.setId(database.getId());
        obj.setName(database.getName());
        obj.setDataSource(database.getDataSource());
        obj.setProject(database.getProject());
        obj.setUnauthorizedPermissionTypes(types);
        obj.setApplicable(applicable);
        return obj;
    }

}
