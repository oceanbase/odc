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
package com.oceanbase.odc.service.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.exception.AccessDeniedException;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.dbobject.DBColumnEntity;
import com.oceanbase.odc.metadb.dbobject.DBColumnRepository;
import com.oceanbase.odc.metadb.dbobject.DBObjectEntity;
import com.oceanbase.odc.metadb.dbobject.DBObjectRepository;
import com.oceanbase.odc.service.collaboration.environment.model.EnvironmentStyle;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseMapper;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.schema.DBSchemaIndexService;
import com.oceanbase.odc.service.db.schema.model.QueryDBObjectParams;
import com.oceanbase.odc.service.db.schema.model.QueryDBObjectResp;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.test.tool.TestRandom;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author gaoda.xy
 * @date 2024/4/2 14:53
 */
public class DBSchemaIndexServiceTest extends ServiceTestEnv {

    @Autowired
    private DBSchemaIndexService service;
    @Autowired
    private DatabaseRepository databaseRepository;
    @Autowired
    private DBObjectRepository dbObjectRepository;
    @Autowired
    private DBColumnRepository dbColumnRepository;

    @MockBean
    private ProjectPermissionValidator projectPermissionValidator;
    @MockBean
    private ProjectService projectService;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private ConnectionService connectionService;
    @MockBean
    private DBResourcePermissionHelper permissionHelper;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final DatabaseMapper databaseMapper = DatabaseMapper.INSTANCE;
    private static final Long VALID_PROJECT_ID = 1L;
    private static final Long INVALID_PROJECT_ID = 2L;
    private static final Long CONNECTION_ID = 1L;
    private static final Long USER_ID = 1L;

    private List<Long> validDatabaseIds = new ArrayList<>();

    @Before
    public void setUp() {
        tearDown();
        // Create metadata
        List<Database> databases = createDatabaseEntity(2, VALID_PROJECT_ID);
        validDatabaseIds = databases.stream().map(Database::getId).collect(Collectors.toList());
        databases.addAll(createDatabaseEntity(1, INVALID_PROJECT_ID));
        for (Database database : databases) {
            List<DBObjectEntity> objects = createDBObjectEntity(2, database.getId(), DBObjectType.TABLE);
            objects.addAll(createDBObjectEntity(1, database.getId(), DBObjectType.VIEW));
            for (DBObjectEntity object : objects) {
                createDBColumnEntity(2, database.getId(), object.getId());
            }
            createDBObjectEntity(1, database.getId(), DBObjectType.FUNCTION);
            createDBObjectEntity(2, database.getId(), DBObjectType.PROCEDURE);
            createDBObjectEntity(1, database.getId(), DBObjectType.TRIGGER);
            createDBObjectEntity(2, database.getId(), DBObjectType.SEQUENCE);
            createDBObjectEntity(1, database.getId(), DBObjectType.PACKAGE);
            createDBObjectEntity(2, database.getId(), DBObjectType.TYPE);
            createDBObjectEntity(1, database.getId(), DBObjectType.SYNONYM);
        }
        // Mock all not related service beans
        Mockito.doNothing().when(projectPermissionValidator).checkProjectRole(Mockito.eq(VALID_PROJECT_ID),
                Mockito.anyList());
        Mockito.doThrow(AccessDeniedException.class).when(projectPermissionValidator)
                .checkProjectRole(Mockito.eq(INVALID_PROJECT_ID), Mockito.anyList());
        Mockito.when(projectService.getMemberProjectIds(USER_ID))
                .thenReturn(new HashSet<>(Arrays.asList(VALID_PROJECT_ID)));
        Mockito.when(projectService.mapByIdIn(Mockito.anySet())).thenReturn(getProjectMap());
        Mockito.when(authenticationFacade.currentUserId()).thenReturn(USER_ID);
        Mockito.when(authenticationFacade.currentUser()).thenReturn(getUser());
        Mockito.when(connectionService.getBasicWithoutPermissionCheck(Mockito.eq(CONNECTION_ID)))
                .thenReturn(getConnectionConfig());
        Mockito.when(connectionService.mapByIdIn(Mockito.anySet())).thenReturn(getConnectionMap());
        Mockito.when(permissionHelper.getDBPermissions(Mockito.anySet())).thenReturn(
                getDatabaseId2PermissionTypes(databases.stream().map(Database::getId).collect(Collectors.toList())));
    }

    @After
    public void tearDown() {
        databaseRepository.deleteAll();
        dbObjectRepository.deleteAll();
        dbColumnRepository.deleteAll();
    }

    @Test
    public void testList_specifyProjectId_all() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(VALID_PROJECT_ID)
                .searchKey("test").build();
        QueryDBObjectResp resp = service.listDatabaseObjects(params);
        Assert.assertEquals(2, resp.getDatabases().size());
        Assert.assertEquals(12, resp.getDbColumns().size());
        Assert.assertEquals(26, resp.getDbObjects().size());
    }

    @Test
    public void testList_specifyProjectIdAndDatabaseId_all() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(VALID_PROJECT_ID)
                .databaseIds(validDatabaseIds.subList(0, 1))
                .searchKey("test").build();
        QueryDBObjectResp resp = service.listDatabaseObjects(params);
        Assert.assertEquals(1, resp.getDatabases().size());
        Assert.assertEquals(6, resp.getDbColumns().size());
        Assert.assertEquals(13, resp.getDbObjects().size());
    }

    @Test
    public void testList_specifyProjectIdAndDatabaseId_Database() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(VALID_PROJECT_ID)
                .databaseIds(validDatabaseIds.subList(0, 1))
                .types(Collections.singletonList(DBObjectType.SCHEMA))
                .searchKey("test").build();
        QueryDBObjectResp resp = service.listDatabaseObjects(params);
        Assert.assertEquals(1, resp.getDatabases().size());
        Assert.assertTrue(CollectionUtils.isEmpty(resp.getDbColumns()));
        Assert.assertTrue(CollectionUtils.isEmpty(resp.getDbObjects()));
    }

    @Test
    public void testList_specifyProjectIdAndDatabaseId_Column() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(VALID_PROJECT_ID)
                .databaseIds(validDatabaseIds.subList(0, 1))
                .types(Collections.singletonList(DBObjectType.COLUMN))
                .searchKey("test").build();
        QueryDBObjectResp resp = service.listDatabaseObjects(params);
        Assert.assertEquals(6, resp.getDbColumns().size());
        Assert.assertTrue(CollectionUtils.isEmpty(resp.getDatabases()));
        Assert.assertTrue(CollectionUtils.isEmpty(resp.getDbObjects()));
    }

    @Test
    public void testList_specifyProjectIdAndDatabaseId_Function() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(VALID_PROJECT_ID)
                .databaseIds(validDatabaseIds.subList(0, 1))
                .types(Arrays.asList(DBObjectType.SYNONYM, DBObjectType.PUBLIC_SYNONYM))
                .searchKey("test").build();
        QueryDBObjectResp resp = service.listDatabaseObjects(params);
        Assert.assertEquals(1, resp.getDbObjects().size());
        Assert.assertTrue(CollectionUtils.isEmpty(resp.getDatabases()));
        Assert.assertTrue(CollectionUtils.isEmpty(resp.getDbColumns()));
    }

    @Test
    public void testList_specifyInvalidProjectId_throwAccessDeniedException() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(INVALID_PROJECT_ID)
                .searchKey("test").build();
        thrown.expect(AccessDeniedException.class);
        service.listDatabaseObjects(params);
    }

    @Test
    public void testList_specifyDatasourceId_all() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .datasourceId(CONNECTION_ID)
                .searchKey("test").build();
        QueryDBObjectResp resp = service.listDatabaseObjects(params);
        Assert.assertEquals(2, resp.getDatabases().size());
        Assert.assertEquals(12, resp.getDbColumns().size());
        Assert.assertEquals(26, resp.getDbObjects().size());
    }

    @Test
    public void testList_specifyDatasourceIdAndProjectId_throwIllegalArgumentException() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .projectId(VALID_PROJECT_ID)
                .datasourceId(CONNECTION_ID)
                .searchKey("test").build();
        thrown.expect(IllegalArgumentException.class);
        service.listDatabaseObjects(params);
    }

    @Test
    public void testList_specifyNeitherDatasourceIdAndProjectId_throwIllegalArgumentException() {
        QueryDBObjectParams params = QueryDBObjectParams.builder()
                .searchKey("test").build();
        thrown.expect(IllegalArgumentException.class);
        service.listDatabaseObjects(params);
    }

    private List<Database> createDatabaseEntity(int quantity, Long projectId) {
        List<Database> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            DatabaseEntity entity = TestRandom.nextObject(DatabaseEntity.class);
            entity.setId(null);
            entity.setProjectId(projectId);
            entity.setConnectionId(CONNECTION_ID);
            entity.setExisted(true);
            entity.setName("project-" + projectId + "-test-" + i);
            DatabaseEntity saved = databaseRepository.saveAndFlush(entity);
            result.add(databaseMapper.entityToModel(saved));
        }
        return result;
    }

    private List<DBObjectEntity> createDBObjectEntity(int quantity, Long databaseId, DBObjectType type) {
        List<DBObjectEntity> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            DBObjectEntity entity = TestRandom.nextObject(DBObjectEntity.class);
            entity.setId(null);
            entity.setDatabaseId(databaseId);
            entity.setName("database-" + databaseId + "-type-" + type + "-test-" + i);
            entity.setType(type);
            DBObjectEntity saved = dbObjectRepository.saveAndFlush(entity);
            result.add(saved);
        }
        return result;
    }

    private List<DBColumnEntity> createDBColumnEntity(int quantity, Long databaseId, Long objectId) {
        List<DBColumnEntity> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            DBColumnEntity entity = TestRandom.nextObject(DBColumnEntity.class);
            entity.setId(null);
            entity.setDatabaseId(databaseId);
            entity.setObjectId(objectId);
            entity.setName("database-" + databaseId + "-object-" + objectId + "-test-" + i);
            DBColumnEntity saved = dbColumnRepository.saveAndFlush(entity);
            result.add(saved);
        }
        return result;
    }

    private ConnectionConfig getConnectionConfig() {
        ConnectionConfig config = new ConnectionConfig();
        config.setId(CONNECTION_ID);
        config.setCreatorId(USER_ID);
        return config;
    }

    private User getUser() {
        User user = User.of(USER_ID);
        user.setOrganizationType(OrganizationType.TEAM);
        return user;
    }

    private Map<Long, List<Project>> getProjectMap() {
        Map<Long, List<Project>> map = new HashMap<>();
        Project validProject = new Project();
        validProject.setId(VALID_PROJECT_ID);
        map.put(VALID_PROJECT_ID, Collections.singletonList(validProject));
        Project invalidProject = new Project();
        invalidProject.setId(INVALID_PROJECT_ID);
        map.put(INVALID_PROJECT_ID, Collections.singletonList(invalidProject));
        return map;
    }

    private Map<Long, List<ConnectionConfig>> getConnectionMap() {
        Map<Long, List<ConnectionConfig>> map = new HashMap<>();
        ConnectionConfig config = new ConnectionConfig();
        config.setId(CONNECTION_ID);
        config.setEnvironmentId(1L);
        config.setEnvironmentName("test");
        config.setEnvironmentStyle(EnvironmentStyle.GRAY);
        map.put(CONNECTION_ID, Collections.singletonList(config));
        return map;
    }

    private Map<Long, Set<DatabasePermissionType>> getDatabaseId2PermissionTypes(Collection<Long> databaseIds) {
        Map<Long, Set<DatabasePermissionType>> ret = new HashMap<>();
        if (CollectionUtils.isEmpty(databaseIds)) {
            return ret;
        }
        for (Long id : databaseIds) {
            Set<DatabasePermissionType> permissionTypes = new HashSet<>();
            permissionTypes.add(DatabasePermissionType.QUERY);
            ret.put(id, permissionTypes);
        }
        return ret;
    }

}
