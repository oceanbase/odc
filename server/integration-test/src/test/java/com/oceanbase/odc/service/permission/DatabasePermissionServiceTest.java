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
package com.oceanbase.odc.service.permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.util.TimeUtils;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.iam.PermissionEntity;
import com.oceanbase.odc.metadb.iam.PermissionRepository;
import com.oceanbase.odc.metadb.iam.UserDatabasePermissionRepository;
import com.oceanbase.odc.metadb.iam.UserPermissionEntity;
import com.oceanbase.odc.metadb.iam.UserPermissionRepository;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.connection.database.DatabaseMapper;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.database.DatabasePermissionService;
import com.oceanbase.odc.service.permission.database.model.CreateDatabasePermissionReq;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;
import com.oceanbase.odc.service.permission.database.model.QueryDatabasePermissionParams;
import com.oceanbase.odc.service.permission.database.model.UserDatabasePermission;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * @author gaoda.xy
 * @date 2024/1/12 14:35
 */
public class DatabasePermissionServiceTest extends ServiceTestEnv {

    @Autowired
    private DatabasePermissionService service;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired
    private ConnectionConfigRepository connectionConfigRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserDatabasePermissionRepository userDatabasePermissionRepository;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectPermissionValidator projectPermissionValidator;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    private List<Long> databaseIds;

    private static final Long PROJECT_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long ORGANIZATION_ID = 1L;
    private static final Long TICKET_ID = 1L;

    @Before
    public void setUp() {
        // Clear existing data
        connectionConfigRepository.deleteAll();
        databaseRepository.deleteAll();
        permissionRepository.deleteAll();
        userPermissionRepository.deleteAll();
        // Create test metadata
        ConnectionEntity c1 = createDataSource("c1");
        ConnectionEntity c2 = createDataSource("c2");
        DatabaseEntity d1 = createDatabase(c1.getId(), "d1");
        DatabaseEntity d2 = createDatabase(c2.getId(), "d2");
        this.databaseIds = Arrays.asList(d1.getId(), d2.getId());
        PermissionEntity p1 = createPermission(d1.getId(), "export", AuthorizationType.USER_AUTHORIZATION, null,
                DateUtils.addDays(new Date(), -3));
        PermissionEntity p2 = createPermission(d1.getId(), "export", AuthorizationType.USER_AUTHORIZATION, null,
                DateUtils.addDays(new Date(), 3));
        PermissionEntity p3 = createPermission(d1.getId(), "export", AuthorizationType.TICKET_APPLICATION, TICKET_ID,
                DateUtils.addDays(new Date(), 3));
        PermissionEntity p4 = createPermission(d2.getId(), "change", AuthorizationType.TICKET_APPLICATION, TICKET_ID,
                DateUtils.addDays(new Date(), 30));
        PermissionEntity p5 = createPermission(d2.getId(), "change", AuthorizationType.TICKET_APPLICATION, TICKET_ID,
                DateUtils.addDays(new Date(), 30));
        PermissionEntity p6 = createPermission(d2.getId(), "query", AuthorizationType.TICKET_APPLICATION, TICKET_ID + 1,
                TimeUtils.getMySQLMaxDatetime());
        createUserPermission(p1.getId());
        createUserPermission(p2.getId());
        createUserPermission(p3.getId());
        createUserPermission(p4.getId());
        createUserPermission(p5.getId());
        createUserPermission(p6.getId());
        // Mock
        Mockito.doNothing().when(projectPermissionValidator).checkProjectRole(Mockito.eq(PROJECT_ID), Mockito.any());
        Mockito.when(projectService.getMemberProjectIds(Mockito.eq(USER_ID)))
                .thenReturn(Collections.singleton(PROJECT_ID));
        Mockito.when(databaseService.listDatabasesByIds(Mockito.any())).thenReturn(
                Arrays.asList(DatabaseMapper.INSTANCE.entityToModel(d1), DatabaseMapper.INSTANCE.entityToModel(d2)));
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(USER_ID);
        Mockito.when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
    }

    @Test
    public void test_list_withNoParams() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder().build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(6, result.getTotalElements());
    }

    @Test
    public void test_list_withAllParams() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .userId(USER_ID)
                .ticketId(TICKET_ID)
                .fuzzyDatabaseName("d")
                .fuzzyDataSourceName("c")
                .types(DatabasePermissionType.all())
                .authorizationType(AuthorizationType.TICKET_APPLICATION)
                .statuses(ExpirationStatusFilter.all())
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(3, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_expired() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(Arrays.asList(ExpirationStatusFilter.EXPIRED))
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(1, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_expiring() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(Arrays.asList(ExpirationStatusFilter.EXPIRING))
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(2, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_notExpired() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(Arrays.asList(ExpirationStatusFilter.NOT_EXPIRED))
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(5, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_notExpiredAndExpiring() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(Arrays.asList(ExpirationStatusFilter.EXPIRED, ExpirationStatusFilter.EXPIRING))
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(3, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_expiredAndNotExpired() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(Arrays.asList(ExpirationStatusFilter.EXPIRED, ExpirationStatusFilter.NOT_EXPIRED))
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(6, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_expiringAndNotExpired() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(Arrays.asList(ExpirationStatusFilter.EXPIRING, ExpirationStatusFilter.NOT_EXPIRED))
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(5, result.getTotalElements());
    }

    @Test
    public void test_list_expireTime_expiredAndExpiringAndNotExpired() {
        QueryDatabasePermissionParams params = QueryDatabasePermissionParams.builder()
                .statuses(ExpirationStatusFilter.all())
                .build();
        Page<UserDatabasePermission> result = service.list(PROJECT_ID, params, Pageable.unpaged());
        Assert.assertEquals(6, result.getTotalElements());
    }

    @Test
    public void test_batchCreate() {
        permissionRepository.deleteAll();
        userPermissionRepository.deleteAll();
        CreateDatabasePermissionReq req = new CreateDatabasePermissionReq();
        req.setDatabaseIds(this.databaseIds);
        req.setExpireTime(DateUtils.addDays(new Date(), 3));
        req.setUserId(USER_ID);
        req.setTypes(Arrays.asList(DatabasePermissionType.EXPORT, DatabasePermissionType.CHANGE));
        List<UserDatabasePermission> result = service.batchCreate(PROJECT_ID, req);
        Assert.assertEquals(4, result.size());
        Assert.assertEquals(4, userDatabasePermissionRepository.findAll().size());
    }

    @Test
    public void test_batchInvoke() {
        List<UserDatabasePermission> exist = service
                .list(PROJECT_ID, QueryDatabasePermissionParams.builder().build(), Pageable.unpaged()).getContent();
        List<Long> ids = exist.stream().map(UserDatabasePermission::getId).collect(Collectors.toList());
        List<UserDatabasePermission> result = service.batchRevoke(PROJECT_ID, ids);
        Assert.assertEquals(6, exist.size());
        Assert.assertEquals(6, result.size());
        Assert.assertEquals(0, userDatabasePermissionRepository.findAll().size());
    }

    private ConnectionEntity createDataSource(String name) {
        ConnectionEntity entity = TestRandom.nextObject(ConnectionEntity.class);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setName(name);
        return connectionConfigRepository.saveAndFlush(entity);
    }

    private DatabaseEntity createDatabase(Long dataSourceId, String name) {
        DatabaseEntity entity = TestRandom.nextObject(DatabaseEntity.class);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setConnectionId(dataSourceId);
        entity.setProjectId(PROJECT_ID);
        entity.setName(name);
        return databaseRepository.saveAndFlush(entity);
    }

    private PermissionEntity createPermission(Long databaseId, String action, AuthorizationType authorizationType,
            Long ticketId, Date expireTime) {
        PermissionEntity entity = TestRandom.nextObject(PermissionEntity.class);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setResourceIdentifier(ResourceType.ODC_DATABASE.name() + ":" + databaseId);
        entity.setAction(action);
        entity.setAuthorizationType(authorizationType);
        entity.setTicketId(ticketId);
        entity.setExpireTime(expireTime);
        entity.setResourceType(ResourceType.ODC_DATABASE);
        entity.setResourceId(databaseId);
        return permissionRepository.saveAndFlush(entity);
    }

    private void createUserPermission(Long permissionId) {
        UserPermissionEntity entity = TestRandom.nextObject(UserPermissionEntity.class);
        entity.setOrganizationId(ORGANIZATION_ID);
        entity.setPermissionId(permissionId);
        entity.setUserId(USER_ID);
        userPermissionRepository.saveAndFlush(entity);
    }

}
