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
package com.oceanbase.odc.service.connection.database.model;

import java.util.Set;

import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/20 13:54
 * @Version 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UnauthorizedDBResource extends DBResource {

    private Boolean applicable;
    private Set<DatabasePermissionType> unauthorizedPermissionTypes;

    public static UnauthorizedDBResource from(DBResource dbResource, Set<DatabasePermissionType> types,
            boolean applicable) {
        UnauthorizedDBResource obj = new UnauthorizedDBResource();
        obj.setType(dbResource.getType());
        obj.setDialectType(dbResource.getDialectType());
        obj.setDataSourceId(dbResource.getDataSourceId());
        obj.setDataSourceName(dbResource.getDataSourceName());
        obj.setDatabaseId(dbResource.getDatabaseId());
        obj.setDatabaseName(dbResource.getDatabaseName());
        obj.setTableId(dbResource.getTableId());
        obj.setTableName(dbResource.getTableName());
        obj.setUnauthorizedPermissionTypes(types);
        obj.setApplicable(applicable);
        return obj;
    }

}
