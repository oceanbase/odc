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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.collaboration.ProjectRepository;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateEntity;
import com.oceanbase.odc.metadb.databasechange.DatabaseChangeChangingOrderTemplateRepository;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderTemplateReq;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeChangingOrderTemplateResp;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingOrderTemplateExists;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderParams;
import com.oceanbase.odc.service.databasechange.model.UpdateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @author: zijia.cj
 * @date: 2024/4/23
 */
public class DatabaseChangeChangingOrderTemplateServiceTest extends ServiceTestEnv {

    private static final Long PROJECT_ID = 1L;
    private static final Long CURRENT_USER_ID = 1L;

    private static final Long ORGANIZATION_ID = 1L;
    private static final String TEMPLATE_NAME = "template";
    private static final String TEMPLATE_RENAME = "template_rename";

    @Autowired
    private DatabaseChangeChangingOrderTemplateService templateService;
    @Autowired
    private DatabaseChangeChangingOrderTemplateRepository templateRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private DatabaseRepository databaseRepository;
    @MockBean
    private ProjectRepository projectRepository;
    @MockBean
    private ProjectPermissionValidator projectPermissionValidator;
    @MockBean
    private DatabaseService databaseService;

    @Before
    public void setUp() {
        templateRepository.deleteAll();
        when(authenticationFacade.currentUserId()).thenReturn(CURRENT_USER_ID);
        when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(projectRepository.existsById(any())).thenReturn(true);
        when(projectPermissionValidator.hasProjectRole(anyLong(), any())).thenReturn(true);
    }

    @After
    public void clear() {
        templateRepository.deleteAll();
    }

    @Test
    public void createDatabaseChangingOrderTemplate_saveEntity_succeed() {
        CreateDatabaseChangeChangingOrderTemplateReq req = new CreateDatabaseChangeChangingOrderTemplateReq();
        req.setProjectId(PROJECT_ID);
        req.setName(TEMPLATE_NAME);
        List<List<Long>> orders = new ArrayList<>();
        orders.add(Arrays.asList(1L, 2L));
        orders.add(Arrays.asList(3L, 4L));
        req.setOrders(orders);
        List<DatabaseEntity> databaseEntities = new ArrayList<>();
        DatabaseEntity databaseEntity = new DatabaseEntity();
        databaseEntity.setProjectId(PROJECT_ID);
        orders.stream().flatMap(Collection::stream).forEach(x -> databaseEntities.add(databaseEntity));
        when(databaseRepository.findByIdIn(any())).thenReturn(databaseEntities);
        DatabaseChangeChangingOrderTemplateResp templateResp = templateService.create(
                req);
        int size = templateRepository.findAll().size();
        Assert.assertEquals(TEMPLATE_NAME, templateResp.getName());
        Assert.assertEquals(PROJECT_ID, templateResp.getProjectId());
        Assert.assertEquals(1, size);
    }

    @Test(expected = BadRequestException.class)
    public void createDatabaseChangingOrderTemplate_tempaltNameIsDuplicate_throwIllegalArgumentException() {
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
    }


    @Test(expected = NotFoundException.class)
    public void createDatabaseChangingOrderTemplate_projectIsNotExist_throwIllegalArgumentException() {
        CreateDatabaseChangeChangingOrderTemplateReq req = new CreateDatabaseChangeChangingOrderTemplateReq();
        req.setProjectId(PROJECT_ID);
        req.setName(TEMPLATE_NAME);
        List<List<Long>> orders = new ArrayList<>();
        List<Long> list = Arrays.asList(1L, 2L);
        orders.add(list);
        req.setOrders(orders);
        when(projectRepository.existsById(any())).thenReturn(false);
        templateService.create(req);
    }

    @Test(expected = BadArgumentException.class)
    public void createDatabaseChangingOrderTemplate_databaseNotBelongToProject_throwIllegalArgumentException() {
        CreateDatabaseChangeChangingOrderTemplateReq req = new CreateDatabaseChangeChangingOrderTemplateReq();
        req.setProjectId(PROJECT_ID);
        req.setName(TEMPLATE_NAME);
        List<List<Long>> orders = new ArrayList<>();
        List<Long> list = Arrays.asList(1L, 2L);
        orders.add(list);
        req.setOrders(orders);
        List<DatabaseEntity> databases = new ArrayList<>();
        DatabaseEntity database = new DatabaseEntity();
        database.setProjectId(2L);
        databases.add(database);
        when(projectRepository.existsById(any())).thenReturn(true);
        when(databaseRepository.findByIdIn(any())).thenReturn(databases);
        templateService.create(req);
    }

    @Test
    public void modifyDatabaseChangingOrderTemplate_modifyTemplate_succeed() {
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        DatabaseChangeChangingOrderTemplateEntity byNameAndProjectId =
                templateRepository.findByNameAndProjectId(TEMPLATE_NAME, PROJECT_ID).get();
        UpdateDatabaseChangeChangingOrderReq req = new UpdateDatabaseChangeChangingOrderReq();
        req.setProjectId(PROJECT_ID);
        req.setName(TEMPLATE_RENAME);
        List<List<Long>> orders = new ArrayList<>();
        orders.add(Arrays.asList(1L, 2L));
        orders.add(Arrays.asList(3L, 4L));
        req.setOrders(orders);
        DatabaseChangeChangingOrderTemplateResp update = templateService
                .update(byNameAndProjectId.getId(), req);
        assertEquals(TEMPLATE_RENAME, update.getName());
        Optional<DatabaseChangeChangingOrderTemplateEntity> byId =
                templateRepository.findById(byNameAndProjectId.getId());
        assertEquals(TEMPLATE_RENAME, byId.get().getName());

    }

    @Test(expected = NotFoundException.class)
    public void modifyDatabaseChangingOrderTemplate_notFoundTemplate_throwIllegalArgumentException() {
        UpdateDatabaseChangeChangingOrderReq req = new UpdateDatabaseChangeChangingOrderReq();
        req.setProjectId(PROJECT_ID);
        req.setName(TEMPLATE_RENAME);
        List<List<Long>> orders = new ArrayList<>();
        List<Long> list = Arrays.asList(1L, 2L);
        orders.add(list);
        req.setOrders(orders);
        when(authenticationFacade.currentUserId()).thenReturn(1L);
        when(authenticationFacade.currentOrganizationId()).thenReturn(1L);
        templateService.update(1L, req);
    }

    @Test
    public void modifyDatabaseChangingOrderTemplate_projectNotExists_throwNotFoundException() {
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        DatabaseChangeChangingOrderTemplateEntity byNameAndProjectId =
                templateRepository.findByNameAndProjectId(TEMPLATE_NAME, PROJECT_ID).get();
        UpdateDatabaseChangeChangingOrderReq req = new UpdateDatabaseChangeChangingOrderReq();
        req.setName(TEMPLATE_RENAME);
        req.setProjectId(2L);
        List<List<Long>> orders = new ArrayList<>();
        List<Long> list = Arrays.asList(1L, 2L);
        orders.add(list);
        req.setOrders(orders);
        when(projectRepository.existsById(2L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> {
            templateService.update(byNameAndProjectId.getId(),
                    req);
        });
    }

    @Test
    public void queryDatabaseChangingOrderTemplateById_findExistingTemplate_succeed() {
        Database database = new Database();
        when(databaseService.listDatabasesByIds(any())).thenReturn(Arrays.asList(database));
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        DatabaseChangeChangingOrderTemplateEntity byNameAndProjectId =
                templateRepository.findByNameAndProjectId(TEMPLATE_NAME, PROJECT_ID).get();
        DatabaseEntity databaseEntity = new DatabaseEntity();
        databaseEntity.setProjectId(PROJECT_ID);
        when(databaseRepository.findByIdIn(anyList())).thenReturn(Arrays.asList(databaseEntity));
        DatabaseChangeChangingOrderTemplateResp databaseChangeChangingOrderTemplateResp =
                templateService.detail(
                        byNameAndProjectId.getId());
        assertEquals(PROJECT_ID, databaseChangeChangingOrderTemplateResp.getProjectId());
        assertEquals(TEMPLATE_NAME, databaseChangeChangingOrderTemplateResp.getName());
        assertEquals(PROJECT_ID, databaseChangeChangingOrderTemplateResp.getProjectId());

    }

    @Test
    public void listDatabaseChangingOrderTemplates_useQueryCondition_succeed() {
        Database database = new Database();
        when(databaseService.listDatabasesByIds(any())).thenReturn(Arrays.asList(database));
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        Pageable pageable = Pageable.unpaged();
        QueryDatabaseChangeChangingOrderParams params = QueryDatabaseChangeChangingOrderParams.builder()
                .projectId(PROJECT_ID).creatorId(CURRENT_USER_ID).name(TEMPLATE_NAME).build();
        Page<DatabaseChangeChangingOrderTemplateResp> result =
                templateService.listTemplates(pageable, params);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getContent().size());
    }


    @Test
    public void deleteDatabaseChangingOrderTemplateById_deleteExistingTemplate_succeed() {
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        DatabaseChangeChangingOrderTemplateEntity databaseChangeChangingOrderTemplateEntity =
                templateRepository.findByNameAndProjectId(TEMPLATE_NAME, PROJECT_ID).get();
        DatabaseChangeChangingOrderTemplateResp delete = templateService.delete(
                databaseChangeChangingOrderTemplateEntity.getId());
        int size = templateRepository.findAll().size();
        assertEquals(0, size);
    }

    @Test
    public void exists_checkTemplateExist_true() {
        createDatabaseChangingOrderTemplate_saveEntity_succeed();
        DatabaseChangingOrderTemplateExists exists = templateService.exists(TEMPLATE_NAME, PROJECT_ID);
        assertEquals(true, exists.getExists());
    }
}


