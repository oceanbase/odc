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
package com.oceanbase.odc.service.automation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.permission.ResourcePermission;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.metadb.automation.AutomationActionEntity;
import com.oceanbase.odc.metadb.automation.AutomationActionRepository;
import com.oceanbase.odc.metadb.automation.AutomationConditionEntity;
import com.oceanbase.odc.metadb.automation.AutomationConditionRepository;
import com.oceanbase.odc.metadb.automation.AutomationRuleEntity;
import com.oceanbase.odc.metadb.automation.AutomationRuleRepository;
import com.oceanbase.odc.metadb.automation.EventMetadataEntity;
import com.oceanbase.odc.metadb.automation.EventMetadataRepository;
import com.oceanbase.odc.service.automation.model.AutomationAction;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.automation.model.AutomationEventMetadata;
import com.oceanbase.odc.service.automation.model.AutomationRule;
import com.oceanbase.odc.service.automation.model.AutomationRuleSpecs;
import com.oceanbase.odc.service.automation.model.CreateRuleReq;
import com.oceanbase.odc.service.automation.model.QueryAutomationRuleParams;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ResourcePermissionAccessor;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Validated
@Authenticated
public class AutomationService {
    @Autowired
    private AutomationRuleRepository ruleRepository;
    @Autowired
    private AutomationConditionRepository conditionRepository;
    @Autowired
    private AutomationActionRepository actionRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private AutomationConditionService conditionService;
    @Autowired
    private AutomationActionService actionService;
    @Autowired
    private EventMetadataRepository eventRepository;
    @Autowired
    private ResourcePermissionAccessor resourcePermissionAccessor;
    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @SkipAuthorize
    public List<AutomationEventMetadata> listEventMetadata() {
        return eventRepository.findAll().stream()
                .map(AutomationEventMetadata::new)
                .filter(metadata -> !metadata.getHidden())
                .collect(Collectors.toList());
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_AUTOMATION_RULE", indexOfIdParam = 0)
    public AutomationRule detail(@NotNull Long id) {
        AutomationRule automationRule = entityToModel(nullSafeGet(id));
        queryRelatedConditionAndAction(automationRule);
        return automationRule;
    }

    @SkipAuthorize("permission check inside")
    public Page<AutomationRule> listRules(@NotNull Pageable pageable, @NotNull QueryAutomationRuleParams params) {
        Map<String, Set<String>> permittedActions = resourcePermissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_AUTOMATION_RULE, permission -> {
                    ResourcePermission minPermission = new ResourcePermission(permission.getResourceId(),
                            ResourceType.ODC_AUTOMATION_RULE.name(), "read");
                    return permission.implies(minPermission);
                });
        if (permittedActions.isEmpty()) {
            return Page.empty(pageable);
        }
        String creatorName = params.getCreatorName();
        List<Long> creatorIds = null;
        if (StringUtils.isNotBlank(creatorName)) {
            creatorIds = userService.getUsersByFuzzyNameWithoutPermissionCheck(creatorName)
                    .stream().map(User::getId).collect(Collectors.toList());
        }
        Specification<AutomationRuleEntity> specification = Specification
                .where(AutomationRuleSpecs.valueIn("creatorId", creatorIds))
                .and(AutomationRuleSpecs.valueEquals("enabled", params.getEnabled()))
                .and(AutomationRuleSpecs.valueLike("name", params.getName()))
                .and(AutomationRuleSpecs.valueEquals("organizationId", authenticationFacade.currentOrganizationId()));
        if (!permittedActions.containsKey("*")) {
            specification = specification.and(AutomationRuleSpecs.valueIn("id",
                    permittedActions.keySet().stream().map(Long::parseLong).collect(Collectors.toList())));
        }
        return ruleRepository.findAll(specification, pageable).map(this::entityToModel);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_AUTOMATION_RULE", isForAll = true)
    public AutomationRule create(@NotNull @Valid CreateRuleReq request) {
        PreConditions.notNull(request, "createRuleRequest");
        PreConditions.notBlank(request.getName(), "rule.name");
        PreConditions.validNoDuplicated(ResourceType.ODC_AUTOMATION_RULE, "rule.name", request.getName(),
                () -> exists(request.getName()));
        if (!eventRepository.findById(request.getEventId()).isPresent()) {
            throw new BadRequestException("No event found by event id:" + request.getEventId());
        }

        AutomationRuleEntity ruleEntity = new AutomationRuleEntity();
        ruleEntity.setName(request.getName());
        ruleEntity.setEventId(request.getEventId());
        ruleEntity.setCreatorId(authenticationFacade.currentUserId());
        ruleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        ruleEntity.setDescription(request.getDescription());
        ruleEntity.setEnabled(request.getEnabled());
        ruleEntity.setBuiltIn(false);
        AutomationRuleEntity savedRuleEntity = ruleRepository.saveAndFlush(ruleEntity);

        AutomationRule rule = entityToModel(savedRuleEntity);
        if (CollectionUtils.isNotEmpty(request.getConditions())) {
            for (AutomationCondition condition : request.getConditions()) {
                if (!checkOperation(condition.getOperation())) {
                    throw new UnsupportedOperationException("Illegal operation :" + condition);
                }
            }
            rule.setConditions(conditionService.insertAll(rule.getId(), request.getConditions()));
        }
        if (CollectionUtils.isNotEmpty(request.getActions())) {
            rule.setActions(actionService.insertAll(rule.getId(), request.getActions()));
        }
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_AUTOMATION_RULE", indexOfIdParam = 0)
    public AutomationRule setRuleEnabled(@NotNull Long id, @NotNull boolean enabled) {
        CreateRuleReq req = new CreateRuleReq();
        req.setEnabled(enabled);
        return update(id, req);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_AUTOMATION_RULE", indexOfIdParam = 0)
    public AutomationRule update(@NotNull Long id, @NotNull @Valid CreateRuleReq req) {
        AutomationRuleEntity ruleEntity = nullSafeGet(id);
        if (Objects.nonNull(req.getName())) {
            ruleEntity.setName(req.getName());
        }
        if (Objects.nonNull(req.getEventId())) {
            if (!eventRepository.findById(req.getEventId()).isPresent()) {
                throw new BadRequestException("No event found by event id:" + req.getEventId());
            }
            ruleEntity.setEventId(req.getEventId());
        }
        if (Objects.nonNull(req.getEnabled())) {
            ruleEntity.setEnabled(req.getEnabled());
        }
        if (Objects.nonNull(req.getDescription())) {
            ruleEntity.setDescription(req.getDescription());
        }
        ruleEntity.setLastModifierId(authenticationFacade.currentUserId());
        ruleRepository.updateById(ruleEntity);
        AutomationRule automationRule = entityToModel(nullSafeGet(id));
        Long ruleId = automationRule.getId();
        if (Objects.nonNull(req.getConditions())) {
            for (AutomationCondition condition : req.getConditions()) {
                if (!checkOperation(condition.getOperation())) {
                    throw new UnsupportedOperationException("Illegal operation :" + condition);
                }
            }
            conditionService.deleteByRuleId(ruleId);
            automationRule.setConditions(conditionService.insertAll(ruleId, req.getConditions()));
        }
        if (Objects.nonNull(req.getActions())) {
            actionService.deleteByRuleId(ruleId);
            automationRule.setActions(actionService.insertAll(ruleId, req.getActions()));
        }
        return automationRule;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "delete", resourceType = "ODC_AUTOMATION_RULE", indexOfIdParam = 0)
    public AutomationRule delete(@NotNull Long id) {
        AutomationRuleEntity ruleEntity = nullSafeGet(id);
        ruleRepository.delete(ruleEntity);
        AutomationRule automationRule = entityToModel(ruleEntity);
        automationRule.setConditions(conditionService.deleteByRuleId(id));
        automationRule.setActions(actionService.deleteByRuleId(id));
        return automationRule;
    }

    @SkipAuthorize("permission check inside")
    public boolean exists(@NotNull String ruleName) {
        Specification<AutomationRuleEntity> specification = Specification
                .where(AutomationRuleSpecs.valueEquals("name", ruleName))
                .and(AutomationRuleSpecs.valueEquals("organizationId", authenticationFacade.currentOrganizationId()));

        Map<String, Set<String>> permittedActions = resourcePermissionAccessor.permittedResourceActions(
                authenticationFacade.currentUserId(), ResourceType.ODC_AUTOMATION_RULE, permission -> {
                    ResourcePermission minPermission = new ResourcePermission(permission.getResourceId(),
                            ResourceType.ODC_AUTOMATION_RULE.name(), "read");
                    return permission.implies(minPermission);
                });
        if (permittedActions.isEmpty()) {
            throw new AccessDeniedException("No permission to access automation rules");
        }
        if (!permittedActions.containsKey("*")) {
            specification = specification.and(AutomationRuleSpecs.valueIn("id",
                    permittedActions.keySet().stream().map(Long::parseLong).collect(Collectors.toList())));
        }
        return ruleRepository.findAll(specification).size() > 0;
    }

    @SkipAuthorize("odc internal usage")
    public List<AutomationRule> listRulesByEventName(String eventName) {
        EventMetadataEntity eventEntity = eventRepository.findByName(eventName);
        if (Objects.isNull(eventEntity)) {
            throw new UnexpectedException("Event not found by name " + eventName);
        }
        return ruleRepository.findAllByEventId(eventEntity.getId()).stream()
                .map(this::entityToModelWithoutCreator)
                .peek(this::queryRelatedConditionAndAction)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("odc internal usage")
    public static boolean checkOperation(String operation) {
        String[] permittedOperations = new String[] {
                "contains",
                "matches",
                "equals"
        };
        return Arrays.asList(permittedOperations).contains(operation);
    }

    private AutomationRule entityToModelWithoutCreator(AutomationRuleEntity ruleEntity) {
        AutomationRule automationRule = new AutomationRule(ruleEntity);
        Optional<EventMetadataEntity> optional = eventRepository.findById(ruleEntity.getEventId());
        if (!optional.isPresent()) {
            throw new UnexpectedException("Unexpected event id:" + ruleEntity.getEventId());
        }
        automationRule.setEventName(optional.get().getName());
        return automationRule;
    }

    private AutomationRule entityToModel(AutomationRuleEntity ruleEntity) {
        AutomationRule automationRule = entityToModelWithoutCreator(ruleEntity);
        permissionValidator.checkCurrentOrganization(automationRule);
        try {
            User creator = userService.detailWithoutPermissionCheck(ruleEntity.getCreatorId());
            automationRule.setCreatorName(creator.getName());
        } catch (Exception ex) {
            log.warn("Query creator name failed, reason={}", ex.getMessage());
        }
        return automationRule;
    }

    private AutomationRuleEntity nullSafeGet(Long id) {
        Optional<AutomationRuleEntity> optional = ruleRepository.findById(id);
        if (!optional.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_AUTOMATION_RULE, "ID", id);
        }
        return optional.get();
    }

    private void queryRelatedConditionAndAction(AutomationRule automationRule) {
        List<AutomationConditionEntity> conditionEntities = conditionRepository.findByRuleId(automationRule.getId());
        List<AutomationCondition> automationConditions =
                conditionEntities.stream().map(AutomationCondition::of).collect(Collectors.toList());
        List<AutomationActionEntity> actionEntities = actionRepository.findByRuleId(automationRule.getId());
        List<AutomationAction> automationActions =
                actionEntities.stream().map(AutomationAction::of).collect(Collectors.toList());
        automationRule.setConditions(automationConditions);
        automationRule.setActions(automationActions);
    }


}
