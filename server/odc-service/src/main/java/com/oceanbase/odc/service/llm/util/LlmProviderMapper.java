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

import com.oceanbase.odc.metadb.llm.LlmProviderEntity;
import com.oceanbase.odc.service.llm.model.LlmProvider;

/**
 * LLM provider mapper for entity-model conversion
 */
@Mapper
public interface LlmProviderMapper {
    LlmProviderMapper INSTANCE = Mappers.getMapper(LlmProviderMapper.class);

    /**
     * Convert LlmProviderEntity to LlmProvider
     * 
     * @param entity LlmProviderEntity
     * @return LlmProvider
     */
    LlmProvider entityToModel(LlmProviderEntity entity);

    /**
     * Convert LlmProvider to LlmProviderEntity
     * 
     * @param model LlmProvider
     * @return LlmProviderEntity
     */
    LlmProviderEntity modelToEntity(LlmProvider model);
}
