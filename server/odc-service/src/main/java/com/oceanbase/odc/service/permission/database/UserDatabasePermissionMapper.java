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
package com.oceanbase.odc.service.permission.database;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.iam.UserDatabasePermissionEntity;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;

/**
 * @author gaoda.xy
 * @date 2024/1/11 15:44
 */
@Mapper
public interface UserDatabasePermissionMapper {

    UserDatabasePermissionMapper INSTANCE = Mappers.getMapper(UserDatabasePermissionMapper.class);

    @Mapping(source = "action", target = "type", qualifiedByName = "actionToType")
    UserDatabasePermission entityToModel(UserDatabasePermissionEntity entity);

    @Named("actionToType")
    static DatabasePermissionType actionToType(String action) {
        if ("query".equalsIgnoreCase(action)) {
            return DatabasePermissionType.QUERY;
        }
        if ("change".equalsIgnoreCase(action)) {
            return DatabasePermissionType.CHANGE;
        }
        if ("export".equalsIgnoreCase(action)) {
            return DatabasePermissionType.EXPORT;
        }
        return null;
    }

}
