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
package com.oceanbase.odc.service.datasecurity.util;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.datasecurity.MaskingAlgorithmEntity;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;

/**
 * @author gaoda.xy
 * @date 2023/5/17 10:56
 */
@Mapper
public interface MaskingAlgorithmMapper {

    MaskingAlgorithmMapper INSTANCE = Mappers.getMapper(MaskingAlgorithmMapper.class);

    MaskingAlgorithm entityToModel(MaskingAlgorithmEntity entity);

    MaskingAlgorithmEntity modelToEntity(MaskingAlgorithm model);

}
