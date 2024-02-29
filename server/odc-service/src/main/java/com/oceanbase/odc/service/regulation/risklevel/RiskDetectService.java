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
package com.oceanbase.odc.service.regulation.risklevel;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleRepository;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskDetectRuleSpecs;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.risklevel.model.QueryRiskDetectRuleParams;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/6/15 18:51
 * @Description: []
 */
@Service
@Authenticated
public class RiskDetectService {
    private final RiskDetectRuleMapper ruleMapper = RiskDetectRuleMapper.INSTANCE;

    @Autowired
    private RiskDetectRuleRepository riskDetectRuleRepository;

    @Autowired
    @Qualifier("RiskLevelServiceFrom420")
    private RiskLevelService riskLevelService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private UserService userService;

    @SkipAuthorize("internal usage")
    public List<RiskDetectRule> list(@NonNull QueryRiskDetectRuleParams params) {
        params.setOrganizationId(authenticationFacade.currentOrganizationId());
        return innerList(params);
    }

    @SkipAuthorize("internal usage")
    public List<RiskDetectRule> listAllByOrganizationId(@NonNull Long organizationId) {
        return innerList(QueryRiskDetectRuleParams.builder().organizationId(organizationId).build());
    }

    @SkipAuthorize("internal usage")
    public Set<RiskLevel> detect(List<RiskDetectRule> rules, @NonNull RiskLevelDescriber describer) {
        Set<RiskLevel> matched = new HashSet<>();
        if (CollectionUtils.isEmpty(rules)) {
            matched.add(riskLevelService.findDefaultRiskLevel());
            return matched;
        }
        for (RiskDetectRule rule : rules) {
            if (Objects.isNull(rule.getRootNode())) {
                continue;
            }
            if (rule.getRootNode().evaluate(describer)) {
                matched.add(rule.getRiskLevel());
            }
        }
        return matched;
    }

    @SkipAuthorize("internal authenticated")
    public RiskDetectRule detail(@NonNull Long id) {
        RiskDetectRuleEntity ruleEntity = findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id);
        return entityToModel(ruleEntity);
    }

    @PreAuthenticate(actions = "create", resourceType = "ODC_RISK_DETECT_RULE", isForAll = true)
    @Transactional(rollbackFor = Exception.class)
    public RiskDetectRule create(@NonNull RiskDetectRule rule) {
        if (Objects.isNull(rule.getRootNode())) {
            throw new BadRequestException("rule conditions cannot be empty");
        }
        Long organizationId = authenticationFacade.currentOrganizationId();
        if (!riskLevelService.exists(organizationId, rule.getRiskLevelId())) {
            throw new BadRequestException("invalid risk level");
        }
        RiskDetectRuleEntity entity = ruleMapper.modelToEntity(rule);
        entity.setCreatorId(authenticationFacade.currentUserId());
        entity.setOrganizationId(organizationId);
        entity.setBuiltIn(false);
        RiskDetectRuleEntity savedRule = riskDetectRuleRepository.save(entity);
        return entityToModel(savedRule);
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_RISK_DETECT_RULE", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public RiskDetectRule update(@NonNull Long id, @NonNull RiskDetectRule rule) {
        RiskDetectRuleEntity updateEntity = modelToEntity(rule);
        RiskDetectRuleEntity savedRule = findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id);
        savedRule.setName(updateEntity.getName());
        savedRule.setValueJson(updateEntity.getValueJson());
        return entityToModel(riskDetectRuleRepository.save(savedRule));
    }

    @PreAuthenticate(actions = "delete", resourceType = "ODC_RISK_DETECT_RULE", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public RiskDetectRule delete(@NonNull Long id) {
        RiskDetectRuleEntity saved = findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id);
        riskDetectRuleRepository.delete(saved);
        return entityToModel(saved);
    }


    @SkipAuthorize("internal usage")
    public RiskDetectRuleEntity findByOrganizationIdAndId(@NonNull Long organizationId, @NonNull Long id) {
        return riskDetectRuleRepository.findByOrganizationIdAndId(organizationId, id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RISK_DETECT_RULE, "id", id));
    }

    private List<RiskDetectRule> innerList(@NonNull QueryRiskDetectRuleParams params) {
        Specification<RiskDetectRuleEntity> specs = RiskDetectRuleSpecs.riskLevelIdEqual(params.getRiskLevelId())
                .and(RiskDetectRuleSpecs.organizationIdEqual(authenticationFacade.currentOrganizationId()))
                .and(RiskDetectRuleSpecs.nameLike(params.getName()));
        List<RiskDetectRuleEntity> ruleEntities = riskDetectRuleRepository
                .findAll(specs, PageRequest.of(0, Integer.MAX_VALUE, Sort.by("id")))
                .toList();
        if (CollectionUtils.isEmpty(ruleEntities)) {
            return Collections.emptyList();
        }
        return ruleEntities.stream()
                .map(ruleEntity -> entityToModel(ruleEntity))
                .collect(Collectors.toList());
    }

    private RiskDetectRule entityToModel(RiskDetectRuleEntity ruleEntity) {
        RiskDetectRule rule = ruleMapper.entityToModel(ruleEntity);
        rule.setRiskLevel(
                riskLevelService.findById(ruleEntity.getRiskLevelId()).orElseThrow(() -> new NotFoundException(
                        ResourceType.ODC_RISK_LEVEL, "id", ruleEntity.getRiskLevelId())));
        return rule;
    }

    private RiskDetectRuleEntity modelToEntity(RiskDetectRule rule) {
        return ruleMapper.modelToEntity(rule);
    }

}
