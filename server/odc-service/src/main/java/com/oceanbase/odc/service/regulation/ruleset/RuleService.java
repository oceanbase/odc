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
package com.oceanbase.odc.service.regulation.ruleset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/29 16:26
 * @Description: []
 */
@Slf4j
@Service
@Validated
@Authenticated
public class RuleService {

    @Autowired
    private RuleMetadataService metadataService;

    @Autowired
    private RuleApplyingRepository ruleApplyingRepository;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private LoadingCache<Long, List<Rule>> rulesetId2RulesCache;

    @SkipAuthorize("odc internal usage")
    public List<Rule> list(@NonNull Long rulesetId, @NonNull QueryRuleMetadataParams params) {
        return internalList(rulesetId, params);
    }

    @SkipAuthorize("odc internal usage")
    public List<Rule> listAllFromDB(@NonNull Long rulesetId) {
        return internalList(rulesetId, QueryRuleMetadataParams.builder().build());
    }

    @SkipAuthorize("odc internal usage")
    public List<Rule> listAllFromCache(@NonNull Long rulesetId) {
        return rulesetId2RulesCache.get(rulesetId);
    }

    @SkipAuthorize("odc internal usage")
    public Stats statsRules(@NonNull Long rulesetId, @NonNull QueryRuleMetadataParams params) {
        List<Rule> rules = list(rulesetId, params);
        Set<String> subTypes =
                rules.stream().filter(rule -> CollectionUtils.isNotEmpty(rule.getMetadata().getSubTypes()))
                        .flatMap(rule -> rule.getMetadata().getSubTypes().stream()).collect(Collectors.toSet());
        Set<String> supportedDialectTypes = rules.stream().filter(rule -> CollectionUtils.isNotEmpty(rule.getMetadata()
                .getSupportedDialectTypes()))
                .flatMap(rule -> rule.getMetadata().getSupportedDialectTypes().stream().map(DialectType::name))
                .collect(Collectors.toSet());
        Stats stats = new Stats()
                .andDistinct("subTypes", subTypes)
                .andDistinct("supportedDialectTypes", supportedDialectTypes);
        return stats;
    }

    @SkipAuthorize("internal authenticated")
    public Optional<Rule> getByRulesetIdAndName(@NonNull Long rulesetId, @NotEmpty String name) {
        List<Rule> filtered = listAllFromCache(rulesetId).stream()
                .filter(rule -> StringUtils.equals(rule.getMetadata().getName(), name)).collect(
                        Collectors.toList());
        Verify.singleton(filtered, "filteredRules");
        return Optional.of(filtered.get(0));

    }

    @SkipAuthorize("internal authenticated")
    public List<Rule> getByOrganizationIdAndRuleMetaDataName(@NonNull Long organizationId, @NotBlank String name) {
        List<RuleApplyingEntity> entities =
                ruleApplyingRepository.findByOrganizationIdAndRuleMetadataName(organizationId, name);
        return entities.stream().map(e -> {
            Rule rule = entityToModel(e);
            rule.setRulesetId(e.getRulesetId());
            return rule;
        }).collect(Collectors.toList());
    }

    @SkipAuthorize("internal authenticated")
    public Rule detail(@NonNull Long rulesetId, @NonNull Long ruleId) {
        RuleApplyingEntity applyingEntity = ruleApplyingRepository.findById(ruleId)
                .orElseThrow(() -> new UnexpectedException("rule not found, ruleId = " + ruleId));
        Rule rule = entityToModel(applyingEntity);
        permissionValidator.checkCurrentOrganization(rule);
        rule.setRulesetId(rulesetId);
        rule.setMetadata(metadataService.detail(applyingEntity.getRuleMetadataId()));
        return rule;
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_RULESET", indexOfIdParam = 0)
    public Rule update(@NonNull Long rulesetId, @NonNull Long ruleId, @NonNull Rule rule) {
        RuleApplyingEntity saved = ruleApplyingRepository.findByOrganizationIdAndId(
                authenticationFacade.currentOrganizationId(), ruleId)
                .orElseThrow(() -> new UnexpectedException("rule not found, ruleId = " + ruleId));
        saved.setLevel(rule.getLevel());
        saved.setEnabled(rule.getEnabled());
        saved.setPropertiesJson(JsonUtils.toJson(rule.getProperties()));
        if (Objects.nonNull(rule.getAppliedDialectTypes())) {
            saved.setAppliedDialectTypes(
                    rule.getAppliedDialectTypes().stream().map(DialectType::name).collect(Collectors.toList()));
        }
        ruleApplyingRepository.save(saved);
        rulesetId2RulesCache.invalidate(rulesetId);
        return rule;
    }

    private List<Rule> internalList(@NonNull Long rulesetId, @NonNull QueryRuleMetadataParams params) {
        List<RuleMetadata> ruleMetadatas = metadataService.list(params);
        if (CollectionUtils.isEmpty(ruleMetadatas)) {
            return Collections.EMPTY_LIST;
        }
        List<Rule> rules = new ArrayList<>();
        Map<Long, List<RuleApplyingEntity>> metadataId2RuleApplying =
                ruleApplyingRepository.findByRulesetId(rulesetId).stream()
                        .collect(Collectors.groupingBy(RuleApplyingEntity::getRuleMetadataId));
        ruleMetadatas.stream().forEach(metadata -> {
            if (!metadataId2RuleApplying.containsKey(metadata.getId())) {
                log.warn("rule applying record not found, metadataId={}", metadata.getId());
                throw new UnexpectedException("rule applying record not found, metadataId=" + metadata.getId());
            }
            List<RuleApplyingEntity> entities = metadataId2RuleApplying.get(metadata.getId());
            Verify.equals(1, entities.size(), "ruleApplyingEntities");
            Rule rule = entityToModel(entities.get(0));
            permissionValidator.checkCurrentOrganization(rule);
            rule.setRulesetId(rulesetId);
            rule.setMetadata(metadata);
            rules.add(rule);
        });
        return rules;
    }

    private Rule entityToModel(RuleApplyingEntity ruleApplyingEntity) {
        Rule rule = new Rule();
        rule.setId(ruleApplyingEntity.getId());
        rule.setLevel(ruleApplyingEntity.getLevel());
        rule.setEnabled(ruleApplyingEntity.getEnabled());
        rule.setCreateTime(ruleApplyingEntity.getCreateTime());
        rule.setUpdateTime(ruleApplyingEntity.getUpdateTime());
        if (CollectionUtils.isNotEmpty(ruleApplyingEntity.getAppliedDialectTypes())) {
            rule.setAppliedDialectTypes(ruleApplyingEntity.getAppliedDialectTypes().stream()
                    .map(DialectType::fromValue).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(ruleApplyingEntity.getPropertiesJson())) {
            rule.setProperties(JsonUtils.fromJson(ruleApplyingEntity.getPropertiesJson(),
                    new TypeReference<Map<String, Object>>() {}));
        }
        rule.setOrganizationId(ruleApplyingEntity.getOrganizationId());
        return rule;
    }
}
