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
package com.oceanbase.odc.service.task.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.task.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.task.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.task.runtime.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.task.runtime.QueryDatabaseChangeChangineOrderResp;

@Service
@Validated
public class DatabaseChangeChangingOrderTemplateService {
    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository databaseChangeChangingOrderTemplateRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private DatabaseRepository databaseRepository;


    @Transactional
    public Boolean createOrModifyDatabaseTemplate(
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        // 根据id判断是创建还是修改
        Long id = req.getId();
        Long userId = authenticationFacade.currentUserId();
        Long organizationId = authenticationFacade.currentOrganizationId();
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                new DatabaseChangeChangingOrderTemplateEntity();
        QueryDatabaseChangeChangineOrderResp queryDatabaseChangeChangineOrderResp =
                new QueryDatabaseChangeChangineOrderResp();

        // 创建模板
        if (id == null) {
            // 判断当前用户下的模版是否已经采用此名称
            if (databaseChangeChangingOrderTemplateRepository.existsByNameAndCreatorId(req.getName(), userId)) {
                throw new RuntimeException("创建失败，当前模版名称已经存在");
            } ;
            databaseChangeChangingOrderTemplateEntity.setName(req.getName());
            databaseChangeChangingOrderTemplateEntity.setCreatorId(userId);
            databaseChangeChangingOrderTemplateEntity.setOrganizationId(organizationId);
            databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(JsonUtils.toJson(req.getOrders()));
            DatabaseChangeChangingOrderTemplateEntity createdDatabaseChangeChangingOrderTemplateEntity =
                    databaseChangeChangingOrderTemplateRepository.save(
                            databaseChangeChangingOrderTemplateEntity);
            return true;
        }
        // 修改模版
        // 判断需要修改的模板是否归属于当前用户
        databaseChangeChangingOrderTemplateRepository.findByIdAndCreatorId(id, userId)
                .orElseThrow(() -> new RuntimeException("修改失败，当前模板不属于您"));
        // 判断模板名称是否已经被其他记录使用
        Optional<List<DatabaseChangeChangingOrderTemplateEntity>> listByNameAndCreatorId =
                databaseChangeChangingOrderTemplateRepository.findByNameAndCreatorId(req.getName(), userId);
        if (listByNameAndCreatorId.isPresent()) {
            for (DatabaseChangeChangingOrderTemplateEntity changeChangingOrderTemplateEntity : listByNameAndCreatorId
                    .get()) {
                if (changeChangingOrderTemplateEntity.getId() != id) {
                    throw new RuntimeException("修改模板失败，当前模板名称已经存在");
                }
            }
        }
        databaseChangeChangingOrderTemplateEntity.setId(id);
        databaseChangeChangingOrderTemplateEntity.setName(req.getName());
        databaseChangeChangingOrderTemplateEntity.setCreatorId(userId);
        databaseChangeChangingOrderTemplateEntity.setOrganizationId(organizationId);
        databaseChangeChangingOrderTemplateEntity.setDatabaseSequences(JsonUtils.toJson(req.getOrders()));
        databaseChangeChangingOrderTemplateRepository.save(
                databaseChangeChangingOrderTemplateEntity);
        return true;
    }

    public QueryDatabaseChangeChangineOrderResp queryDatabaseTemplateById(@NotNull @Min(value = 0) Long id) {
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                databaseChangeChangingOrderTemplateRepository.findByIdAndCreatorId(id,
                        authenticationFacade.currentUserId()).orElseThrow(() -> new RuntimeException("无法查看，当前模板不属于您"));
        QueryDatabaseChangeChangineOrderResp queryDatabaseChangeChangineOrderResp =
                new QueryDatabaseChangeChangineOrderResp();
        queryDatabaseChangeChangineOrderResp.setId(databaseChangeChangingOrderTemplateEntity.getId());
        queryDatabaseChangeChangineOrderResp.setName(databaseChangeChangingOrderTemplateEntity.getName());
        queryDatabaseChangeChangineOrderResp
                .setCreatorId(databaseChangeChangingOrderTemplateEntity.getCreatorId());
        queryDatabaseChangeChangineOrderResp
                .setOrganizationId(databaseChangeChangingOrderTemplateEntity.getOrganizationId());
        // todo 封装数据库对象 [[1,2],[3,4]]
        List<List> orangeList = JsonUtils
                .fromJson(databaseChangeChangingOrderTemplateEntity.getDatabaseSequences(), List.class);
        ArrayList<List> targetList = new ArrayList<>();
        for (List list : orangeList) {
            ArrayList<DatabaseEntity> databaseEntities = new ArrayList<>();
            for (Object databaseId : list) {
                DatabaseEntity database = databaseRepository.findById(Long.valueOf(databaseId.toString())).orElseThrow(
                        () -> new RuntimeException("模板关联的数据库查找失败"));
                databaseEntities.add(database);
            }
            targetList.add(databaseEntities);
        }
        queryDatabaseChangeChangineOrderResp
                .setDatabaseSequenceList(targetList);
        return queryDatabaseChangeChangineOrderResp;
    }


    public Page<QueryDatabaseChangeChangineOrderResp> listDatabaseTemplate(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {

        Page<DatabaseChangeChangingOrderTemplateEntity> pageByCreatorId =
                databaseChangeChangingOrderTemplateRepository.findByCreatorId(
                        authenticationFacade.currentUserId(), pageable);
        List<DatabaseChangeChangingOrderTemplateEntity> content = pageByCreatorId.getContent();
        List<QueryDatabaseChangeChangineOrderResp> queryDatabaseChangeChangineOrderResps = new ArrayList<>();
        for (DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity : content) {
            QueryDatabaseChangeChangineOrderResp queryDatabaseChangeChangineOrderResp =
                    new QueryDatabaseChangeChangineOrderResp();
            queryDatabaseChangeChangineOrderResp.setId(databaseChangeChangingOrderTemplateEntity.getId());
            queryDatabaseChangeChangineOrderResp.setName(databaseChangeChangingOrderTemplateEntity.getName());
            queryDatabaseChangeChangineOrderResps.add(queryDatabaseChangeChangineOrderResp);
        }
        Page<QueryDatabaseChangeChangineOrderResp> page = new PageImpl<>(
                queryDatabaseChangeChangineOrderResps, pageable, pageByCreatorId.getTotalElements());
        return page;

    }

    @Transactional
    public Boolean deleteDatabseTemplateById(@NotNull @Min(value = 0) Long id) {
        if (!databaseChangeChangingOrderTemplateRepository.existsByIdAndCreatorId(id,
                authenticationFacade.currentUserId())) {
            throw new RuntimeException("删除失败，没有此模板");
        }
        databaseChangeChangingOrderTemplateRepository.deleteByIdAndCreatorId(id, authenticationFacade.currentUserId());
        return true;
    }

}
