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

    /**
     * 创建自动化规则
     *
     * @param request 创建规则请求
     * @return 创建的自动化规则
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_AUTOMATION_RULE", isForAll = true)
    public AutomationRule create(@NotNull @Valid CreateRuleReq request) {
        // 参数校验
        PreConditions.notNull(request, "createRuleRequest");
        PreConditions.notBlank(request.getName(), "rule.name");
        PreConditions.validNoDuplicated(ResourceType.ODC_AUTOMATION_RULE, "rule.name", request.getName(),
            () -> exists(request.getName()));
        // 判断触发事件是否存在
        if (!eventRepository.findById(request.getEventId()).isPresent()) {
            throw new BadRequestException("No event found by event id:" + request.getEventId());
        }

        // 创建自动触发规则实体
        AutomationRuleEntity ruleEntity = new AutomationRuleEntity();
        ruleEntity.setName(request.getName());
        ruleEntity.setEventId(request.getEventId());
        ruleEntity.setCreatorId(authenticationFacade.currentUserId());
        ruleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        ruleEntity.setDescription(request.getDescription());
        ruleEntity.setEnabled(request.getEnabled());
        ruleEntity.setBuiltIn(false);
        AutomationRuleEntity savedRuleEntity = ruleRepository.saveAndFlush(ruleEntity);

        // 将自动触发规则实体转换为自动化规则模型
        AutomationRule rule = entityToModel(savedRuleEntity);
        // 插入条件
        if (CollectionUtils.isNotEmpty(request.getConditions())) {
            for (AutomationCondition condition : request.getConditions()) {
                // 检查操作是否合法
                if (!checkOperation(condition.getOperation())) {
                    throw new UnsupportedOperationException("Illegal operation :" + condition);
                }
            }
            rule.setConditions(conditionService.insertAll(rule.getId(), request.getConditions()));
        }
        // 插入操作
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

    /**
     * 更新自动化规则
     *
     * @param id  规则ID
     * @param req 创建规则请求体
     * @return 更新后的自动化规则
     */
    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "update", resourceType = "ODC_AUTOMATION_RULE", indexOfIdParam = 0)
    public AutomationRule update(@NotNull Long id, @NotNull @Valid CreateRuleReq req) {
        // 获取规则实体
        AutomationRuleEntity ruleEntity = nullSafeGet(id);
        // 如果请求体中包含规则名称，则更新规则实体的名称
        if (Objects.nonNull(req.getName())) {
            ruleEntity.setName(req.getName());
        }
        // 如果请求体中包含事件ID，则更新规则实体的事件ID
        if (Objects.nonNull(req.getEventId())) {
            // 判断事件是否存在，若不存在则抛出异常
            if (!eventRepository.findById(req.getEventId()).isPresent()) {
                throw new BadRequestException("No event found by event id:" + req.getEventId());
            }
            ruleEntity.setEventId(req.getEventId());
        }
        // 如果请求体中包含启用状态，则更新规则实体的启用状态
        if (Objects.nonNull(req.getEnabled())) {
            ruleEntity.setEnabled(req.getEnabled());
        }
        // 如果请求体中包含规则描述，则更新规则实体的描述
        if (Objects.nonNull(req.getDescription())) {
            ruleEntity.setDescription(req.getDescription());
        }
        // 设置规则实体的最后修改人ID
        ruleEntity.setLastModifierId(authenticationFacade.currentUserId());
        // 更新规则实体
        ruleRepository.updateById(ruleEntity);
        // 将规则实体转换为自动化规则模型
        AutomationRule automationRule = entityToModel(nullSafeGet(id));
        // 获取自动化规则的ID
        Long ruleId = automationRule.getId();
        // 如果请求体中包含条件列表，则删除原有条件并插入新的条件
        if (Objects.nonNull(req.getConditions())) {
            for (AutomationCondition condition : req.getConditions()) {
                // 检查操作是否合法，若不合法则抛出异常
                if (!checkOperation(condition.getOperation())) {
                    throw new UnsupportedOperationException("Illegal operation :" + condition);
                }
            }
            conditionService.deleteByRuleId(ruleId);
            automationRule.setConditions(conditionService.insertAll(ruleId, req.getConditions()));
        }
        // 如果请求体中包含动作列表，则删除原有动作并插入新的动作
        if (Objects.nonNull(req.getActions())) {
            actionService.deleteByRuleId(ruleId);
            automationRule.setActions(actionService.insertAll(ruleId, req.getActions()));
        }
        // 返回更新后的自动化规则
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

    /**
     * 将实体类转换为模型类，不包括创建者信息
     *
     * @param ruleEntity 实体类对象
     * @return 模型类对象
     */
    private AutomationRule entityToModelWithoutCreator(AutomationRuleEntity ruleEntity) {
        // 创建模型类对象
        AutomationRule automationRule = new AutomationRule(ruleEntity);
        // 根据事件ID查询事件元数据实体类
        Optional<EventMetadataEntity> optional = eventRepository.findById(ruleEntity.getEventId());
        // 如果事件ID不存在，则抛出异常
        if (!optional.isPresent()) {
            throw new UnexpectedException("Unexpected event id:" + ruleEntity.getEventId());
        }
        // 设置模型类对象的事件名称
        automationRule.setEventName(optional.get().getName());
        // 返回模型类对象
        return automationRule;
    }

    /**
     * 将实体类转换为模型类
     *
     * @param ruleEntity 自动化规则实体类
     * @return 自动化规则模型类
     */
    private AutomationRule entityToModel(AutomationRuleEntity ruleEntity) {
        // 调用entityToModelWithoutCreator方法将实体类转换为模型类，同时获取创建人信息
        AutomationRule automationRule = entityToModelWithoutCreator(ruleEntity);
        // 校验当前组织是否有权限
        permissionValidator.checkCurrentOrganization(automationRule);
        try {
            // 查询创建人信息
            User creator = userService.detailWithoutPermissionCheck(ruleEntity.getCreatorId());
            // 设置创建人名称
            automationRule.setCreatorName(creator.getName());
        } catch (Exception ex) {
            // 查询创建人信息失败，记录日志
            log.warn("Query creator name failed, reason={}", ex.getMessage());
        }
        // 返回自动化规则模型类
        return automationRule;
    }

    /**
     * 根据ID获取自动化规则实体对象，如果不存在则抛出NotFoundException异常
     *
     * @param id 自动化规则实体对象的ID
     * @return 自动化规则实体对象
     * @throws NotFoundException 如果自动化规则实体对象不存在，则抛出NotFoundException异常
     */
    private AutomationRuleEntity nullSafeGet(Long id) {
        // 使用Optional类来处理可能为null的返回值
        Optional<AutomationRuleEntity> optional = ruleRepository.findById(id);
        // 如果返回值为空，则抛出NotFoundException异常
        if (!optional.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_AUTOMATION_RULE, "ID", id);
        }
        // 返回自动化规则实体对象
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
