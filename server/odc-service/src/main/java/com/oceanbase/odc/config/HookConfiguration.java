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
package com.oceanbase.odc.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.config.UserConfigService;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigService;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.ConditionExpression;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;

import lombok.extern.slf4j.Slf4j;

/**
 * Hook configuration
 * 
 * @author yizhou.xw
 * @version : HookConfiguration.java, v 0.1 2022-05-19 16:51， move from {@link BeanConfiguration}
 */
@Slf4j
@Configuration
public class HookConfiguration {

    @Autowired
    private UserService userService;

    @Autowired
    private ApprovalFlowConfigService approvalFlowConfigService;

    @Autowired
    private RiskLevelService riskLevelService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ResourceRoleService resourceRoleService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private RiskDetectService riskDetectService;

    @Autowired
    private UserConfigService userConfigService;

    @PostConstruct
    public void init() {
        userService.addPostUserDeleteHook(event -> {
            Long userId = event.getUserId();
            projectService.deleteUserRelatedProjectResources(userId, event.getAccountName(), event.getOrganizationId());
            userConfigService.deleteUserConfigurations(userId);
        });
        log.info("PostUserDeleteHook added");

        userService.addPreUserDeleteHook(event -> {
            projectReferenceCheck(event.getUserId(), event.getOrganizationId());
        });
        log.info("PreUserDeleteHook added");

        approvalFlowConfigService.addPreApprovalFlowConfigDeleteHook(event -> {
            approvalFlowConfigUsageCheck(event.getId());
        });
        log.info("PreApprovalFlowConfigDeleteHook added");

        environmentService.addDeleteHook(event -> {
            checkDataSourceReference(event.getId(), event.getOrganizationId(), AuditEventAction.DELETE_ENVIRONMENT);
            checkRiskLevelReference(event.getId(), event.getName(), event.getOrganizationId(),
                    AuditEventAction.DELETE_ENVIRONMENT);
        });
        log.info("PreDeleteEnvironmentHooks added");

        environmentService.addDisableHook(event -> checkDataSourceReference(event.getId(), event.getOrganizationId(),
                AuditEventAction.DISABLE_ENVIRONMENT));
        log.info("PreDisableEnvironmentHooks added");
    }

    private void approvalFlowConfigUsageCheck(Long flowConfigId) {
        List<RiskLevel> relatedRiskLevels = riskLevelService.listRelatedRiskLevelByFlowConfigId(flowConfigId);
        if (relatedRiskLevels.size() > 0) {
            String names = relatedRiskLevels.stream()
                    .map(e -> I18n.translate(e.getName().substring(2, e.getName().length() - 1), null, e.getName(),
                            LocaleContextHolder.getLocale()))
                    .collect(Collectors.joining(", "));
            String errorMessage = String.format(
                    "Approval flow config id=%s cannot be %s because it has been referenced to following risk level: {%s}",
                    flowConfigId, AuditEventAction.DELETE_FLOW_CONFIG, names);
            throw new BadRequestException(ErrorCodes.CannotOperateDueReference,
                    new Object[] {AuditEventAction.DELETE_FLOW_CONFIG.getLocalizedMessage(),
                            ResourceType.ODC_APPROVAL_FLOW_CONFIG.getLocalizedMessage(), "name", names,
                            ResourceType.ODC_RISK_LEVEL.getLocalizedMessage()},
                    errorMessage);
        }
    }

    private void projectReferenceCheck(Long userId, Long organizationId) {
        Map<Long, ProjectEntity> id2Project =
                projectRepository.findAllByOrganizationId(organizationId).stream().filter(p -> !p.getArchived())
                        .collect(Collectors.toMap(ProjectEntity::getId, p -> p));

        Map<Long, Set<ResourceRoleName>> projectId2ResourceRoleNames =
                resourceRoleService.getProjectId2ResourceRoleNames(userId).entrySet().stream()
                        .filter(e -> e.getValue().contains(ResourceRoleName.OWNER)
                                || e.getValue().contains(ResourceRoleName.DBA))
                        .filter(e -> id2Project.containsKey(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (projectId2ResourceRoleNames.size() == 0) {
            return;
        }
        String names = projectId2ResourceRoleNames.keySet().stream()
                .map(id -> id2Project.get(id).getName())
                .collect(Collectors.joining(", "));

        String errorMessage = String.format(
                "User id=%s cannot be deleted because it has been referenced to following project: {%s}", userId,
                names);

        throw new BadRequestException(ErrorCodes.CannotOperateDueReference,
                new Object[] {AuditEventAction.DELETE_USER.getLocalizedMessage(),
                        ResourceType.ODC_USER.getLocalizedMessage(), "name", names,
                        ResourceType.ODC_PROJECT.getLocalizedMessage()},
                errorMessage);

    }

    private void checkDataSourceReference(Long id, Long organizationId, AuditEventAction action) {
        List<ConnectionConfig> dataSources = connectionService.listByOrganizationIdAndEnvironmentId(organizationId, id);
        if (!CollectionUtils.isEmpty(dataSources)) {
            throw new BadRequestException(ErrorCodes.CannotOperateDueReference,
                    new Object[] {
                            action.getLocalizedMessage(),
                            ResourceType.ODC_ENVIRONMENT.getLocalizedMessage(), "name",
                            String.join(dataSources.stream().map(ConnectionConfig::getName)
                                    .collect(Collectors.joining(", "))),
                            ResourceType.ODC_CONNECTION.getLocalizedMessage()},
                    "cannot disable the environment due to referenced by some data sources");
        }

    }

    private void checkRiskLevelReference(Long id, String name, Long organizationId, AuditEventAction action) {
        Set<RiskDetectRule> referencedRiskDetectRules =
                riskDetectService.listAllByOrganizationId(organizationId)
                        .stream()
                        .filter(rule -> rule.getRootNode().find(ConditionExpression.ENVIRONMENT_ID.name(), id)
                                || rule.getRootNode().find(ConditionExpression.ENVIRONMENT_NAME.name(), name))
                        .collect(Collectors.toSet());

        if (!referencedRiskDetectRules.isEmpty()) {
            String riskLevelNames = referencedRiskDetectRules.stream().map(rule -> I18n.translate(
                    StringUtils.substring(rule.getRiskLevel().getName(), 2, rule.getRiskLevel().getName().length() - 1),
                    null, rule.getRiskLevel().getName(), LocaleContextHolder.getLocale()))
                    .collect(Collectors.joining(", "));

            throw new BadRequestException(ErrorCodes.CannotOperateDueReference,
                    new Object[] {
                            action.getLocalizedMessage(),
                            ResourceType.ODC_ENVIRONMENT.getLocalizedMessage(), "name",
                            riskLevelNames,
                            ResourceType.ODC_RISK_LEVEL.getLocalizedMessage()},
                    "cannot delete the environment due to referenced by some risk level");
        }
    }

}
