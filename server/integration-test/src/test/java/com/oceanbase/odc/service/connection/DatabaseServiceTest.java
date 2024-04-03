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
package com.oceanbase.odc.service.connection;

import java.util.Arrays;
import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.AuthorityTestEnv;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.collaboration.project.model.QueryProjectParams;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.response.PageAndStats;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseSyncStatus;
import com.oceanbase.odc.service.connection.database.model.QueryDatabaseParams;
import com.oceanbase.odc.service.connection.database.model.TransferDatabasesReq;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.QueryConnectionParams;
import com.oceanbase.odc.service.db.DBIdentitiesService;
import com.oceanbase.odc.service.db.DBSchemaService;
import com.oceanbase.odc.service.db.object.model.DBObjectSyncStatus;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 19:57
 * @Description: []
 */
public class DatabaseServiceTest extends AuthorityTestEnv {
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DatabaseRepository databaseRepository;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private ProjectPermissionValidator projectPermissionValidator;

    @MockBean
    private EnvironmentService environmentService;

    @MockBean
    private ConnectionService connectionService;

    @Autowired
    private DBSchemaService dbSchemaService;

    @Autowired
    private DBIdentitiesService dbIdentitiesService;


    @Before
    public void setUp() {
        databaseRepository.deleteAll();
        Mockito.when(projectService.detail(Mockito.anyLong())).thenReturn(getProject());
        Mockito.doNothing().when(projectPermissionValidator).checkProjectRole(Mockito.anyLong(), Mockito.anyList());
        Mockito.when(projectPermissionValidator.hasProjectRole(Mockito.anyLong(), Mockito.anyList())).thenReturn(true);
        Mockito.when(projectPermissionValidator.hasProjectRole(Mockito.anyList(), Mockito.anyList())).thenReturn(true);
        Mockito.when(connectionService.checkPermission(Mockito.anyLong(), Mockito.anyList())).thenReturn(true);
        Mockito.when(projectService.list(QueryProjectParams.builder().build(), Pageable.unpaged()))
                .thenReturn(Page.empty());
        Mockito.when(connectionService.getForConnectionSkipPermissionCheck(Mockito.anyLong()))
                .thenReturn(getDataSource());
        Mockito.when(connectionService.list(QueryConnectionParams.builder().build(), Pageable.unpaged())).thenReturn(
                PageAndStats.empty());
        Mockito.when(connectionService.getWithoutPermissionCheck(Mockito.anyLong())).thenReturn(getDataSource());
        Mockito.when(environmentService.detailSkipPermissionCheck(Mockito.anyLong())).thenReturn(getEnvironment());
    }

    @After
    public void tearDown() {
        databaseRepository.deleteAll();
    }

    @Test
    public void testListByProject_Success() {
        DatabaseEntity database = databaseRepository.saveAndFlush(getEntity());
        Mockito.when(projectService.list(QueryProjectParams.builder().build(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Arrays.asList(getProject())));
        QueryDatabaseParams params = QueryDatabaseParams.builder().projectId(database.getProjectId()).build();
        Page<Database> databases = databaseService.list(params, Pageable.unpaged());
        Assert.assertEquals(1, databases.getSize());
    }

    @Test
    public void testListByDataSource_Success() {
        DatabaseEntity database = databaseRepository.saveAndFlush(getEntity());
        Mockito.when(projectService.list(QueryProjectParams.builder().build(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Arrays.asList(getProject())));
        QueryDatabaseParams params = QueryDatabaseParams.builder().dataSourceId(database.getConnectionId()).build();
        Page<Database> databases = databaseService.list(params, Pageable.unpaged());
        Assert.assertEquals(1, databases.getSize());
    }

    @Test
    public void testListByFuzzyNameAndDataSource_Return1() {
        DatabaseEntity database = databaseRepository.saveAndFlush(getEntity());
        Mockito.when(projectService.list(QueryProjectParams.builder().build(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(Arrays.asList(getProject())));
        QueryDatabaseParams params =
                QueryDatabaseParams.builder().schemaName("fake_").dataSourceId(database.getConnectionId()).build();
        Page<Database> databases = databaseService.list(params, Pageable.unpaged());
        Assert.assertEquals(1, databases.getSize());
    }

    @Test
    public void testListByFuzzyNameAndDataSource_ReturnEmpty() {
        DatabaseEntity database = databaseRepository.saveAndFlush(getEntity());
        ConnectionConfig connection = new ConnectionConfig();
        connection.setId(database.getConnectionId());
        Project project = new Project();
        project.setId(database.getProjectId());
        Mockito.when(connectionService.list(QueryConnectionParams.builder().minPrivilege("update").build(),
                Pageable.unpaged()))
                .thenReturn(PageAndStats.of(new PageImpl<>(Arrays.asList(connection)), new Stats()));
        QueryDatabaseParams params = QueryDatabaseParams.builder()
                .schemaName("real").dataSourceId(1L).build();
        Page<Database> databases = databaseService.list(params, Pageable.unpaged());
        Assert.assertEquals(0, databases.getSize());
    }

    @Test
    public void testDetail_Success() {
        DatabaseEntity entity = databaseRepository.saveAndFlush(getEntity());
        Database database = databaseService.detail(entity.getId());
        Assert.assertEquals("fake_db", database.getName());
    }

    @Test
    public void testFindById_Success() {
        DatabaseEntity entity = databaseRepository.saveAndFlush(getEntity());
        databaseService.detail(entity.getId());
    }

    @Test
    public void testTransfer_Success() {
        Mockito.when(connectionService.checkPermission(Mockito.anyList(), Mockito.anyList())).thenReturn(true);
        Mockito.when(projectPermissionValidator.hasProjectRole(Mockito.anyList(), Mockito.anyList())).thenReturn(true);
        DatabaseEntity saved = databaseRepository.saveAndFlush(getEntity());
        TransferDatabasesReq req = new TransferDatabasesReq();
        req.setDatabaseIds(Arrays.asList(saved.getId()));
        req.setProjectId(2L);
        Assert.assertTrue(databaseService.transfer(req));
        Assert.assertEquals(2L, databaseRepository.findById(saved.getId()).get().getProjectId().longValue());
    }

    private DatabaseEntity getEntity() {
        DatabaseEntity entity = new DatabaseEntity();
        entity.setName("fake_db");
        entity.setDatabaseId("fake_id");
        entity.setExisted(true);
        entity.setProjectId(1L);
        entity.setConnectionId(1L);
        entity.setEnvironmentId(1L);
        entity.setTableCount(1L);
        entity.setCollationName("collation_name");
        entity.setCharsetName("charset_name");
        entity.setLastSyncTime(new Date(System.currentTimeMillis()));
        entity.setOrganizationId(1L);
        entity.setSyncStatus(DatabaseSyncStatus.SUCCEEDED);
        entity.setObjectSyncStatus(DBObjectSyncStatus.INITIALIZED);
        return entity;
    }

    private Project getProject() {
        Project project = new Project();
        project.setId(1L);
        project.setName("fake_project");
        return project;
    }

    private ConnectionConfig getDataSource() {
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setId(1L);
        connectionConfig.setName("fake_datasource");
        return connectionConfig;
    }

    private Environment getEnvironment() {
        Environment environment = new Environment();
        environment.setId(1L);
        environment.setName("fake_env");
        return environment;
    }
}
