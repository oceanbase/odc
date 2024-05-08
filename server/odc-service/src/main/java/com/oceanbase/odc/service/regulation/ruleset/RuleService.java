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
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.DefaultRuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.RuleMetadata;
import com.oceanbase.odc.service.regulation.ruleset.model.Ruleset;

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
    private RulesetService rulesetService;

    @Autowired
    private RuleApplyingRepository ruleApplyingRepository;

    @Autowired
    private DefaultRuleApplyingRepository defaultRuleApplyingRepository;

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
        return new Stats()
                .andDistinct("subTypes", subTypes)
                .andDistinct("supportedDialectTypes", supportedDialectTypes);
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
        Ruleset ruleset = rulesetService.detail(rulesetId);
        if (!ruleset.getBuiltin()) {
            RuleApplyingEntity applyingEntity = ruleApplyingRepository.findById(ruleId).orElseThrow(
                    () -> new UnexpectedException("rule applying not found, ruleId = " + ruleId));
            Rule rule = entityToModel(applyingEntity);
            rule.setRulesetId(rulesetId);
            rule.setMetadata(metadataService.detail(applyingEntity.getRuleMetadataId()));
            return rule;
        }

        DefaultRuleApplyingEntity defaultApplying = defaultRuleApplyingRepository.findById(ruleId).orElseThrow(
                () -> new UnexpectedException("default rule applying not found, ruleId = " + ruleId));
        Optional<RuleApplyingEntity> applyingEntityOpt =
                ruleApplyingRepository.findByOrganizationIdAndRulesetIdAndRuleMetadataId(
                        authenticationFacade.currentOrganizationId(), rulesetId, defaultApplying.getRuleMetadataId());
        RuleApplyingEntity merged = RuleApplyingEntity.merge(defaultApplying, applyingEntityOpt);
        Rule rule = entityToModel(merged);
        rule.setRulesetId(rulesetId);
        rule.setMetadata(metadataService.detail(merged.getRuleMetadataId()));
        return rule;
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_RULESET", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Rule update(@NonNull Long rulesetId, @NonNull Long ruleId, @NonNull Rule rule) {
        Ruleset ruleset = rulesetService.detail(rulesetId);
        if (!ruleset.getBuiltin()) {
            RuleApplyingEntity savedRuleApplyingOpt;
            savedRuleApplyingOpt = ruleApplyingRepository.findById(ruleId).orElseThrow(
                    () -> new UnexpectedException("rule applying not found, ruleId = " + ruleId));
            savedRuleApplyingOpt.setLevel(rule.getLevel());
            savedRuleApplyingOpt.setEnabled(rule.getEnabled());
            savedRuleApplyingOpt.setPropertiesJson(JsonUtils.toJson(rule.getProperties()));
            if (Objects.nonNull(rule.getAppliedDialectTypes())) {
                savedRuleApplyingOpt.setAppliedDialectTypes(
                        rule.getAppliedDialectTypes().stream().map(DialectType::name).collect(Collectors.toList()));
            }
            ruleApplyingRepository.save(savedRuleApplyingOpt);
            rulesetId2RulesCache.invalidate(rulesetId);
            return rule;
        }
        DefaultRuleApplyingEntity defaultApplying = defaultRuleApplyingRepository.findById(ruleId).orElseThrow(
                () -> new UnexpectedException("default rule applying not found, ruleId = " + ruleId));
        Optional<RuleApplyingEntity> savedOpt =
                ruleApplyingRepository.findByOrganizationIdAndRulesetIdAndRuleMetadataId(
                        authenticationFacade.currentOrganizationId(), rulesetId, defaultApplying.getRuleMetadataId());
        RuleApplyingEntity saved;
        if (savedOpt.isPresent()) {
            saved = savedOpt.get();
            saved.setLevel(rule.getLevel());
            saved.setEnabled(rule.getEnabled());
            saved.setPropertiesJson(JsonUtils.toJson(rule.getProperties()));
            if (Objects.nonNull(rule.getAppliedDialectTypes())) {
                saved.setAppliedDialectTypes(
                        rule.getAppliedDialectTypes().stream().map(DialectType::name).collect(Collectors.toList()));
            }
        } else {
            saved = new RuleApplyingEntity();
            saved.setOrganizationId(authenticationFacade.currentOrganizationId());
            saved.setRuleMetadataId(defaultApplying.getRuleMetadataId());
            saved.setLevel(rule.getLevel());
            saved.setEnabled(rule.getEnabled());
            saved.setPropertiesJson(JsonUtils.toJson(rule.getProperties()));
            if (Objects.nonNull(rule.getAppliedDialectTypes())) {
                saved.setAppliedDialectTypes(
                        rule.getAppliedDialectTypes().stream().map(DialectType::name).collect(Collectors.toList()));
            }
            saved.setRulesetId(rulesetId);
        }
        ruleApplyingRepository.save(saved);
        rulesetId2RulesCache.invalidate(rulesetId);
        return rule;
    }

    @SkipAuthorize("internal usage")
    @Transactional(rollbackFor = Exception.class)
    public List<Rule> create(@NotNull Long rulesetId, @NotEmpty List<Rule> rules) {
        List<RuleApplyingEntity> entities = rules.stream().map(rule -> {
            RuleApplyingEntity entity = new RuleApplyingEntity();
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            entity.setRulesetId(rulesetId);
            entity.setRuleMetadataId(rule.getMetadata().getId());
            entity.setLevel(rule.getLevel());
            entity.setEnabled(rule.getEnabled());
            entity.setPropertiesJson(JsonUtils.toJson(rule.getProperties()));
            if (Objects.nonNull(rule.getAppliedDialectTypes())) {
                entity.setAppliedDialectTypes(
                        rule.getAppliedDialectTypes().stream().map(DialectType::name).collect(Collectors.toList()));
            }
            return entity;
        }).collect(Collectors.toList());
        ruleApplyingRepository.batchCreate(entities);
        return rules;
    }


    private List<Rule> internalList(@NonNull Long rulesetId, @NonNull QueryRuleMetadataParams params) {
        List<RuleMetadata> ruleMetadatas = metadataService.list(params);
        if (CollectionUtils.isEmpty(ruleMetadatas)) {
            return Collections.emptyList();
        }
        Ruleset ruleset = rulesetService.detail(rulesetId);
        Map<Long, List<DefaultRuleApplyingEntity>> metadataId2DefaultRuleApplying =
                defaultRuleApplyingRepository.findByRulesetName(ruleset.getName()).stream()
                        .collect(Collectors.groupingBy(DefaultRuleApplyingEntity::getRuleMetadataId));
        List<Rule> rules = new ArrayList<>();
        Map<Long, List<RuleApplyingEntity>> metadataId2RuleApplying =
                ruleApplyingRepository
                        .findByOrganizationIdAndRulesetId(authenticationFacade.currentOrganizationId(), rulesetId)
                        .stream()
                        .collect(Collectors.groupingBy(RuleApplyingEntity::getRuleMetadataId));

        // user-defined ruleset, just use user-defined rule values
        if (!ruleset.getBuiltin()) {
            ruleMetadatas.forEach(metadata -> {
                RuleApplyingEntity userDefinedRuleApplying = metadataId2RuleApplying.get(metadata.getId()).get(0);
                Verify.notNull(userDefinedRuleApplying, "userDefinedRuleApplying");
                Rule rule = entityToModel(userDefinedRuleApplying);
                rule.setRulesetId(rulesetId);
                rule.setMetadata(metadata);
                rules.add(rule);
            });
            return rules;
        }
        // builtin ruleset, merge default rule values and user-defined rule values
        ruleMetadatas.forEach(metadata -> {
            if (!metadataId2DefaultRuleApplying.containsKey(metadata.getId())) {
                throw new UnexpectedException("default rule applying not found, ruleMetadataId = " + metadata.getId());
            }
            List<DefaultRuleApplyingEntity> defaultApplyings = metadataId2DefaultRuleApplying.get(metadata.getId());
            Verify.equals(1, defaultApplyings.size(), "defaultRuleApplyingEntity");
            RuleApplyingEntity merged;
            if (!metadataId2RuleApplying.containsKey(metadata.getId())) {
                merged = RuleApplyingEntity.merge(defaultApplyings.get(0), Optional.empty());
            } else {
                merged = RuleApplyingEntity.merge(defaultApplyings.get(0),
                        Optional.of(metadataId2RuleApplying.get(metadata.getId()).get(0)));
            }
            Rule rule = entityToModel(merged);
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
