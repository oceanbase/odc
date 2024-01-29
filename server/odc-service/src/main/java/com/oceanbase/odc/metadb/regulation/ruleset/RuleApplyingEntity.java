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

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.jpa.JsonListConverter;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/5/18 15:28
 * @Description: []
 */

@Data
@Entity
@Table(name = "regulation_rule_applying")
public class RuleApplyingEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "ruleset_id", nullable = false)
    private Long rulesetId;

    @Column(name = "rule_metadata_id", nullable = false)
    private Long ruleMetadataId;

    @Column(name = "applied_dialect_types")
    @Convert(converter = JsonListConverter.class)
    private List<String> appliedDialectTypes;

    @Column(name = "properties_json", nullable = false)
    private String propertiesJson;

    public static RuleApplyingEntity merge(@NotNull Optional<DefaultRuleApplyingEntity> defaultRuleApplyingEntity,
            @NotNull Optional<RuleApplyingEntity> ruleApplyingEntityOpt) {
        if (!defaultRuleApplyingEntity.isPresent() && !ruleApplyingEntityOpt.isPresent()) {
            return null;
        }
        if (ruleApplyingEntityOpt.isPresent()) {
            return ruleApplyingEntityOpt.get();
        } else {
            RuleApplyingEntity entity = ruleApplyingEntityOpt.get();
            entity.setId(defaultRuleApplyingEntity.get().getId());
            entity.setEnabled(defaultRuleApplyingEntity.get().getEnabled());
            entity.setRuleMetadataId(defaultRuleApplyingEntity.get().getRuleMetadataId());
            entity.setAppliedDialectTypes(defaultRuleApplyingEntity.get().getAppliedDialectTypes());
            entity.setPropertiesJson(defaultRuleApplyingEntity.get().getPropertiesJson());
            entity.setLevel(defaultRuleApplyingEntity.get().getLevel());
            entity.setCreateTime(defaultRuleApplyingEntity.get().getCreateTime());
            entity.setUpdateTime(defaultRuleApplyingEntity.get().getUpdateTime());
            return entity;

        }
    }
}
