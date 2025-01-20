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

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.dlm.TaskGeneratorEntity;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;

/**
 * @Author：tinker
 * @Date: 2023/10/31 15:58
 * @Descripition:
 */
@Mapper
public interface TaskGeneratorMapper {

    TaskGeneratorMapper INSTANCE = Mappers.getMapper(TaskGeneratorMapper.class);

    @Mapping(source = "generatorId", target = "id")
    @Mapping(source = "status", target = "generatorStatus")
    @Mapping(source = "partitionSavePoint", target = "partitionSavePoint")
    @Mapping(target = "primaryKeySavePoint",
            expression = "java(com.oceanbase.tools.migrator.common.element.PrimaryKey.valuesOf(entity.getPrimaryKeySavePoint()))")
    TaskGenerator entityToModel(TaskGeneratorEntity entity);

    @InheritInverseConfiguration
    @Mapping(target = "type", constant = "AUTO")
    @Mapping(target = "primaryKeySavePoint",
            expression = "java(model.getPrimaryKeySavePoint() != null ?model.getPrimaryKeySavePoint().toSqlString():null)")
    @Mapping(target = "id", ignore = true)
    TaskGeneratorEntity modelToEntity(TaskGenerator model);

}
