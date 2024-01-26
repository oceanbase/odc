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
package com.oceanbase.odc.service.collaboration.environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentEntity;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.service.collaboration.environment.model.CreateEnvironmentReq;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.environment.model.UpdateEnvironmentReq;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigService.ApprovalFlowConfigDeleteEvent;
import com.oceanbase.odc.service.regulation.risklevel.RiskDetectService;
import com.oceanbase.odc.service.regulation.risklevel.model.ConditionExpression;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskDetectRule;
import com.oceanbase.odc.service.regulation.ruleset.RuleService;
import com.oceanbase.odc.service.regulation.ruleset.RulesetService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;
import com.oceanbase.odc.service.regulation.ruleset.model.Ruleset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/5/24 11:33
 * @Description: []
 */
@Service
@Authenticated
public class EnvironmentService {
    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    private EnvironmentMapper environmentMapper = EnvironmentMapper.INSTANCE;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @Autowired
    private RulesetService rulesetService;

    @Autowired
    private RuleService ruleService;

    private final List<Consumer<EnvironmentDeleteEvent>> preDeleteHooks = new ArrayList<>();
    private final List<Consumer<EnvironmentDisableEvent>> preDisableHooks = new ArrayList<>();



    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_ENVIRONMENT", isForAll = true)
    public Environment create(@NotNull CreateEnvironmentReq req) {
        PreConditions.validNoDuplicated(ResourceType.ODC_ENVIRONMENT, "name", req.getName(),
                () -> exists(req.getName()));

        Ruleset saveedRuleset = rulesetService.create(buildRuleset(req.getName(), req.getDescription()));
        List<Rule> copiedRules = ruleService.list(req.getCopiedRulesetId(), new QueryRuleMetadataParams());
        ruleService.create(saveedRuleset.getId(), copiedRules);
        return entityToModel(environmentRepository.save(buildEnvironmentEntity(req, saveedRuleset.getId())));
    }


    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("Internal authenticated")
    public List<Environment> list() {
        return list(authenticationFacade.currentOrganizationId());
    }

    @SkipAuthorize("Internal usage")
    @Transactional(rollbackFor = Exception.class)
    public List<Environment> list(Long organizationId) {
        return environmentRepository.findByOrganizationId(organizationId)
                .stream().map(this::entityToModel).collect(Collectors.toList());
    }

    @SkipAuthorize("Internal usage")
    public boolean exists(@NonNull Long id) {
        return exists(id, authenticationFacade.currentOrganizationId());
    }

    @SkipAuthorize("Internal usage")
    public boolean exists(@NonNull Long id, @NonNull Long organizationId) {
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.setId(id);
        entity.setOrganizationId(organizationId);
        return environmentRepository.exists(Example.of(entity));
    }

    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("Internal usage")
    public Environment detail(@NonNull Long id) {
        return innerDetail(id);
    }

    @SkipAuthorize("odc internal usage")
    public Environment detailSkipPermissionCheck(@NonNull Long id) {
        return innerDetailWithoutPermissionCheck(id);
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_ENVIRONMENT", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Environment update(@NotNull Long id, @NotNull UpdateEnvironmentReq req) {
        Environment environment = innerDetail(id);
        if (environment.getBuiltIn()) {
            throw new BadRequestException("Not allowed to update builtin environments");
        }
        environment.setDescription(req.getDescription());
        environment.setStyle(req.getStyle());
        environment.setLastModifier(InnerUser.of(authenticationFacade.currentUserId(), null, null));
        return entityToModel(environmentRepository.save(environmentMapper.modelToEntity(environment)));
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_ENVIRONMENT", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Boolean setEnabled(@NotNull Long id, @NotNull SetEnabledReq req) {
        /**
         * ensure that the environment is not referenced by any data source before disabling it
         */
        if (!req.getEnabled()) {
            for(Consumer<EnvironmentDisableEvent> hook : preDisableHooks) {
                hook.accept(new EnvironmentDisableEvent(id, authenticationFacade.currentOrganizationId()));
            }
        }
        return environmentRepository.updateEnabledById(id, req.getEnabled()) > 0;
    }

    @PreAuthenticate(actions = "delete", resourceType = "ODC_ENVIRONMENT", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Environment delete(@NotNull Long id) {
        for (Consumer<EnvironmentDeleteEvent> hook : preDeleteHooks) {
            hook.accept(new EnvironmentDeleteEvent(id, authenticationFacade.currentOrganizationId()));
        }
        Environment environment = innerDetail(id);
        if (environment.getBuiltIn()) {
            throw new BadRequestException("Not allowed to delete builtin environments");
        }
        environmentRepository.deleteById(id);
        rulesetService.delete(environment.getRulesetId());
        return environment;
    }

    @SkipAuthorize("odc internal usage")
    public void addDeleteHook(Consumer<EnvironmentDeleteEvent> hook) {
        preDeleteHooks.add(hook);
    }

    @SkipAuthorize("odc internal usage")
    public void addDisableHook(Consumer<EnvironmentDisableEvent> hook) {
        preDisableHooks.add(hook);
    }


    private boolean exists(String name) {
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setName(name);
        return environmentRepository.exists(Example.of(entity));
    }

    private Environment innerDetail(@NonNull Long id) {
        EnvironmentEntity entity = environmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_ENVIRONMENT, "id", id));
        Environment model = entityToModel(entity);
        permissionValidator.checkCurrentOrganization(model);
        return model;
    }

    private Environment innerDetailWithoutPermissionCheck(@NonNull Long id) {
        return entityToModel(environmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_ENVIRONMENT, "id", id)));
    }

    private Environment entityToModel(@NonNull EnvironmentEntity entity) {
        return environmentMapper.entityToModel(entity);
    }

    private Ruleset buildRuleset(String name, String description) {
        Long organizationId = authenticationFacade.currentOrganizationId();
        Long userId = authenticationFacade.currentUserId();
        Ruleset ruleset = new Ruleset();
        ruleset.setName(name);
        ruleset.setDescription(description);
        ruleset.setBuiltin(false);
        ruleset.setOrganizationId(organizationId);
        ruleset.setCreator(InnerUser.of(userId, null, null));
        ruleset.setLastModifier(InnerUser.of(userId, null, null));
        return ruleset;
    }

    private EnvironmentEntity buildEnvironmentEntity(CreateEnvironmentReq req, Long rulesetId) {
        EnvironmentEntity environment = new EnvironmentEntity();
        environment.setOrganizationId(authenticationFacade.currentOrganizationId());
        environment.setName(req.getName());
        environment.setDescription(req.getDescription());
        environment.setStyle(req.getStyle());
        environment.setRulesetId(rulesetId);
        environment.setEnabled(req.getEnabled());
        environment.setBuiltin(false);
        environment.setCreatorId(authenticationFacade.currentUserId());
        environment.setLastModifierId(authenticationFacade.currentUserId());
        return environment;
    }

    @Data
    @AllArgsConstructor
    public static class EnvironmentDeleteEvent {
        private Long id;
        private Long organizationId;
    }

    @Data
    @AllArgsConstructor
    public static class EnvironmentDisableEvent {
        private Long id;
        private Long organizationId;
    }

}
