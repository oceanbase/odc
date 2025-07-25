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

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface LlmProviderRepository extends OdcJpaRepository<LlmProviderEntity, Long> {

    List<LlmProviderEntity> findAllByOrganizationId(Long organizationId);

    Optional<LlmProviderEntity> findByOrganizationIdAndName(Long organizationId, String name);

    @Modifying
    @Query("DELETE FROM LlmProviderEntity e WHERE e.organizationId = :organizationId AND e.name = :name")
    int deleteByOrganizationIdAndName(@Param("organizationId") Long organizationId, @Param("name") String name);

    @Modifying
    @Transactional
    @Query("UPDATE LlmProviderEntity e SET e.propertiesJson = :propertiesJson "
            + "WHERE e.organizationId = :organizationId AND e.name = :name")
    int updatePropertiesJsonByOrganizationIdAndName(@Param("organizationId") Long organizationId,
            @Param("name") String name, @Param("propertiesJson") String propertiesJson);

    @Modifying
    @Transactional
    @Query("UPDATE LlmProviderEntity e SET e.description = :description "
            + "WHERE e.organizationId = :organizationId AND e.name = :name")
    int updateDescriptionByOrganizationIdAndName(@Param("organizationId") Long organizationId,
            @Param("name") String name, @Param("description") String description);

}
