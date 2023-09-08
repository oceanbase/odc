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
package com.oceanbase.odc.service.collaboration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.util.YamlUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.metadb.regulation.ruleset.MetadataEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleMetadataRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.regulation.ruleset.RuleMetadataService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/30 17:18
 * @Description: []
 */
@Service
@Slf4j
@SkipAuthorize
public class RuleApplyingMigrator {
    private static final String RULE_APPLYING_RESOURCE = "init-config/init/regulation-rule-applying.yaml";

    @Autowired
    private RuleMetadataService metadataService;

    @Autowired
    private RuleMetadataRepository metadataRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private RuleApplyingRepository ruleApplyingRepository;

    @Transactional(rollbackFor = Exception.class)
    public int migrate(Long organizationId) {
        List<InnerRuleApplying> rules =
                YamlUtils.fromYaml(RULE_APPLYING_RESOURCE, new TypeReference<List<InnerRuleApplying>>() {});
        log.info("start to sync regulation rules, organizationId={}", organizationId);
        int affectRules = 0;

        Map<String, List<RuleMetadata>> name2Metadatas =
                metadataService.list(QueryRuleMetadataParams.builder().build()).stream().collect(
                        Collectors.groupingBy(RuleMetadata::getName));
        Map<String, List<InnerRuleApplying>> envName2ExpectedRuleApplyings =
                rules.stream().collect(Collectors.groupingBy(InnerRuleApplying::getEnvironmentName));

        List<Environment> environments = environmentService.list(organizationId);
        Map<String, List<Long>> envName2RulesetIds = environments.stream().collect(Collectors
                .groupingBy(Environment::getName, Collectors.mapping(Environment::getRulesetId, Collectors.toList())));

        List<RuleApplyingEntity> toAdd = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : envName2RulesetIds.entrySet()) {
            Verify.equals(1, entry.getValue().size(), "environment.rulesetIds");
            String environmentName = entry.getKey();
            Long rulesetId = entry.getValue().get(0);
            List<RuleApplyingEntity> actual = ruleApplyingRepository.findByRulesetId(rulesetId);
            Set<Long> actualMetadataIds =
                    actual.stream().map(RuleApplyingEntity::getRuleMetadataId).collect(Collectors.toSet());
            Set<String> actualMetadataNames = metadataRepository.findAllById(actualMetadataIds).stream()
                    .map(MetadataEntity::getName).collect(Collectors.toSet());

            List<InnerRuleApplying> expected = envName2ExpectedRuleApplyings.get(environmentName);

            for (InnerRuleApplying innerRuleApplying : expected) {
                if (actualMetadataNames.contains(innerRuleApplying.getRuleName())) {
                    continue;
                }
                RuleApplyingEntity entity = new RuleApplyingEntity();
                /***
                 * set metadata id
                 */
                String ruleName = innerRuleApplying.getRuleName();
                if (!name2Metadatas.containsKey(ruleName)) {
                    log.warn("rule metadata not found, ruleName={}", ruleName);
                    continue;
                }
                Verify.equals(1, name2Metadatas.get(ruleName).size(), "type2Metadatas.size");
                entity.setRuleMetadataId(name2Metadatas.get(ruleName).get(0).getId());

                entity.setRulesetId(rulesetId);
                entity.setOrganizationId(organizationId);
                entity.setAppliedDialectTypes(innerRuleApplying.getAppliedDialectTypes());
                entity.setEnabled(innerRuleApplying.getEnabled());
                entity.setLevel(innerRuleApplying.getLevel());
                entity.setPropertiesJson(innerRuleApplying.getPropertiesJson());

                toAdd.add(entity);
            }
        }
        if (CollectionUtils.isNotEmpty(toAdd)) {

            affectRules = ruleApplyingRepository.saveAll(toAdd).size();
        }
        log.info("sync regulation rules successfully, organizationId={}, affectRules={}", organizationId, affectRules);
        return affectRules;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class InnerRuleApplying {
        @JsonProperty("enabled")
        private Boolean enabled;

        @JsonProperty("level")
        private Integer level;

        @JsonProperty("environmentName")
        private String environmentName;

        @JsonProperty("ruleName")
        private String ruleName;

        @JsonProperty("appliedDialectTypes")
        private List<String> appliedDialectTypes;

        @JsonProperty("propertiesJson")
        private String propertiesJson;
    }
}
