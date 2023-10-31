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

package com.oceanbase.odc.service.dlm.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.dlm.TaskUnitEntity;
import com.oceanbase.tools.migrator.core.meta.TaskMeta;

/**
 * @Author：tinker
 * @Date: 2023/10/31 17:04
 * @Descripition:
 */
@Mapper
public interface TaskUnitMapper {

    TaskUnitMapper INSTANCE = Mappers.getMapper(TaskUnitMapper.class);

    @Mapping(target = "lowerBoundPrimaryKey",
            expression = "java(model.getMinPrimaryKey().toSqlString())")
    @Mapping(target = "upperBoundPrimaryKey",
            expression = "java(model.getMaxPrimaryKey().toSqlString())")
    @Mapping(target = "primaryKeyCursor",
            expression = "java(model.getCursorPrimaryKey().toSqlString())")
    @Mapping(source = "taskStatus", target = "status")
    @Mapping(source = "jobMeta.jobId", target = "jobId")
    TaskUnitEntity modelToEntity(TaskMeta model);


    @Mapping(target = "minPrimaryKey",
            expression = "java(com.oceanbase.tools.migrator.common.element.PrimaryKey.valuesOf(entity.getLowerBoundPrimaryKey()))")
    @Mapping(target = "maxPrimaryKey",
            expression = "java(com.oceanbase.tools.migrator.common.element.PrimaryKey.valuesOf(entity.getUpperBoundPrimaryKey()))")
    @Mapping(target = "cursorPrimaryKey",
            expression = "java(com.oceanbase.tools.migrator.common.element.PrimaryKey.valuesOf(entity.getPrimaryKeyCursor()))")
    @Mapping(source = "status", target = "taskStatus")
    TaskMeta entityToModel(TaskUnitEntity entity);
}
