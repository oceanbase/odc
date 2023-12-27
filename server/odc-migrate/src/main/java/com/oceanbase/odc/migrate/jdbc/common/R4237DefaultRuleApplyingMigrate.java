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

package com.oceanbase.odc.migrate.jdbc.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/12/5 11:06
 * @Description: []
 */
@Slf4j
@Migratable(version = "4.2.3.7", description = "init default regulation rule applying", repeatable = true,
        ignoreChecksum = true)
public class R4237DefaultRuleApplyingMigrate implements JdbcMigratable {
    private static final String MIGRATE_CONFIG_FILE = "init-config/init/regulation-rule-applying.yaml";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void migrate(DataSource dataSource) {
        DefaultRuleApplyingRepository defaultRuleApplyingRepository =
                SpringContextUtil.getBean(DefaultRuleApplyingRepository.class);
        RuleMetadataRepository ruleMetadataRepository = SpringContextUtil.getBean(RuleMetadataRepository.class);

        Map<String, MetadataEntity> metadataName2Metadata =
                ruleMetadataRepository.findAll().stream().collect(Collectors.toMap(MetadataEntity::getName, e -> e));
        List<InnerDefaultRuleApplying> expected =
                YamlUtils.fromYaml(MIGRATE_CONFIG_FILE, new TypeReference<List<InnerDefaultRuleApplying>>() {});
        Verify.notEmpty(expected, "expectedDefaultRuleApplyings");

        Map<String, List<DefaultRuleApplyingEntity>> actualRulesetName2RuleApplyings = defaultRuleApplyingRepository
                .findAll().stream().collect(Collectors.groupingBy(DefaultRuleApplyingEntity::getRulesetName));
        List<DefaultRuleApplyingEntity> toAdd = new ArrayList<>();
        for (InnerDefaultRuleApplying expectedApplying : expected) {
            String rulesetName = expectedApplying.getRulesetName();
            MetadataEntity metadataEntity = metadataName2Metadata.get(expectedApplying.getRuleName());
            if (Objects.isNull(metadataEntity)) {
                throw new UnexpectedException("rule metadata not found, ruleName: " + expectedApplying.getRuleName());
            }

            List<DefaultRuleApplyingEntity> actualRuleApplyings =
                    actualRulesetName2RuleApplyings.get(rulesetName);
            if (CollectionUtils.isEmpty(actualRuleApplyings)) {
                toAdd.add(generateNewEntity(expectedApplying, rulesetName, metadataEntity.getId()));
            } else {
                Optional<DefaultRuleApplyingEntity> existed = actualRuleApplyings.stream()
                        .filter(r -> Objects.equals(metadataEntity.getId(), r.getRuleMetadataId())).findFirst();
                if (existed.isPresent()) {
                    DefaultRuleApplyingEntity actualRuleApplying = existed.get();
                    if (!isApplyingEquals(expectedApplying, actualRuleApplying)) {
                        actualRuleApplying.setEnabled(expectedApplying.getEnabled());
                        actualRuleApplying.setLevel(expectedApplying.getLevel());
                        actualRuleApplying.setAppliedDialectTypes(expectedApplying.getAppliedDialectTypes());
                        actualRuleApplying.setPropertiesJson(expectedApplying.getPropertiesJson());
                        toAdd.add(actualRuleApplying);
                    }
                } else {
                    toAdd.add(generateNewEntity(expectedApplying, rulesetName, metadataEntity.getId()));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(toAdd)) {
            log.info("default rule applying changed, start saving, size={}", toAdd.size());
            defaultRuleApplyingRepository.saveAll(toAdd);
            log.info("saving changed default rule applying succeed, size={}", toAdd.size());
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InnerDefaultRuleApplying {
        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("level")
        private Integer level;

        @JsonProperty("rulesetName")
        private String rulesetName;

        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("appliedDialectTypes")
        private List<String> appliedDialectTypes;

        @JsonProperty("propertiesJson")
        private String propertiesJson;
    }

    private boolean isApplyingEquals(@NonNull InnerDefaultRuleApplying innerDefaultRuleApplying,
            @NonNull DefaultRuleApplyingEntity defaultRuleApplyingEntity) {
        return Objects.equals(innerDefaultRuleApplying.getEnabled(), defaultRuleApplyingEntity.getEnabled())
                && Objects.equals(innerDefaultRuleApplying.getLevel(), defaultRuleApplyingEntity.getLevel())
                && Objects.equals(innerDefaultRuleApplying.getAppliedDialectTypes(),
                        defaultRuleApplyingEntity.getAppliedDialectTypes())
                && Objects.equals(innerDefaultRuleApplying.getPropertiesJson(),
                        defaultRuleApplyingEntity.getPropertiesJson());
    }

    private DefaultRuleApplyingEntity generateNewEntity(InnerDefaultRuleApplying innerDefaultRuleApplying,
            String rulesetName, Long metadataId) {
        DefaultRuleApplyingEntity defaultRuleApplyingEntity = new DefaultRuleApplyingEntity();
        defaultRuleApplyingEntity.setEnabled(innerDefaultRuleApplying.getEnabled());
        defaultRuleApplyingEntity.setLevel(innerDefaultRuleApplying.getLevel());
        defaultRuleApplyingEntity.setRulesetName(rulesetName);
        defaultRuleApplyingEntity.setRuleMetadataId(metadataId);
        defaultRuleApplyingEntity.setAppliedDialectTypes(innerDefaultRuleApplying.getAppliedDialectTypes());
        defaultRuleApplyingEntity.setPropertiesJson(innerDefaultRuleApplying.getPropertiesJson());
        return defaultRuleApplyingEntity;
    }

}
