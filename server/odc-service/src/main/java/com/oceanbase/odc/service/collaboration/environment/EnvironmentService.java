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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentEntity;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

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
    private UserService userService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    private EnvironmentMapper environmentMapper = EnvironmentMapper.INSTANCE;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

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
    public Environment update(@NonNull Long id, @NonNull Environment environment) {
        EnvironmentEntity previous = environmentRepository.findByIdAndOrganizationId(id,
                authenticationFacade.currentOrganizationId())
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_ENVIRONMENT, "id", id));

        previous.setDescription(environment.getDescription());
        /**
         * TODO: permission check for ruleset
         */
        previous.setRulesetId(environment.getRulesetId());
        previous.setLastModifierId(authenticationFacade.currentUserId());

        EnvironmentEntity saved = environmentRepository.save(previous);
        return entityToModel(saved);
    }

    @SkipAuthorize("internal usage")
    public Map<Long, List<Environment>> mapByIdIn(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return environmentRepository.findAllById(ids).stream().map(environmentMapper::entityToModel)
                .collect(Collectors.groupingBy(Environment::getId));
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
}
