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
package com.oceanbase.odc.service.connection.logicaldatabase;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.DatabaseMappingRepository;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.database.DatabaseMapper;
import com.oceanbase.odc.service.connection.database.model.DatabaseType;
import com.oceanbase.odc.service.connection.logicaldatabase.model.CreateLogicalDatabaseReq;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * @Author: Lebie
 * @Date: 2024/5/21 16:00
 * @Description: []
 */
public class LogicalDatabaseServiceTest extends ServiceTestEnv {
    private static final Long ORGANIZATION_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final Long CONNECTION_ID = 1L;
    private static final Long ENVIRONMENT_ID = 1L;
    private static final Long PROJECT_ID = 1L;
    private static final Long LOGICAL_DATABASE_ID = 5L;
    private static final String LOGICAL_DATABASE_NAME = "lebie_test";
    private static final String LOGICAL_DATABASE_ALIAS = "lebie_test_alias";
    private static final DatabaseMapper databaseMapper = DatabaseMapper.INSTANCE;

    @Autowired
    private LogicalDatabaseService logicalDatabaseService;
    @Autowired
    private DatabaseRepository databaseRepository;
    @MockBean
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private ProjectPermissionValidator projectPermissionValidator;
    @MockBean
    private ConnectionConfigRepository connectionRepository;
    @MockBean
    private EnvironmentService environmentService;
    @MockBean
    private DBResourcePermissionHelper permissionHelper;
    @MockBean
    private DatabaseMappingRepository databaseMappingRepository;

    @Before
    public void setUp() throws Exception {
        when(authenticationFacade.currentOrganizationId()).thenReturn(ORGANIZATION_ID);
        when(connectionRepository.findById(anyLong())).thenReturn(Optional.of(getConnectionEntity()));
        when(environmentService.detailSkipPermissionCheck(anyLong()))
                .thenReturn(TestRandom.nextObject(Environment.class));
        doNothing().when(projectPermissionValidator).checkProjectRole(anyLong(), anyList());
        doNothing().when(permissionHelper).checkDBPermissions(anyCollection(), anyCollection());
        List<DatabaseEntity> physicalDatabases = listDatabases(4);
        databaseRepository.saveAllAndFlush(physicalDatabases);
    }

    @Test
    public void testCreate() {
        CreateLogicalDatabaseReq req = new CreateLogicalDatabaseReq();
        req.setProjectId(PROJECT_ID);
        req.setAlias(LOGICAL_DATABASE_ALIAS);
        req.setName(LOGICAL_DATABASE_NAME);
        Set<Long> databaseIds = new HashSet<>();
        databaseIds.addAll(Arrays.asList(1L, 2L, 3L, 4L));
        req.setPhysicalDatabaseIds(databaseIds);
        Assert.assertTrue(logicalDatabaseService.create(req));
    }

    @Test
    public void testDetail() {
        DatabaseEntity logicalDatabase = TestRandom.nextObject(DatabaseEntity.class);
        logicalDatabase.setId(LOGICAL_DATABASE_ID);
        logicalDatabase.setType(DatabaseType.LOGICAL);
        databaseRepository.saveAndFlush(logicalDatabase);
        when(databaseMappingRepository.findByLogicalDatabaseId(anyLong())).thenReturn(listDatabaseMappings(4));
        Assert.assertNotNull(logicalDatabaseService.detail(LOGICAL_DATABASE_ID));
    }

    private ConnectionEntity getConnectionEntity() {
        ConnectionEntity config = new ConnectionEntity();
        config.setId(CONNECTION_ID);
        config.setCreatorId(USER_ID);
        config.setDialectType(DialectType.OB_MYSQL);
        return config;
    }

    private List<DatabaseEntity> listDatabases(int quantity) {
        List<DatabaseEntity> entities = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            DatabaseEntity entity = TestRandom.nextObject(DatabaseEntity.class);
            entity.setType(DatabaseType.PHYSICAL);
            entity.setOrganizationId(ORGANIZATION_ID);
            entity.setProjectId(PROJECT_ID);
            entity.setEnvironmentId(ENVIRONMENT_ID);
            entities.add(entity);
        }
        return entities;
    }

    private List<DatabaseMappingEntity> listDatabaseMappings(int quantity) {
        List<DatabaseMappingEntity> mappings = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            DatabaseMappingEntity mapping = TestRandom.nextObject(DatabaseMappingEntity.class);
            mapping.setOrganizationId(ORGANIZATION_ID);
            mapping.setLogicalDatabaseId(LOGICAL_DATABASE_ID);
            mapping.setPhysicalDatabaseId((long) i + 1);
            mapping.setLogicalDatabaseId(PROJECT_ID);
            mappings.add(mapping);
        }
        return mappings;
    }

}
