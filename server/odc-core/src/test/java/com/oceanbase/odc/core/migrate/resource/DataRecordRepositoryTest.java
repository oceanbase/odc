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
package com.oceanbase.odc.core.migrate.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.migrate.resource.mapper.TableSpecDataSpecsMapper;
import com.oceanbase.odc.core.migrate.resource.mapper.TableTemplateDataRecordMapper;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.DataSpec;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableTemplate;
import com.oceanbase.odc.core.migrate.resource.repository.DataRecordRepository;
import com.oceanbase.odc.core.migrate.tool.EnvInitializer;
import com.oceanbase.odc.core.migrate.tool.TestSpecEntityDataSpecsMappers;

import lombok.NonNull;

/**
 * Test cases for {@link DataRecordRepository}
 *
 * @author yh263208
 * @date 2022-04-23 18:17
 * @since ODC-release_3.3.1
 */
public class DataRecordRepositoryTest {

    private static final String JDBC_URL = "jdbc:h2:mem:test;MODE=MySQL";

    private DataSource dataSource;

    @Before
    public void setUp() throws ClassNotFoundException, IOException {
        Class.forName("org.h2.Driver");
        dataSource = new SingleConnectionDataSource(JDBC_URL, false);
        EnvInitializer.init(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("delete from iam_role where 1=1");
        jdbcTemplate.update("delete from iam_permission where 1=1");
        jdbcTemplate.update("delete from iam_role_permission where 1=1");
    }

    @Test
    public void save_permission_successSaved() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);

        TableTemplate entity = defaultEntity.getTemplates().get(0);
        List<DataRecord> permissions = getDataRecords(defaultEntity, manager, entity);
        Assert.assertEquals(1, permissions.size());

        DataRecord permission = permissions.get(0);
        DataRecordRepository repository = new DataRecordRepository(dataSource);
        Assert.assertFalse(repository.exists(permission));
        repository.save(permission);
        Assert.assertTrue(repository.exists(permission));
    }

    @Test
    public void refreshId_permission_idSuccessRefreshed() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);

        TableTemplate entity = defaultEntity.getTemplates().get(0);
        List<DataRecord> permissions = getDataRecords(defaultEntity, manager, entity);
        Assert.assertEquals(1, permissions.size());

        DataRecord permission = permissions.get(0);
        DataRecordRepository repository = new DataRecordRepository(dataSource);
        permission = repository.save(permission);
        DataSpec res = permission.getData().stream().filter(spec -> "id".equals(spec.getName())).peek(DataSpec::refresh)
                .collect(Collectors.toList()).get(0);
        Assert.assertEquals(res.getValue(), entity.getSpecs().get(0).getValue());
    }

    @Test
    public void find_findNonExistsPermission_returnEmpty() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);

        TableTemplate entity = defaultEntity.getTemplates().get(0);
        List<DataRecord> permissions = getDataRecords(defaultEntity, manager, entity);
        Assert.assertEquals(1, permissions.size());

        DataRecord permission = permissions.get(0);
        DataRecordRepository repository = new DataRecordRepository(dataSource);
        List<DataRecord> records = repository.find(permission);
        Assert.assertTrue(records.isEmpty());
    }

    @Test
    public void find_findExistsPermission_returnNotEmpty() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);

        TableTemplate entity = defaultEntity.getTemplates().get(0);
        List<DataRecord> permissions = getDataRecords(defaultEntity, manager, entity);
        Assert.assertEquals(1, permissions.size());

        DataRecord permission = permissions.get(0);
        DataRecordRepository repository = new DataRecordRepository(dataSource);
        permission = repository.save(permission);
        List<DataRecord> records = repository.find(permission);
        Assert.assertEquals(1, records.size());
        Assert.assertEquals(records.get(0), permission);
    }

    @Test
    public void save_savePermissionRoleRelation_totalSaved() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);

        DataRecordRepository repository = new DataRecordRepository(dataSource);
        for (TableTemplate entity : defaultEntity.getTemplates()) {
            List<DataRecord> records = getDataRecords(defaultEntity, manager, entity);
            for (DataRecord record : records) {
                repository.save(record).getData().forEach(DataSpec::refresh);
            }
        }
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<Long> roleIds = jdbcTemplate.query("select id from iam_role", (resultSet, i) -> resultSet.getLong(1));
        List<Long> permissionIds =
                jdbcTemplate.query("select id from iam_permission", (resultSet, i) -> resultSet.getLong(1));
        List<Pair<Long, Long>> roleId2PermissionId =
                jdbcTemplate.query("select role_id, permission_id from iam_role_permission",
                        (resultSet, i) -> new Pair<>(resultSet.getLong(1), resultSet.getLong(2)));
        roleId2PermissionId.forEach(pair -> {
            Assert.assertTrue(roleIds.contains(pair.left));
            Assert.assertTrue(permissionIds.contains(pair.right));
        });
    }

    private List<DataRecord> getDataRecords(ResourceSpec defaultEntity, ResourceManager manager,
            TableTemplate entity) {
        TableSpecDataSpecsMapper mapper = getMapper(defaultEntity, manager);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(mapper);
        return factory.entityToModel(entity);
    }

    private TableSpecDataSpecsMapper getMapper(@NonNull ResourceSpec defaultEntity,
            @NonNull ResourceManager manager) {
        return TestSpecEntityDataSpecsMappers.defaultMapper(defaultEntity, manager, dataSource);
    }

    private ResourceSpec getDefaultResourceEntity(ResourceManager manager) {
        return manager.findByUrl(manager.getResourceUrls().get(0));
    }

    private ResourceManager getResourceManager() throws IOException {
        Map<String, Object> parameters = new HashMap<>();
        Long organizationId = 1L;
        parameters.putIfAbsent("ORGANIZATION_ID", organizationId);
        Long creatorId = 1L;
        parameters.putIfAbsent("USER_ID", creatorId);
        return new ResourceManager(parameters, "migrate/resource/test_repository.yaml");
    }

}
