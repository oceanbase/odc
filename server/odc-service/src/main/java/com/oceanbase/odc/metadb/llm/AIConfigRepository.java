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
package com.oceanbase.odc.metadb.llm;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

@Repository
public interface AIConfigRepository extends OdcJpaRepository<AIConfigEntity, Long> {

    Optional<AIConfigEntity> findByOrganizationId(Long organizationId);

    @Modifying
    @Query("UPDATE AIConfigEntity SET " +
            "aiEnabled = :#{#aiConfig.aiEnabled}, " +
            "chatEnabled = :#{#aiConfig.chatEnabled}, " +
            "copilotEnabled = :#{#aiConfig.copilotEnabled}, " +
            "completionEnabled = :#{#aiConfig.completionEnabled}, " +
            "defaultEmbeddingModel = :#{#aiConfig.defaultEmbeddingModel}, " +
            "defaultLlmModel = :#{#aiConfig.defaultLlmModel}, " +
            "defaultChatModel = :#{#aiConfig.defaultChatModel} " +
            "WHERE organizationId = :organizationId")
    @Transactional
    int updateByOrganizationId(Long organizationId, AIConfigEntity aiConfig);

}
