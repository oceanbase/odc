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
package com.oceanbase.odc.service.databasechange;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateSpecs;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeProperties;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingOrderTemplateExists;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderParams;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderResp;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

@Service
@Validated
public class DatabaseChangeChangingOrderTemplateService {
    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository templateRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Transactional(rollbackFor = Exception.class)
    public Boolean create(
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        List<List<Long>> orders = req.getOrders();
        List<Long> databaseIds = orders.stream().flatMap(List::stream).collect(Collectors.toList());
        if (databaseIds.size() <= DatabaseChangeProperties.MIN_DATABASE_COUNT
                || databaseIds.size() > DatabaseChangeProperties.MAX_DATABASE_COUNT) {
            throw new BadArgumentException(ErrorCodes.IllegalArgument,
                    "The number of databases must be greater than " + DatabaseChangeProperties.MIN_DATABASE_COUNT
                            + " and not more than " + DatabaseChangeProperties.MAX_DATABASE_COUNT + ".");
        }
        if (new HashSet<Long>(databaseIds).size() != databaseIds.size()) {
            throw new BadArgumentException(ErrorCodes.IllegalArgument,
                    "Database cannot be duplicated.");
        }
        validPermission(req);
        PreConditions.validNoDuplicated(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "name", req.getName(),
                () -> templateRepository.existsByNameAndProjectId(req.getName(), req.getProjectId()));
        long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                new DatabaseChangeChangingOrderTemplateEntity();
        databaseChangeChangingOrderTemplateEntity.setName(req.getName());
        databaseChangeChangingOrderTemplateEntity.setCreatorId(userId);
        databaseChangeChangingOrderTemplateEntity.setProjectId(req.getProjectId());
        databaseChangeChangingOrderTemplateEntity.setOrganizationId(organizationId);
        databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(req.getOrders());
        templateRepository.save(
                databaseChangeChangingOrderTemplateEntity);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id,
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        validPermission(req);
        if (!templateRepository.existsById(id)) {
            throw new NotFoundException(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "id", id);
        }
        long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                new DatabaseChangeChangingOrderTemplateEntity();
        databaseChangeChangingOrderTemplateEntity.setId(id);
        databaseChangeChangingOrderTemplateEntity.setName(req.getName());
        databaseChangeChangingOrderTemplateEntity.setCreatorId(userId);
        databaseChangeChangingOrderTemplateEntity.setProjectId(req.getProjectId());
        databaseChangeChangingOrderTemplateEntity.setOrganizationId(organizationId);
        databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(req.getOrders());
        templateRepository.save(databaseChangeChangingOrderTemplateEntity);
        return true;
    }

    public QueryDatabaseChangeChangingOrderResp detail(
            @NotNull Long id) {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                templateRepository.findById(id).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "id", id));
        projectPermissionValidator.checkProjectRole(databaseChangeChangingOrderTemplateEntity.getProjectId(),
                ResourceRoleName.all());
        List<List<Long>> databaseSequences = databaseChangeChangingOrderTemplateEntity.getDatabaseSequences();
        List<Long> ids = databaseSequences.stream().flatMap(Collection::stream).distinct().collect(
                Collectors.toList());
        List<DatabaseEntity> byIdIn = databaseRepository.findByIdIn(ids);
        Map<Long, DatabaseEntity> map = byIdIn.stream().collect(Collectors.toMap(x -> x.getId(), x -> x));
        List<List<DatabaseEntity>> databaseSequenceList = databaseSequences.stream()
                .map(sequence -> sequence.stream()
                        .map(x -> map.get(x))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
        QueryDatabaseChangeChangingOrderResp queryDatabaseChangeChangingOrderResp =
                new QueryDatabaseChangeChangingOrderResp();
        queryDatabaseChangeChangingOrderResp.setId(databaseChangeChangingOrderTemplateEntity.getId());
        queryDatabaseChangeChangingOrderResp.setName(databaseChangeChangingOrderTemplateEntity.getName());
        queryDatabaseChangeChangingOrderResp
                .setCreatorId(databaseChangeChangingOrderTemplateEntity.getCreatorId());
        queryDatabaseChangeChangingOrderResp.setProjectId(databaseChangeChangingOrderTemplateEntity.getProjectId());
        queryDatabaseChangeChangingOrderResp
                .setOrganizationId(databaseChangeChangingOrderTemplateEntity.getOrganizationId());
        queryDatabaseChangeChangingOrderResp.setDatabaseSequenceList(databaseSequenceList);
        return queryDatabaseChangeChangingOrderResp;
    }


    public Page<DatabaseChangeChangingOrderTemplateEntity> listTemplates(
            @NotNull Pageable pageable,
            @NotNull @Valid QueryDatabaseChangeChangingOrderParams params) {
        projectPermissionValidator.checkProjectRole(params.getProjectId(), ResourceRoleName.all());
        Specification<DatabaseChangeChangingOrderTemplateEntity> specification = Specification
                .where(DatabaseChangeChangingOrderTemplateSpecs.projectIdEquals(params.getProjectId()))
                .and(params.getName() == null ? null
                        : DatabaseChangeChangingOrderTemplateSpecs.nameLikes(params.getName()))
                .and(params.getCreatorId() == null ? null
                        : DatabaseChangeChangingOrderTemplateSpecs
                                .creatorIdIn(Collections.singleton(params.getCreatorId())));
        return templateRepository.findAll(specification, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(@NotNull @Min(value = 1) Long id) {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                templateRepository.findById(id).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "id", id));
        projectPermissionValidator.checkProjectRole(databaseChangeChangingOrderTemplateEntity.getProjectId(),
                ResourceRoleName.all());
        templateRepository.deleteById(id);
        return true;
    }

    public DatabaseChangingOrderTemplateExists exists(String name, Long projectId) {
        if (templateRepository.existsByNameAndProjectId(name, projectId)) {
            return DatabaseChangingOrderTemplateExists
                    .builder().exists(true).errorMessage(ErrorCodes.DuplicatedExists.getLocalizedMessage(
                            new Object[] {ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE.getLocalizedMessage(), "name",
                                    name}))
                    .build();
        }
        return DatabaseChangingOrderTemplateExists.builder().exists(false).build();
    }

    private void validPermission(CreateDatabaseChangeChangingOrderReq req) {
        PreConditions.validExists(ResourceType.ODC_PROJECT, "projectId", req.getProjectId(),
                () -> projectRepository.existsById(req.getProjectId()));
        projectPermissionValidator.checkProjectRole(req.getProjectId(), ResourceRoleName.all());
        List<Long> list = req.getOrders().stream().flatMap(x -> x.stream()).collect(Collectors.toList());
        HashSet<Long> ids = new HashSet<>(list);
        List<DatabaseEntity> byIdIn = databaseRepository.findByIdIn(ids);
        if (!(byIdIn.stream().allMatch(x -> x.getProjectId() == req.getProjectId()))) {
            throw new BadArgumentException(ErrorCodes.IllegalArgument,
                    "all databases must belong to the current project");
        }
    }

}
