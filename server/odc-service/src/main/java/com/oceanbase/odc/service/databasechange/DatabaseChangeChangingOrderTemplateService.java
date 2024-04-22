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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderResp;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

@Service
@Validated
public class DatabaseChangeChangingOrderTemplateService {
    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository databaseChangeChangingOrderTemplateRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;

    @Transactional
    public Boolean createDatabaseChangingOrderTemplate(
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        invalidPermission(req);
        if (databaseChangeChangingOrderTemplateRepository.existsByNameAndProjectId(req.getName(), req.getProjectId())) {
            throw new IllegalArgumentException("The name '"+ req.getName() +"' has been used by another template. Please change the name");
        }
        long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                new DatabaseChangeChangingOrderTemplateEntity();
        databaseChangeChangingOrderTemplateEntity.setName(req.getName());
        databaseChangeChangingOrderTemplateEntity.setCreatorId(userId);
        databaseChangeChangingOrderTemplateEntity.setProjectId(req.getProjectId());
        databaseChangeChangingOrderTemplateEntity.setOrganizationId(organizationId);
        databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(JsonUtils.toJson(req.getOrders()));
        databaseChangeChangingOrderTemplateRepository.save(
                databaseChangeChangingOrderTemplateEntity);
        return true;
    }

    @Transactional
    public Boolean modifyDatabaseChangingOrderTemplate(Long id,
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        invalidPermission(req);
        if (!databaseChangeChangingOrderTemplateRepository.existsById(id)) {
            throw new IllegalArgumentException("the current template doesn't exist");
        }
        if (databaseChangeChangingOrderTemplateRepository.existsByNameAndProjectId(req.getName(), req.getProjectId())) {
            throw new IllegalArgumentException("The name '"+ req.getName() +"' has been used by another template. Please change the name");
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
        databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(JsonUtils.toJson(req.getOrders()));
        databaseChangeChangingOrderTemplateRepository.save(databaseChangeChangingOrderTemplateEntity);
        return true;
    }

    public QueryDatabaseChangeChangingOrderResp queryDatabaseChangingOrderTemplateById(
            @NotNull @Min(value = 0) Long id) {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                databaseChangeChangingOrderTemplateRepository.findById(id).orElseThrow(
                        () -> new NoSuchElementException("the template does not exist"));
        projectPermissionValidator.checkProjectRole(databaseChangeChangingOrderTemplateEntity.getProjectId(), ResourceRoleName.all());
        String databaseSequencesJson = databaseChangeChangingOrderTemplateEntity.getDatabaseSequences();
        List<List<Long>> databaseSequences = JsonUtils.fromJson(databaseSequencesJson, new TypeReference<List<List<Long>>>() {});
        List<Long> ids = databaseSequences.stream().flatMap(x -> x.stream()).distinct().collect(
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


    public Page<QueryDatabaseChangeChangingOrderResp> listDatabaseChangingOrderTemplates(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        Page<DatabaseChangeChangingOrderTemplateEntity> pageByCreatorId =
                databaseChangeChangingOrderTemplateRepository.findByProjectId(
                        authenticationFacade.currentUserId(), pageable);
        List<DatabaseChangeChangingOrderTemplateEntity> content = pageByCreatorId.getContent();
        List<QueryDatabaseChangeChangingOrderResp> queryDatabaseChangeChangineOrderResps = new ArrayList<>();
        for (DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity : content) {
            QueryDatabaseChangeChangingOrderResp queryDatabaseChangeChangineOrderResp =
                    new QueryDatabaseChangeChangingOrderResp();
            queryDatabaseChangeChangineOrderResp.setId(databaseChangeChangingOrderTemplateEntity.getId());
            queryDatabaseChangeChangineOrderResp.setName(databaseChangeChangingOrderTemplateEntity.getName());
            queryDatabaseChangeChangineOrderResps.add(queryDatabaseChangeChangineOrderResp);
        }
        Page<QueryDatabaseChangeChangingOrderResp> page = new PageImpl<>(
                queryDatabaseChangeChangineOrderResps, pageable, pageByCreatorId.getTotalElements());
        return page;
    }

    @Transactional
    public Boolean deleteDatabaseChangingOrderTemplateById(@NotNull @Min(value = 0) Long id) {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
            databaseChangeChangingOrderTemplateRepository.findById(id).orElseThrow(
                () -> new NoSuchElementException("the template does not exist"));
        projectPermissionValidator.checkProjectRole(databaseChangeChangingOrderTemplateEntity.getProjectId(), ResourceRoleName.all());
        databaseChangeChangingOrderTemplateRepository.deleteByIdAndProjectId(id, authenticationFacade.currentUserId());
        return true;
    }

    private void invalidPermission(CreateDatabaseChangeChangingOrderReq req) {
        // project是否存在
        if (projectRepository.existsById(req.getProjectId()) == false) {
            throw new NotFoundException(ResourceType.ODC_PROJECT, "id", req.getProjectId());
        }
        // 当前用户是否有权访问此项目
        projectPermissionValidator.checkProjectRole(req.getProjectId(), ResourceRoleName.all());
        // 模板中数据库是否都属于此项目
        Set<Long> ids = req.getOrders().stream().flatMap(x -> x.stream()).collect(Collectors.toSet());
        List<DatabaseEntity> byIdIn = databaseRepository.findByIdIn(ids);
        if ((byIdIn.stream().allMatch(x -> x.getProjectId() == req.getProjectId())) == false) {
            throw new IllegalArgumentException("all databases must belong to the current project");
        }
    }

}
