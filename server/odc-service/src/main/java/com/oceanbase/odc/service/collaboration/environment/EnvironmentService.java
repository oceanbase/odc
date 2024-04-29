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
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentEntity;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.collaboration.EnvironmentSpecs;
import com.oceanbase.odc.service.collaboration.environment.model.CreateEnvironmentReq;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.environment.model.EnvironmentExists;
import com.oceanbase.odc.service.collaboration.environment.model.QueryEnvironmentParam;
import com.oceanbase.odc.service.collaboration.environment.model.UpdateEnvironmentReq;
import com.oceanbase.odc.service.common.model.InnerUser;
import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
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
    private final Set<String> DEFAULT_ENV_NAMES = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    @PostConstruct
    @SkipAuthorize("odc internal usage")
    public void init() {
        DEFAULT_ENV_NAMES.add("开发");
        DEFAULT_ENV_NAMES.add("测试");
        DEFAULT_ENV_NAMES.add("生产");
        DEFAULT_ENV_NAMES.add("默认");
        DEFAULT_ENV_NAMES.add("開發");
        DEFAULT_ENV_NAMES.add("測試");
        DEFAULT_ENV_NAMES.add("生產");
        DEFAULT_ENV_NAMES.add("默認");
        DEFAULT_ENV_NAMES.add("dev");
        DEFAULT_ENV_NAMES.add("sit");
        DEFAULT_ENV_NAMES.add("prod");
        DEFAULT_ENV_NAMES.add("default");
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_ENVIRONMENT", isForAll = true)
    public Environment create(@NotNull @Valid CreateEnvironmentReq req) {
        EnvironmentExists exists = exists(req.getName());
        if (exists.getExists()) {
            throw new BadRequestException(exists.getErrorMessage());
        }
        Ruleset savedRuleset = rulesetService.create(buildRuleset(req.getName(), req.getDescription()));
        List<Rule> copiedRules = ruleService.list(req.getCopiedRulesetId(), new QueryRuleMetadataParams());
        ruleService.create(savedRuleset.getId(), copiedRules);
        return entityToModel(environmentRepository.save(buildEnvironmentEntity(req, savedRuleset.getId())));
    }


    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("Internal authenticated")
    public List<Environment> list(QueryEnvironmentParam param) {
        return list(authenticationFacade.currentOrganizationId(), param);
    }

    @SkipAuthorize("Internal usage")
    @Transactional(rollbackFor = Exception.class)
    public List<Environment> list(Long organizationId, QueryEnvironmentParam param) {
        Specification<EnvironmentEntity> specs = EnvironmentSpecs.organizationIdEquals(organizationId)
                .and(EnvironmentSpecs.enabledEquals(param.getEnabled()));
        return environmentRepository.findAll(specs)
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

    @SkipAuthorize("odc internal usage")
    public List<Environment> detailSkipPermissionCheckForMultipleDatabase(@NotEmpty List<EnvironmentEntity> list) {
        return list.stream().map(environmentEntity -> entityToModel(environmentEntity)).collect(Collectors.toList());
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_ENVIRONMENT", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Environment update(@NotNull Long id, @NotNull @Valid UpdateEnvironmentReq req) {
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
    public Boolean setEnabled(@NotNull Long id, @NotNull @Valid SetEnabledReq req) {
        /**
         * ensure that the environment is not referenced by any data source before disabling it
         */
        if (!req.getEnabled()) {
            for (Consumer<EnvironmentDisableEvent> hook : preDisableHooks) {
                hook.accept(new EnvironmentDisableEvent(id, authenticationFacade.currentOrganizationId()));
            }
        }
        return environmentRepository.updateEnabledById(id, req.getEnabled()) > 0;
    }

    @PreAuthenticate(actions = "delete", resourceType = "ODC_ENVIRONMENT", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Environment delete(@NotNull Long id) {
        Environment environment = innerDetail(id);
        for (Consumer<EnvironmentDeleteEvent> hook : preDeleteHooks) {
            hook.accept(new EnvironmentDeleteEvent(id, environment.getName(),
                    authenticationFacade.currentOrganizationId()));
        }
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


    @SkipAuthorize("internally authenticated")
    public EnvironmentExists exists(String name) {
        if (DEFAULT_ENV_NAMES.contains(name)) {
            return EnvironmentExists.builder().exists(true)
                    .errorMessage(ErrorCodes.ReservedName.getLocalizedMessage(new Object[] {name})).build();
        }
        EnvironmentEntity entity = new EnvironmentEntity();
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setName(name);
        if (environmentRepository.exists(Example.of(entity))) {
            return EnvironmentExists.builder().exists(true)
                    .errorMessage(ErrorCodes.DuplicatedExists.getLocalizedMessage(
                            new Object[] {ResourceType.ODC_ENVIRONMENT.getLocalizedMessage(), "name", name}))
                    .build();
        }
        return EnvironmentExists.builder().exists(false).build();
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
        environment.setBuiltIn(false);
        environment.setCreatorId(authenticationFacade.currentUserId());
        environment.setLastModifierId(authenticationFacade.currentUserId());
        return environment;
    }

    @Data
    @AllArgsConstructor
    public static class EnvironmentDeleteEvent {
        private Long id;
        private String name;
        private Long organizationId;
    }

    @Data
    @AllArgsConstructor
    public static class EnvironmentDisableEvent {
        private Long id;
        private Long organizationId;
    }

}
