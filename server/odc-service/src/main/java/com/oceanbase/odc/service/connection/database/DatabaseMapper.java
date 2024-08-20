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
package com.oceanbase.odc.service.connection.database;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.service.connection.database.model.Database;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 14:36
 * @Description: []
 */
@Mapper
public interface DatabaseMapper {
    DatabaseMapper INSTANCE = Mappers.getMapper(DatabaseMapper.class);

    @Mapping(source = "connectionId", target = "dataSource.id")
    @Mapping(source = "projectId", target = "project.id")
    Database entityToModel(DatabaseEntity entity);

    @InheritInverseConfiguration
    DatabaseEntity modelToEntity(Database model);
}
