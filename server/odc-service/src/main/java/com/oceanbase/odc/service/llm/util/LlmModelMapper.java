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
package com.oceanbase.odc.service.llm.util;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.oceanbase.odc.metadb.llm.LlmModelEntity;
import com.oceanbase.odc.service.llm.model.LlmModel;

/**
 * LLM mapper for entity-model conversion
 */
@Mapper
public interface LlmModelMapper {
    LlmModelMapper INSTANCE = Mappers.getMapper(LlmModelMapper.class);

    /**
     * Convert LlmEntity to Llm
     * 
     * @param entity LlmEntity
     * @return Llm
     */
    LlmModel entityToModel(LlmModelEntity entity);

    /**
     * Convert Llm to LlmEntity
     * 
     * @param model Llm
     * @return LlmEntity
     */
    LlmModelEntity modelToEntity(LlmModel model);
}
