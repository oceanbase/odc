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
package com.oceanbase.odc.service.session.model;

import java.util.Set;

import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.table.model.Table;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ClassName: UnauthorizedResource Package: com.oceanbase.odc.service.session.model Description:
 *
 * @Author: fenghao
 * @Create 2024/3/20 13:54
 * @Version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnauthorizedResource extends Table {
    private static final long serialVersionUID = 2659094834615671659L;

    private Boolean applicable;
    private String tableName;
    private Long tableId;
    private Set<DatabasePermissionType> unauthorizedPermissionTypes;

    public static UnauthorizedResource from(Table res, Set<DatabasePermissionType> types) {
        UnauthorizedResource obj = new UnauthorizedResource();
        obj.setTableName(res.getName());
        obj.setTableId(res.getId());
        obj.setDatabaseId(res.getDatabaseId());
        obj.setDatabaseName(res.getDatabaseName());
        obj.setDataSourceId(res.getDataSourceId());
        obj.setDataSourceName(res.getDataSourceName());
        obj.setProjectId(res.getProjectId());
        obj.setProjectName(res.getProjectName());
        obj.setUnauthorizedPermissionTypes(types);
        return obj;
    }

    public static UnauthorizedResource from(Database res, Set<DatabasePermissionType> types, boolean applicable) {
        UnauthorizedResource obj = new UnauthorizedResource();
        obj.setDatabaseId(res.getId());
        obj.setDatabaseName(res.getName());
        if (res.getDataSource() != null) {
            obj.setDataSourceId(res.getDataSource().getId());
            obj.setDataSourceName(res.getDataSource().getName());
        }
        if (res.getProject() != null) {
            obj.setProjectId(res.getProject().getId());
            obj.setProjectName(res.getProject().getName());
        }
        obj.setUnauthorizedPermissionTypes(types);
        obj.setApplicable(applicable);
        return obj;
    }
}
