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
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectEntity;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateSpecs;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderTemplateReq;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeChangingOrderTemplateResp;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeProperties;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingOrderTemplateExists;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderParams;
import com.oceanbase.odc.service.databasechange.model.UpdateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

@Service
@Validated
@SkipAuthorize("permission check inside")
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
    private DatabaseService databaseService;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;
    @Autowired
    private DatabaseChangeProperties databaseChangeProperties;

    @SkipAuthorize("internal usage")
    public Map<Long, Boolean> getChangingOrderTemplateId2EnableStatus(Set<Long> templateIds) {
        List<DatabaseChangeChangingOrderTemplateEntity> templates =
                this.templateRepository.findAllById(templateIds);
        Map<Long, List<DatabaseChangeChangingOrderTemplateEntity>> projectId2TemplateEntityList = templates.stream()
                .collect(Collectors.groupingBy(DatabaseChangeChangingOrderTemplateEntity::getProjectId));
        List<ProjectEntity> projectEntities = projectRepository.findByIdIn(projectId2TemplateEntityList.keySet());
        List<Long> archivedProjectIds = projectEntities.stream()
                .filter(p -> Boolean.TRUE.equals(p.getArchived()))
                .map(ProjectEntity::getId).collect(Collectors.toList());
        List<Long> disabledTemplateIds = projectId2TemplateEntityList.entrySet().stream()
                .filter(entry -> archivedProjectIds.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream()
                        .map(DatabaseChangeChangingOrderTemplateEntity::getId))
                .collect(Collectors.toList());

        List<Long> nonArchivedProjectIds = projectEntities.stream()
                .filter(p -> Boolean.FALSE.equals(p.getArchived()))
                .map(ProjectEntity::getId).collect(Collectors.toList());

        Map<Long, List<DatabaseEntity>> projectId2Databases = this.databaseRepository
                .findByProjectIdIn(nonArchivedProjectIds).stream()
                .collect(Collectors.groupingBy(DatabaseEntity::getProjectId));
        disabledTemplateIds.addAll(projectId2TemplateEntityList.entrySet().stream()
                // 留下未归档的projectId2TemplateEntityList
                .filter(entry -> nonArchivedProjectIds.contains(entry.getKey()))
                .flatMap(entry -> {
                    List<DatabaseEntity> databases = projectId2Databases.get(entry.getKey());
                    if (CollectionUtils.isEmpty(databases)) {
                        return entry.getValue().stream().map(DatabaseChangeChangingOrderTemplateEntity::getId);
                    }
                    Set<Long> dbIds = databases.stream().map(DatabaseEntity::getId).collect(Collectors.toSet());
                    return entry.getValue().stream().filter(en -> {
                        Set<Long> templateDbIds = en.getDatabaseSequences().stream()
                                .flatMap(Collection::stream).collect(Collectors.toSet());
                        return !CollectionUtils.containsAll(dbIds, templateDbIds);
                    }).map(DatabaseChangeChangingOrderTemplateEntity::getId);
                }).collect(Collectors.toList()));
        return templateIds.stream().collect(Collectors.toMap(id -> id, id -> !disabledTemplateIds.contains(id)));
    }

    @Transactional(rollbackFor = Exception.class)
    public DatabaseChangeChangingOrderTemplateResp create(
            @NotNull @Valid CreateDatabaseChangeChangingOrderTemplateReq req) {
        PreConditions.validExists(ResourceType.ODC_PROJECT, "projectId", req.getProjectId(),
                () -> projectRepository.existsById(req.getProjectId()));
        projectPermissionValidator.checkProjectRole(req.getProjectId(), ResourceRoleName.all());
        List<List<Long>> orders = req.getOrders();
        List<Long> databaseIds = orders.stream().flatMap(List::stream).collect(Collectors.toList());
        validateSizeAndNotDuplicated(databaseIds);
        PreConditions.validNoDuplicated(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "name", req.getName(),
                () -> templateRepository.existsByNameAndProjectId(req.getName(), req.getProjectId()));
        List<DatabaseEntity> databaseEntities = databaseRepository.findByIdIn(databaseIds);
        if (databaseEntities.size() < databaseIds.size()) {
            throw new BadArgumentException(ErrorCodes.BadArgument, "some of these databases do not exist");
        }
        if (!(databaseEntities.stream().allMatch(x -> x.getProjectId().equals(req.getProjectId())))) {
            throw new BadArgumentException(ErrorCodes.BadArgument,
                    "all databases must belong to the current project");
        }
        long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseChangeChangingOrderTemplateEntity templateEntity =
                new DatabaseChangeChangingOrderTemplateEntity();
        templateEntity.setName(req.getName());
        templateEntity.setCreatorId(userId);
        templateEntity.setProjectId(req.getProjectId());
        templateEntity.setOrganizationId(organizationId);
        templateEntity.setDatabaseSequences(req.getOrders());
        templateEntity.setEnabled(true);
        DatabaseChangeChangingOrderTemplateEntity savedEntity = templateRepository.saveAndFlush(templateEntity);
        DatabaseChangeChangingOrderTemplateResp templateResp = new DatabaseChangeChangingOrderTemplateResp();
        templateResp.setId(savedEntity.getId());
        templateResp.setName(savedEntity.getName());
        templateResp.setCreatorId(savedEntity.getCreatorId());
        templateResp.setProjectId(savedEntity.getProjectId());
        templateResp.setOrganizationId(savedEntity.getOrganizationId());
        List<List<Long>> databaseSequences = savedEntity.getDatabaseSequences();
        List<List<DatabaseChangeDatabase>> databaseSequenceList = databaseSequences.stream()
                .map(s -> s.stream().map(DatabaseChangeDatabase::new).collect(Collectors.toList()))
                .collect(Collectors.toList());
        templateResp.setDatabaseSequenceList(databaseSequenceList);
        templateResp.setEnabled(true);
        return templateResp;
    }

    public void validateSizeAndNotDuplicated(List<Long> databaseIds) {
        if (databaseIds.size() <= databaseChangeProperties.getMinDatabaseCount()
                || databaseIds.size() > databaseChangeProperties.getMaxDatabaseCount()) {
            throw new BadArgumentException(ErrorCodes.BadArgument,
                    String.format("The number of databases must be greater than %s and not more than %s.",
                            databaseChangeProperties.getMinDatabaseCount(),
                            databaseChangeProperties.getMaxDatabaseCount()));
        }
        if (new HashSet<Long>(databaseIds).size() != databaseIds.size()) {
            throw new BadArgumentException(ErrorCodes.BadArgument,
                    "Databases cannot be duplicated.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public DatabaseChangeChangingOrderTemplateResp update(Long id,
            @NotNull @Valid UpdateDatabaseChangeChangingOrderReq req) {
        PreConditions.validExists(ResourceType.ODC_PROJECT, "projectId", req.getProjectId(),
                () -> projectRepository.existsById(req.getProjectId()));
        projectPermissionValidator.checkProjectRole(req.getProjectId(), ResourceRoleName.all());
        DatabaseChangeChangingOrderTemplateEntity originEntity =
                templateRepository.findByIdAndProjectId(id, req.getProjectId()).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "id", id));
        PreConditions.validNoDuplicated(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "name", req.getName(),
                () -> templateRepository.existsByNameAndProjectId(req.getName(), req.getProjectId()));
        List<List<Long>> orders = req.getOrders();
        List<Long> databaseIds = orders.stream().flatMap(List::stream).collect(Collectors.toList());
        validateSizeAndNotDuplicated(databaseIds);
        PreConditions.validNoDuplicated(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "name", req.getName(),
                () -> templateRepository.existsByNameAndProjectId(req.getName(), req.getProjectId()));
        List<DatabaseEntity> databaseEntities = databaseRepository.findByIdIn(databaseIds);
        if (databaseEntities.size() < databaseIds.size()) {
            throw new BadArgumentException(ErrorCodes.BadArgument, "some of these databases do not exist");
        }
        if (!(databaseEntities.stream().allMatch(x -> x.getProjectId().equals(req.getProjectId())))) {
            throw new BadArgumentException(ErrorCodes.BadArgument,
                    "all databases must belong to the current project");
        }
        originEntity.setName(req.getName());
        originEntity.setDatabaseSequences(req.getOrders());
        DatabaseChangeChangingOrderTemplateEntity savedEntity = templateRepository.saveAndFlush(originEntity);
        DatabaseChangeChangingOrderTemplateResp templateResp = new DatabaseChangeChangingOrderTemplateResp();
        templateResp.setId(savedEntity.getId());
        templateResp.setName(savedEntity.getName());
        templateResp.setCreatorId(savedEntity.getCreatorId());
        templateResp.setProjectId(savedEntity.getProjectId());
        templateResp.setOrganizationId(savedEntity.getOrganizationId());
        List<List<DatabaseChangeDatabase>> databaseSequenceList = orders.stream()
                .map(s -> s.stream().map(DatabaseChangeDatabase::new).collect(Collectors.toList()))
                .collect(Collectors.toList());
        templateResp.setDatabaseSequenceList(databaseSequenceList);
        templateResp.setEnabled(true);
        return templateResp;
    }

    public DatabaseChangeChangingOrderTemplateResp detail(@NotNull Long id) {
        DatabaseChangeChangingOrderTemplateEntity templateEntity =
                templateRepository.findById(id).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "id", id));
        projectPermissionValidator.checkProjectRole(templateEntity.getProjectId(),
                ResourceRoleName.all());
        List<List<Long>> databaseSequences = templateEntity.getDatabaseSequences();
        DatabaseChangeChangingOrderTemplateResp templateResp =
                new DatabaseChangeChangingOrderTemplateResp();
        templateResp.setId(templateEntity.getId());
        templateResp.setName(templateEntity.getName());
        templateResp
                .setCreatorId(templateEntity.getCreatorId());
        templateResp.setProjectId(templateEntity.getProjectId());
        templateResp
                .setOrganizationId(templateEntity.getOrganizationId());
        List<List<DatabaseChangeDatabase>> databaseSequenceList = databaseSequences.stream()
                .map(s -> s.stream().map(DatabaseChangeDatabase::new).collect(Collectors.toList()))
                .collect(Collectors.toList());
        templateResp.setDatabaseSequenceList(databaseSequenceList);
        Map<Long, Boolean> templateId2Status = getChangingOrderTemplateId2EnableStatus(
                Collections.singleton(templateEntity.getId()));
        templateResp.setEnabled(templateId2Status.getOrDefault(templateEntity.getId(), templateEntity.getEnabled()));
        if (!templateResp.getEnabled()) {
            templateEntity.setEnabled(false);
            templateRepository.save(templateEntity);
        }
        return templateResp;
    }


    public Page<DatabaseChangeChangingOrderTemplateResp> listTemplates(@NotNull Pageable pageable,
            @NotNull @Valid QueryDatabaseChangeChangingOrderParams params) {
        projectPermissionValidator.checkProjectRole(params.getProjectId(), ResourceRoleName.all());
        Specification<DatabaseChangeChangingOrderTemplateEntity> specification = Specification
                .where(DatabaseChangeChangingOrderTemplateSpecs.projectIdEquals(params.getProjectId()))
                .and(params.getName() == null ? null
                        : DatabaseChangeChangingOrderTemplateSpecs.nameLikes(params.getName()))
                .and(params.getCreatorId() == null ? null
                        : DatabaseChangeChangingOrderTemplateSpecs
                                .creatorIdIn(Collections.singleton(params.getCreatorId())));
        Page<DatabaseChangeChangingOrderTemplateEntity> pageResult =
                templateRepository.findAll(specification, pageable);
        List<DatabaseChangeChangingOrderTemplateEntity> entityList = pageResult.getContent();
        List<DatabaseChangeChangingOrderTemplateResp> templateRespList = entityList.stream().map(entity -> {
            DatabaseChangeChangingOrderTemplateResp templateResp = new DatabaseChangeChangingOrderTemplateResp();
            templateResp.setId(entity.getId());
            templateResp.setName(entity.getName());
            templateResp.setCreatorId(entity.getCreatorId());
            templateResp.setProjectId(entity.getProjectId());
            templateResp.setOrganizationId(entity.getOrganizationId());
            templateResp.setEnabled(entity.getEnabled());
            return templateResp;
        }).collect(Collectors.toList());
        return new PageImpl<>(templateRespList, pageable, pageResult.getTotalElements());
    }

    @Transactional(rollbackFor = Exception.class)
    public DatabaseChangeChangingOrderTemplateResp delete(@NotNull Long id) {
        DatabaseChangeChangingOrderTemplateEntity templateEntity =
                templateRepository.findById(id).orElseThrow(
                        () -> new NotFoundException(ResourceType.ODC_DATABASE_CHANGE_ORDER_TEMPLATE, "id", id));
        projectPermissionValidator.checkProjectRole(templateEntity.getProjectId(),
                ResourceRoleName.all());
        templateRepository.deleteById(id);
        DatabaseChangeChangingOrderTemplateResp templateResp = new DatabaseChangeChangingOrderTemplateResp();
        templateResp.setId(templateEntity.getId());
        templateResp.setName(templateEntity.getName());
        templateResp.setCreatorId(templateEntity.getCreatorId());
        templateResp.setProjectId(templateEntity.getProjectId());
        templateResp.setOrganizationId(templateEntity.getOrganizationId());
        List<List<Long>> databaseSequences = templateEntity.getDatabaseSequences();
        List<List<DatabaseChangeDatabase>> databaseSequenceList = databaseSequences.stream()
                .map(s -> s.stream().map(DatabaseChangeDatabase::new).collect(Collectors.toList()))
                .collect(Collectors.toList());
        templateResp.setDatabaseSequenceList(databaseSequenceList);
        templateResp.setEnabled(templateEntity.getEnabled());
        return templateResp;
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

}
