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
package com.oceanbase.odc.metadb.regulation.ruleset;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface RuleApplyingRepository extends OdcJpaRepository<RuleApplyingEntity, Long>,
        JpaRepository<RuleApplyingEntity, Long>, JpaSpecificationExecutor<RuleApplyingEntity> {
    List<RuleApplyingEntity> findByRulesetId(Long rulesetId);

    List<RuleApplyingEntity> findByOrganizationIdAndRulesetId(Long organizationId, Long rulesetId);


    Optional<RuleApplyingEntity> findByOrganizationIdAndId(Long organizationId, Long id);

    Optional<RuleApplyingEntity> findByOrganizationIdAndRulesetIdAndRuleMetadataId(Long organizationId, Long rulesetId,
            Long ruleMetadataId);

    @Transactional
    @Query(value = "select ra.* from regulation_rule_applying as ra where ra.organization_id = :organizationId and "
            + "ra.rule_metadata_id in (select rm.id from regulation_rule_metadata rm where rm.name = :name)",
            nativeQuery = true)
    List<RuleApplyingEntity> findByOrganizationIdAndRuleMetadataName(@Param("organizationId") Long organizationId,
            @Param("name") String name);

    @Transactional
    int deleteByOrganizationIdAndRulesetId(Long organizationId, Long rulesetId);


    default List<RuleApplyingEntity> batchCreate(List<RuleApplyingEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("regulation_rule_applying")
                .field(RuleApplyingEntity_.ORGANIZATION_ID)
                .field(RuleApplyingEntity_.RULESET_ID)
                .field(RuleApplyingEntity_.RULE_METADATA_ID)
                .field(RuleApplyingEntity_.APPLIED_DIALECT_TYPES)
                .field(RuleApplyingEntity_.ENABLED)
                .field(RuleApplyingEntity_.LEVEL)
                .field(RuleApplyingEntity_.PROPERTIES_JSON)
                .build();
        List<Function<RuleApplyingEntity, Object>> getter = valueGetterBuilder().add(
                RuleApplyingEntity::getOrganizationId)
                .add(RuleApplyingEntity::getRulesetId)
                .add(RuleApplyingEntity::getRuleMetadataId)
                .add(RuleApplyingEntity::getAppliedDialectTypes)
                .add(RuleApplyingEntity::getEnabled)
                .add(RuleApplyingEntity::getLevel)
                .add(RuleApplyingEntity::getPropertiesJson)
                .build();
        return batchCreate(entities, sql, getter, RuleApplyingEntity::setId);
    }
}

