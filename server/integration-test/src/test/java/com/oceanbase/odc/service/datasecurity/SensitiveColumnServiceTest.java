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
package com.oceanbase.odc.service.datasecurity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolationException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.datasecurity.SensitiveColumnEntity;
import com.oceanbase.odc.metadb.datasecurity.SensitiveColumnRepository;
import com.oceanbase.odc.service.collaboration.project.model.Project;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;
import com.oceanbase.odc.service.datasecurity.model.QuerySensitiveColumnParams;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnStats;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnType;
import com.oceanbase.odc.service.datasecurity.util.SensitiveColumnMapper;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

/**
 * @author gaoda.xy
 * @date 2023/5/22 20:17
 */
public class SensitiveColumnServiceTest extends ServiceTestEnv {

    @Autowired
    private SensitiveColumnService service;

    @MockBean
    private MaskingAlgorithmService algorithmService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private ConnectionService connectionService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private SensitiveColumnRepository repository;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final Long DEFAULT_MASKING_ALGORITHM_ID = 1L;
    private static final Long DEFAULT_PROJECT_ID = 1L;
    private static final Long DEFAULT_DATASOURCE_ID = 1L;
    private static final Long DEFAULT_DATABASE_ID = 1L;

    @Before
    public void setUp() {
        repository.deleteAll();
        MaskingAlgorithm algorithm = new MaskingAlgorithm();
        algorithm.setId(DEFAULT_MASKING_ALGORITHM_ID);
        algorithm.setOrganizationId(authenticationFacade.currentOrganizationId());
        Mockito.when(algorithmService.batchNullSafeGetModel(Mockito.anySet()))
                .thenReturn(Collections.singletonList(algorithm));
        Database database = createDatabase(DEFAULT_DATABASE_ID, DEFAULT_PROJECT_ID);
        Mockito.when(databaseService.listDatabasesByIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(database));
        Mockito.when(databaseService.listDatabaseIdsByProjectId(Mockito.anyLong()))
                .thenReturn(Collections.singleton(DEFAULT_DATABASE_ID));
        Mockito.when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(database));
        Mockito.when(databaseService.listDatabaseByNames(Mockito.anyCollection()))
                .thenReturn(Collections.singleton(database));
        Mockito.when(connectionService.innerListByIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(database.getDataSource()));
    }

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void test_exists_notExists() {
        SensitiveColumn column =
                batchCreateSensitiveColumn(1, 1, DEFAULT_DATABASE_ID, "test_exists", "test_exists").get(0);
        Assert.assertFalse(service.exists(DEFAULT_PROJECT_ID, column));
    }

    @Test
    public void test_exists_exists() {
        List<SensitiveColumn> columns =
                batchCreateSensitiveColumn(1, 1, DEFAULT_DATABASE_ID, "test_exists", "test_exists");
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
        Assert.assertTrue(service.exists(DEFAULT_PROJECT_ID, columns.get(0)));
    }

    @Test
    public void test_batchCreate_success() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 3, DEFAULT_DATABASE_ID, "test_create", "test_create"));
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, columns);
        Assert.assertEquals(3, created.size());
    }

    @Test
    public void test_batchCreate_badArguments_throwConstraintViolationException() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 3, DEFAULT_DATABASE_ID, "test_create", "test_create"));
        columns.add(createSensitiveColumn(DEFAULT_DATABASE_ID, "test_create", ""));
        thrown.expect(ConstraintViolationException.class);
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
    }

    @Test
    public void test_batchCreate_duplicated_throwBadRequestException() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 1, DEFAULT_DATABASE_ID, "test_create", "test_create"));
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
        thrown.expect(BadRequestException.class);
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
    }

    @Test
    public void test_detail_success() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 1, DEFAULT_DATABASE_ID, "test_update", "test_update"));
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, columns);
        SensitiveColumn detailed = service.detail(DEFAULT_PROJECT_ID, created.get(0).getId());
        Assert.assertEquals(created.get(0).getId(), detailed.getId());
    }

    @Test
    public void test_batchUpdate_success() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 3, DEFAULT_DATABASE_ID, "test_batchUpdate", "test_batchUpdate"));
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, columns);
        List<SensitiveColumn> updated = service.batchUpdate(DEFAULT_PROJECT_ID,
                created.stream().map(SensitiveColumn::getId).collect(Collectors.toList()),
                DEFAULT_MASKING_ALGORITHM_ID + 1L);
        Assert.assertEquals(Long.valueOf(DEFAULT_MASKING_ALGORITHM_ID + 1L), updated.get(0).getMaskingAlgorithmId());
        Assert.assertEquals(Long.valueOf(DEFAULT_MASKING_ALGORITHM_ID + 1L), updated.get(1).getMaskingAlgorithmId());
        Assert.assertEquals(Long.valueOf(DEFAULT_MASKING_ALGORITHM_ID + 1L), updated.get(2).getMaskingAlgorithmId());
    }

    @Test
    public void test_batchDelete_success() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 3, DEFAULT_DATABASE_ID, "test_batchDelete", "test_batchDelete"));
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, columns);
        service.batchDelete(DEFAULT_PROJECT_ID,
                created.stream().map(SensitiveColumn::getId).collect(Collectors.toList()));
        Assert.assertEquals(0, repository.findAll().size());
    }

    @Test
    public void test_list_listAllByProjectId_unPaged() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 5, DEFAULT_DATABASE_ID, "test_list", "test_list"));
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
        Page<SensitiveColumn> listed =
                service.list(DEFAULT_PROJECT_ID, QuerySensitiveColumnParams.builder().build(), Pageable.unpaged());
        Assert.assertEquals(5, listed.getNumberOfElements());
        listed.getContent().forEach(column -> {
            Assert.assertTrue(column.getTableName().startsWith("test_list"));
            Assert.assertTrue(column.getColumnName().startsWith("test_list"));
        });
    }

    @Test
    public void test_list_filteringByParams_paged() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 5, DEFAULT_DATABASE_ID, "test_list", "test_list"));
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
        Page<SensitiveColumn> listed =
                service.list(DEFAULT_PROJECT_ID, QuerySensitiveColumnParams.builder().build(), PageRequest.of(0, 3));
        Assert.assertEquals(3, listed.getNumberOfElements());
        Assert.assertEquals(5, listed.getTotalElements());
        listed.getContent().forEach(column -> {
            Assert.assertTrue(column.getTableName().startsWith("test_list"));
            Assert.assertTrue(column.getColumnName().startsWith("test_list"));
        });
    }

    @Test
    public void test_stats() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 5, DEFAULT_DATABASE_ID, "test_stats", "test_stats"));
        service.batchCreate(DEFAULT_PROJECT_ID, columns);
        SensitiveColumnStats stats = service.stats(DEFAULT_PROJECT_ID);
        Assert.assertEquals(1, stats.getDatabases().size());
        Assert.assertEquals(1, stats.getMaskingAlgorithms().size());
    }

    @Test
    public void test_setEnabled_success() {
        SensitiveColumn column = createSensitiveColumn(DEFAULT_DATABASE_ID, "test_setEnabled", "test_setEnabled");
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, Arrays.asList(column));
        service.setEnabled(DEFAULT_PROJECT_ID, created.get(0).getId(), false);
        Assert.assertEquals(false, service.detail(DEFAULT_PROJECT_ID, created.get(0).getId()).getEnabled());
    }

    @Test
    public void test_nullSafeGet_success() {
        SensitiveColumn column = createSensitiveColumn(DEFAULT_DATABASE_ID, "test", "test");
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, Arrays.asList(column));
        SensitiveColumnEntity entity = service.nullSafeGet(created.get(0).getId());
        Assert.assertEquals(created.get(0), SensitiveColumnMapper.INSTANCE.entityToModel(entity));
    }

    @Test
    public void test_batchNullSafeGet_success() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 2, DEFAULT_DATABASE_ID, "test", "test"));
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, columns);
        List<SensitiveColumnEntity> entities =
                service.batchNullSafeGet(created.stream().map(SensitiveColumn::getId).collect(Collectors.toSet()));
        Assert.assertEquals(created.size(), entities.size());
        for (int i = 0; i < created.size(); i++) {
            Assert.assertEquals(created.get(i), SensitiveColumnMapper.INSTANCE.entityToModel(entities.get(i)));
        }
    }

    @Test
    public void test_batchNullSafeGet_throwNotFoundException() {
        List<SensitiveColumn> columns = new ArrayList<>();
        columns.addAll(batchCreateSensitiveColumn(1, 2, DEFAULT_DATABASE_ID, "test", "test"));
        List<SensitiveColumn> created = service.batchCreate(DEFAULT_PROJECT_ID, columns);
        Set<Long> ids = created.stream().map(SensitiveColumn::getId).collect(Collectors.toSet());
        ids.add(-1L);
        thrown.expect(NotFoundException.class);
        service.batchNullSafeGet(ids);
    }

    private List<SensitiveColumn> batchCreateSensitiveColumn(int start, int end, Long databaseId,
            String tableNamePrefix, String columnNamePrefix) {
        List<SensitiveColumn> columns = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            columns.add(createSensitiveColumn(databaseId, tableNamePrefix + i, columnNamePrefix + i));
        }
        return columns;
    }

    private SensitiveColumn createSensitiveColumn(Long databaseId, String tableName, String columnName) {
        SensitiveColumn column = new SensitiveColumn();
        column.setEnabled(true);
        column.setType(SensitiveColumnType.TABLE_COLUMN);
        column.setDatabase(createDatabase(databaseId, DEFAULT_PROJECT_ID));
        column.setTableName(tableName);
        column.setColumnName(columnName);
        column.setMaskingAlgorithmId(DEFAULT_MASKING_ALGORITHM_ID);
        return column;
    }

    private Database createDatabase(Long id, Long projectId) {
        Database database = new Database();
        database.setId(id);
        database.setName("database");
        Project project = new Project();
        project.setId(projectId);
        database.setProject(project);
        ConnectionConfig datasource = new ConnectionConfig();
        datasource.setId(DEFAULT_DATASOURCE_ID);
        datasource.setName("datasource");
        database.setDataSource(datasource);
        return database;
    }

}
