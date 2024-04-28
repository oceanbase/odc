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
package com.oceanbase.odc.service.connection.table;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.connection.TableEntity;
import com.oceanbase.odc.service.connection.table.model.Table;

/**
 * ClassName: TableMapper Package: com.oceanbase.odc.service.connection.table Description:
 *
 * @Author: fenghao
 * @Create 2024/3/12 21:23
 * @Version 1.0
 */
@Mapper
public interface TableMapper {
    TableMapper INSTANCE = Mappers.getMapper(TableMapper.class);

    Table entityToModel(TableEntity entity);

    TableEntity modelToEntity(Table model);
}
