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
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RulesetEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RulesetRepository;
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
        migrateBuiltInRules();
        migrateUserDefinedRules();
    }

    private void migrateBuiltInRules() {
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

        List<DefaultRuleApplyingEntity> changeList = new ArrayList<>();
        for (InnerDefaultRuleApplying expectedApplying : expected) {
            String rulesetName = expectedApplying.getRulesetName();
            MetadataEntity metadataEntity = metadataName2Metadata.get(expectedApplying.getRuleName());
            if (Objects.isNull(metadataEntity)) {
                throw new UnexpectedException("rule metadata not found, ruleName: " + expectedApplying.getRuleName());
            }
            List<DefaultRuleApplyingEntity> actualRuleApplyings =
                    actualRulesetName2RuleApplyings.get(rulesetName);
            if (CollectionUtils.isEmpty(actualRuleApplyings)) {
                changeList.add(generateNewEntity(expectedApplying, rulesetName, metadataEntity.getId()));
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
                        changeList.add(actualRuleApplying);
                    }
                } else {
                    changeList.add(generateNewEntity(expectedApplying, rulesetName, metadataEntity.getId()));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(changeList)) {
            log.info("Start saving changed applyings, size={}", changeList.size());
            defaultRuleApplyingRepository.saveAll(changeList);
            log.info("Saving changed default rule applyings succeed, size={}", changeList.size());
        }
    }

    /**
     * For migrating the customized environment's SQL rules:<br>
     * 1. All the customized rules will be disabled by default.<br>
     * 2. All the customized rules' default value will follow the value of the built-in default
     * environment.<br>
     */
    private void migrateUserDefinedRules() {
        DefaultRuleApplyingRepository defaultRuleApplyingRepository =
                SpringContextUtil.getBean(DefaultRuleApplyingRepository.class);
        List<DefaultRuleApplyingEntity> expectedRuleApplyings =
                defaultRuleApplyingRepository
                        .findAll().stream().filter(e -> e.getRulesetName().endsWith("default-default-ruleset.name}"))
                        .collect(Collectors.toList());

        RulesetRepository rulesetRepository = SpringContextUtil.getBean(RulesetRepository.class);
        List<RulesetEntity> userDefinedRulesets = rulesetRepository.findByBuiltin(false);
        if (CollectionUtils.isEmpty(userDefinedRulesets)) {
            log.info("There does not exists user defined rulesets, skip migrating user defined rulesets");
            return;
        }
        RuleApplyingRepository ruleApplyingRepository = SpringContextUtil.getBean(RuleApplyingRepository.class);
        Set<Long> userDefinedRulesetIds =
                userDefinedRulesets.stream().map(RulesetEntity::getId).collect(Collectors.toSet());

        Map<Long, List<RuleApplyingEntity>> rulesetId2RuleApplyings = ruleApplyingRepository.findAll().stream()
                .collect(Collectors.groupingBy(RuleApplyingEntity::getRulesetId));
        for (Entry<Long, List<RuleApplyingEntity>> entry : rulesetId2RuleApplyings.entrySet()) {
            if (!userDefinedRulesetIds.contains(entry.getKey())) {
                throw new UnexpectedException(
                        "rulesetId2RuleApplyings contains rulesetId not in userDefinedRulesets, rulesetId="
                                + entry.getKey());
            }
            Verify.notEmpty(entry.getValue(), "rulesetId2RuleApplyings");

            Map<Long, RuleApplyingEntity> ruleApplyings = entry.getValue().stream().collect(
                    Collectors.toMap(RuleApplyingEntity::getRuleMetadataId, e -> e));
            List<RuleApplyingEntity> toAdd = new ArrayList<>();
            expectedRuleApplyings.stream().forEach(e -> {
                if (!ruleApplyings.containsKey(e.getRuleMetadataId())) {
                    RuleApplyingEntity ruleApplyingEntity = new RuleApplyingEntity();
                    ruleApplyingEntity.setAppliedDialectTypes(e.getAppliedDialectTypes());
                    ruleApplyingEntity.setEnabled(false);
                    ruleApplyingEntity.setLevel(e.getLevel());
                    ruleApplyingEntity.setPropertiesJson(e.getPropertiesJson());
                    ruleApplyingEntity.setRuleMetadataId(e.getRuleMetadataId());
                    ruleApplyingEntity.setRulesetId(entry.getKey());
                    ruleApplyingEntity.setOrganizationId(entry.getValue().get(0).getOrganizationId());
                    toAdd.add(ruleApplyingEntity);
                }
            });
            log.info("Start saving new added user defined rule applyings, rulesetId={}, size={}", entry.getKey(),
                    toAdd.size());
            ruleApplyingRepository.saveAll(toAdd);
            log.info("Saving new added user defined rule applyings success, rulesetId={}, size={}", entry.getKey(),
                    toAdd.size());
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
