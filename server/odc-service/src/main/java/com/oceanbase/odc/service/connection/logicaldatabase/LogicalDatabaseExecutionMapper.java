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
package com.oceanbase.odc.service.connection.logicaldatabase;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBChangeExecutionUnitEntity;
import com.oceanbase.odc.service.connection.logicaldatabase.core.executor.sql.SqlExecutionResultWrapper;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalDBChangeExecutionUnit;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 20:31
 * @Description: []
 */
@Mapper
public interface LogicalDatabaseExecutionMapper {
    LogicalDatabaseExecutionMapper INSTANCE = Mappers.getMapper(LogicalDatabaseExecutionMapper.class);

    @Mapping(target = "result", source = "executionResultJson", qualifiedByName = "fromJson")
    @Mapping(target = "order", source = "executionOrder")
    LogicalDBChangeExecutionUnit entityToModel(LogicalDBChangeExecutionUnitEntity entity);


    @Mapping(target = "executionResultJson", source = "result", qualifiedByName = "toJson")
    @Mapping(target = "executionOrder", source = "order")
    LogicalDBChangeExecutionUnitEntity modelToEntity(LogicalDBChangeExecutionUnit model);

    @Named("fromJson")
    static SqlExecutionResultWrapper fromJson(String json) {
        return JsonUtils.fromJson(json, SqlExecutionResultWrapper.class);
    }

    @Named("toJson")
    static String toJson(SqlExecutionResultWrapper result) {
        return JsonUtils.toJson(result);
    }
}
