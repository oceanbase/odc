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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.service.automation.model.AutomationAction;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.automation.model.AutomationRule;
import com.oceanbase.odc.service.automation.model.TriggerEvent;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project.ProjectMember;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.RoleService;
import com.oceanbase.odc.service.iam.UserPermissionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@SkipAuthorize
public abstract class AbstractAutomationEventHandler implements TriggerEventHandler, InitializingBean {

    @Autowired
    private AutomationService automationService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserPermissionService userPermissionService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ResourceRoleService resourceRoleService;

    protected final Map<String, ActionHandler<Object>> actionHandlerMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        registerActionHandlers().forEach(p -> addActionHandler(p.left, p.right));
    }

    private void addActionHandler(String name, ActionHandler<Object> applier) {
        actionHandlerMap.put(name, applier);
    }

    private void handleAction(AutomationAction action, TriggerEvent event) {
        ActionHandler<Object> actionHandler = actionHandlerMap.get(action.getAction());
        if (actionHandler == null) {
            throw new UnsupportedOperationException(
                    "not support action=" + action.getAction() + " event name=" + event.getEventName());
        }
        actionHandler.accept(action, event.getSource());
    }

    @Override
    @Transactional
    public void handle(TriggerEvent triggerEvent) {
        List<AutomationRule> enabledRules = automationService.listRulesByEventName(triggerEvent.getEventName()).stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled())).collect(Collectors.toList());

        for (AutomationRule rule : enabledRules) {
            boolean matched = true;

            List<AutomationCondition> conditions = rule.getConditions();
            if (CollectionUtils.isNotEmpty(conditions)) {
                matched = conditions.stream().allMatch(c -> match(c, triggerEvent));
            }
            if (matched && CollectionUtils.isNotEmpty(rule.getActions())) {
                rule.getActions().forEach(action -> handleAction(action, triggerEvent));
            }
        }
    }

    protected abstract boolean match(AutomationCondition condition, TriggerEvent triggerEvent);

    protected abstract List<Pair<String, ActionHandler<Object>>> registerActionHandlers();

    protected void bindRole(Long userId, AutomationAction action) {
        long roleId = ((Integer) action.getArguments().get("roleId")).longValue();
        roleService.bindUserRole(userId, roleId, action.getCreatorId(), null);
        log.info("Successfully bind roleId {} to userId {}", roleId, userId);
    }

    protected void bindPermission(Long userId, AutomationAction action) {
        String resourceType = (String) action.getArguments().get("resourceType");
        Object resourceId = action.getArguments().get("resourceId");
        if (Objects.nonNull(resourceId)) {
            if ("ALL".equals(resourceId)) {
                resourceId = null;
            } else {
                resourceId = ((Integer) resourceId).longValue();
            }
        }
        String actions = (String) action.getArguments().get("actions");
        if ("ODC_CONNECTION".equalsIgnoreCase(resourceType)) {
            userPermissionService.bindUserAndConnectionAccessPermission(userId, (Long) resourceId, actions,
                    action.getCreatorId());
            log.info("Successfully bind connectionId {} to userId {}", resourceId, userId);
        }
    }

    protected void bindProjectRole(Long userId, AutomationAction action) {
        Long projectId = ((Integer) action.getArguments().get("projectId")).longValue();
        List<Integer> roleIds = (List<Integer>) action.getArguments().get("roles");
        List<ProjectMember> members =
                resourceRoleService.listResourceRoles(Collections.singletonList(ResourceType.ODC_PROJECT)).stream()
                        .filter(resourceRole -> roleIds.contains(resourceRole.getId().intValue()))
                        .map(resourceRole -> {
                            ProjectMember member = new ProjectMember();
                            member.setRole(resourceRole.getRoleName());
                            member.setId(userId);
                            return member;
                        })
                        .collect(Collectors.toList());
        projectService.createMembersSkipPermissionCheck(projectId, action.getOrganizationId(), members);
    }


    @FunctionalInterface
    public interface ActionHandler<T> {
        void accept(AutomationAction action, T source);
    }


}
