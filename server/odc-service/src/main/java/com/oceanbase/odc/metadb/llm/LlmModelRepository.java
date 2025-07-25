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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface LlmModelRepository extends OdcJpaRepository<LlmModelEntity, Long> {

    /**
     * Query the count of models for each provider in the specified organization
     * 
     * @return List of Object arrays, where each array contains [providerName, modelCount]
     */
    @Query("SELECT e.providerName, COUNT(e) " +
            "FROM LlmModelEntity e " +
            "WHERE e.organizationId = :organizationId " +
            "GROUP BY e.providerName")
    List<Object[]> countModelsByProviderNameInOrganization(@Param("organizationId") Long organizationId);

    List<LlmModelEntity> findByOrganizationId(Long organizationId);

    List<LlmModelEntity> findByOrganizationIdAndProviderName(Long organizationId, String providerName);

    Optional<LlmModelEntity> findByOrganizationIdAndProviderNameAndModelName(Long organizationId, String providerName,
            String modelName);

    @Modifying
    @Query("DELETE FROM LlmModelEntity e WHERE e.organizationId = :organizationId " +
            "AND e.providerName = :providerName " +
            "AND e.custom = false")
    @Transactional
    int deleteAllBuiltinModelsByOrganizationIdAndProviderName(@Param("organizationId") Long organizationId,
            @Param("providerName") String providerName);

    @Modifying
    @Query("DELETE FROM LlmModelEntity e WHERE e.organizationId = :organizationId " +
            "AND e.providerName = :providerName " +
            "AND e.modelName = :modelName")
    @Transactional
    int deleteByOrganizationIdAndProviderNameAndModelName(@Param("organizationId") Long organizationId,
            @Param("providerName") String providerName, @Param("modelName") String modelName);

    @Modifying
    @Query("UPDATE LlmModelEntity e SET e.enabled = :enabled WHERE e.organizationId = :organizationId " +
            "AND e.providerName = :providerName " +
            "AND e.modelName = :modelName")
    @Transactional
    int updateEnabledByOrganizationIdAndProviderNameAndModelName(@Param("organizationId") Long organizationId,
            @Param("providerName") String providerName, @Param("modelName") String modelName,
            @Param("enabled") boolean enabled);

    @Modifying
    @Query("UPDATE LlmModelEntity e SET e.propertiesJson = :propertiesJson, e.description = :description "
            + "WHERE e.organizationId = :organizationId "
            + "AND e.providerName = :providerName "
            + "AND e.modelName = :modelName")
    @Transactional
    int updatePropertiesJsonAndDescriptionByOrganizationAndProviderAndModel(
            @Param("organizationId") Long organizationId,
            @Param("providerName") String providerName, @Param("modelName") String modelName,
            @Param("propertiesJson") String propertiesJson, @Param("description") String description);

    @Modifying
    @Transactional
    @Query("UPDATE LlmModelEntity e SET e.deprecated = :deprecated "
            + "WHERE e.providerName = :provider "
            + "AND e.modelName IN :models")
    int updateAllDeprecatedByProviderNameAndModelNames(
            @Param("provider") String provider, @Param("models") Collection<String> models,
            @Param("deprecated") boolean deprecated);

    default List<LlmModelEntity> batchCreate(List<LlmModelEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("integration_llm_model")
                .field(LlmModelEntity_.creatorId)
                .field(LlmModelEntity_.organizationId)
                .field(LlmModelEntity_.lastModifierId)
                .field(LlmModelEntity_.providerName)
                .field(LlmModelEntity_.modelName)
                .field(LlmModelEntity_.displayName)
                .field(LlmModelEntity_.modelType)
                .field("is_enabled")
                .field("is_deprecated")
                .field("is_custom")
                .field("is_function_calling_support")
                .field(LlmModelEntity_.salt)
                .field(LlmModelEntity_.maxToken)
                .field(LlmModelEntity_.usedToken)
                .field(LlmModelEntity_.contextSize)
                .field(LlmModelEntity_.propertiesJson)
                .build();
        List<Function<LlmModelEntity, Object>> getter = valueGetterBuilder()
                .add(LlmModelEntity::getCreatorId)
                .add(LlmModelEntity::getOrganizationId)
                .add(LlmModelEntity::getLastModifierId)
                .add(LlmModelEntity::getProviderName)
                .add(LlmModelEntity::getModelName)
                .add(LlmModelEntity::getDisplayName)
                .add(e -> e.getModelType().name())
                .add(LlmModelEntity::getEnabled)
                .add(LlmModelEntity::getDeprecated)
                .add(LlmModelEntity::getCustom)
                .add(LlmModelEntity::getFunctionCallingSupport)
                .add(LlmModelEntity::getSalt)
                .add(LlmModelEntity::getMaxToken)
                .add(LlmModelEntity::getUsedToken)
                .add(LlmModelEntity::getContextSize)
                .add(LlmModelEntity::getPropertiesJson)
                .build();

        return batchCreate(entities, sql, getter, LlmModelEntity::setId);
    }

}
