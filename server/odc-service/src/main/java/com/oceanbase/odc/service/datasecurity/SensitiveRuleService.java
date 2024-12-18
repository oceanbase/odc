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
package com.oceanbase.odc.service.datasecurity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.datasecurity.SensitiveRuleEntity;
import com.oceanbase.odc.metadb.datasecurity.SensitiveRuleRepository;
import com.oceanbase.odc.metadb.datasecurity.SensitiveRuleSpecs;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.datasecurity.model.QuerySensitiveRuleParams;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRule;
import com.oceanbase.odc.service.datasecurity.util.SensitiveRuleMapper;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * @author gaoda.xy
 * @date 2023/5/19 10:36
 */
@Slf4j
@Service
@Validated
@Authenticated
public class SensitiveRuleService {

    @Autowired
    private MaskingAlgorithmService algorithmService;

    @Autowired
    private SensitiveRuleRepository ruleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HorizontalDataPermissionValidator horizontalPermissionValidator;

    private static final SensitiveRuleMapper ruleMapper = SensitiveRuleMapper.INSTANCE;

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA"}, actions = {"OWNER", "DBA"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public Boolean exists(@NotNull Long projectId, @NotBlank String name) {
        SensitiveRuleEntity entity = new SensitiveRuleEntity();
        entity.setName(name);
        entity.setProjectId(projectId);
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        return ruleRepository.exists(Example.of(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, SECURITY_ADMINISTRATOR"},
            actions = {"OWNER", "DBA", "SECURITY_ADMINISTRATOR"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public SensitiveRule create(@NotNull Long projectId, @NotNull @Valid SensitiveRule rule) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        Long userId = authenticationFacade.currentUserId();
        PreConditions.validNoDuplicated(ResourceType.ODC_SENSITIVE_RULE, "name", rule.getName(),
                () -> exists(projectId, rule.getName()));
        horizontalPermissionValidator.checkCurrentOrganization(
                algorithmService.batchNullSafeGetModel(Collections.singleton(rule.getMaskingAlgorithmId())));
        SensitiveRuleEntity entity = ruleMapper.modelToEntity(rule);
        entity.setBuiltin(false);
        entity.setProjectId(projectId);
        entity.setCreatorId(userId);
        entity.setOrganizationId(organizationId);
        ruleRepository.save(entity);
        log.info("Sensitive rule has been created, id={}, name={}", entity.getId(), entity.getName());
        return detail(projectId, entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, SECURITY_ADMINISTRATOR"},
            actions = {"OWNER", "DBA", "SECURITY_ADMINISTRATOR"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public SensitiveRule detail(@NotNull Long projectId, @NotNull Long id) {
        SensitiveRuleEntity entity = nullSafeGet(id);
        checkCurrentProject(projectId, entity);
        SensitiveRule rule = ruleMapper.entityToModel(entity);
        horizontalPermissionValidator.checkCurrentOrganization(rule);
        if (Objects.nonNull(entity.getCreatorId())) {
            Optional<UserEntity> userEntityOptional = userRepository.findById(entity.getCreatorId());
            userEntityOptional.ifPresent(user -> rule.setCreator(new InnerUser(user)));
        }
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, SECURITY_ADMINISTRATOR"},
            actions = {"OWNER", "DBA", "SECURITY_ADMINISTRATOR"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public SensitiveRule update(@NotNull Long projectId, @NotNull Long id, @NotNull @Valid SensitiveRule rule) {
        SensitiveRuleEntity entity = nullSafeGet(id);
        checkCurrentProject(projectId, entity);
        horizontalPermissionValidator.checkCurrentOrganization(ruleMapper.entityToModel(entity));
        if (entity.getBuiltin()) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"builtin sensitive rule"},
                    "Operation on builtin sensitive rule is not allowed");
        }
        Long algorithmId = rule.getMaskingAlgorithmId();
        horizontalPermissionValidator
                .checkCurrentOrganization(algorithmService.batchNullSafeGetModel(Collections.singleton(algorithmId)));
        entity.setName(rule.getName());
        entity.setEnabled(rule.getEnabled());
        entity.setType(rule.getType());
        entity.setDatabaseRegexExpression(rule.getDatabaseRegexExpression());
        entity.setTableRegexExpression(rule.getTableRegexExpression());
        entity.setColumnRegexExpression(rule.getColumnRegexExpression());
        entity.setColumnCommentRegexExpression(rule.getColumnCommentRegexExpression());
        entity.setGroovyScript(rule.getGroovyScript());
        entity.setPathIncludes(rule.getPathIncludes());
        entity.setPathExcludes(rule.getPathExcludes());
        entity.setLevel(rule.getLevel());
        entity.setMaskingAlgorithmId(algorithmId);
        entity.setDescription(rule.getDescription());
        ruleRepository.saveAndFlush(entity);
        log.info("Sensitive rule has been updated, id={}, name={}", entity.getId(), entity.getName());
        return detail(entity.getProjectId(), entity.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, SECURITY_ADMINISTRATOR"},
            actions = {"OWNER", "DBA", "SECURITY_ADMINISTRATOR"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public SensitiveRule delete(@NotNull Long projectId, @NotNull Long id) {
        SensitiveRuleEntity entity = nullSafeGet(id);
        checkCurrentProject(projectId, entity);
        SensitiveRule rule = ruleMapper.entityToModel(entity);
        horizontalPermissionValidator.checkCurrentOrganization(rule);
        if (entity.getBuiltin()) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"builtin sensitive rule"},
                    "Operation on builtin sensitive rule is not allowed");
        }
        ruleRepository.delete(entity);
        log.info("Sensitive rule has been deleted, id={}, name={}", entity.getId(), entity.getName());
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, SECURITY_ADMINISTRATOR"},
            actions = {"OWNER", "DBA", "SECURITY_ADMINISTRATOR"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public Page<SensitiveRule> list(@NotNull Long projectId, @NotNull @Valid QuerySensitiveRuleParams params,
            Pageable pageable) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        Specification<SensitiveRuleEntity> spec = Specification
                .where(SensitiveRuleSpecs.projectIdEqual(projectId))
                .and(SensitiveRuleSpecs.nameLike(params.getName()))
                .and(SensitiveRuleSpecs.typeIn(params.getTypes()))
                .and(SensitiveRuleSpecs.maskingAlgorithmIdIn(params.getMaskingAlgorithmIds()))
                .and(SensitiveRuleSpecs.enabledEqual(params.getEnabled()))
                .and(SensitiveRuleSpecs.organizationIdEqual(organizationId));
        return ruleRepository.findAll(spec, pageable).map(ruleMapper::entityToModel);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(hasAnyResourceRole = {"OWNER, DBA, SECURITY_ADMINISTRATOR"},
            actions = {"OWNER", "DBA", "SECURITY_ADMINISTRATOR"}, resourceType = "ODC_PROJECT",
            indexOfIdParam = 0)
    public SensitiveRule setEnabled(@NotNull Long projectId, @NotNull Long id, @NotNull Boolean enabled) {
        SensitiveRuleEntity entity = nullSafeGet(id);
        checkCurrentProject(projectId, entity);
        SensitiveRule rule = ruleMapper.entityToModel(entity);
        horizontalPermissionValidator.checkCurrentOrganization(rule);
        if (entity.getBuiltin()) {
            throw new UnsupportedException(ErrorCodes.IllegalOperation, new Object[] {"builtin sensitive rule"},
                    "Operation on builtin sensitive rule is not allowed");
        }
        if (!Objects.equals(entity.getEnabled(), enabled)) {
            entity.setEnabled(enabled);
            ruleRepository.saveAndFlush(entity);
            log.info("Sensitive rule has been updated, id={}, name={}", entity.getId(), entity.getName());
        }
        return ruleMapper.entityToModel(entity);
    }

    @SkipAuthorize("odc internal usages")
    public List<SensitiveRule> getByProjectIdAndEnabled(@NotNull Long projectId) {
        List<SensitiveRuleEntity> entities = ruleRepository.findByProjectIdAndEnabled(projectId, true);
        return entities.stream().map(ruleMapper::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usages")
    public List<SensitiveRule> batchNullSafeGetModel(Set<Long> ids) {
        List<SensitiveRuleEntity> entities = ruleRepository.findByIdIn(ids);
        if (ids.size() > entities.size()) {
            Set<Long> presentIds = entities.stream().map(SensitiveRuleEntity::getId).collect(Collectors.toSet());
            String absentIds = ids.stream().filter(id -> !presentIds.contains(id)).map(Object::toString)
                    .collect(Collectors.joining(","));
            throw new NotFoundException(ResourceType.ODC_SENSITIVE_RULE, "id", absentIds);
        }
        return entities.stream().map(ruleMapper::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usages")
    public SensitiveRuleEntity nullSafeGet(@NotNull Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_SENSITIVE_RULE, "id", id));
    }

    private void checkCurrentProject(@NotNull Long projectId, @NotNull SensitiveRuleEntity entity) {
        PreConditions.validExists(ResourceType.ODC_SENSITIVE_RULE, "id", entity.getId(),
                () -> Objects.equals(projectId, entity.getProjectId()));
    }

}
