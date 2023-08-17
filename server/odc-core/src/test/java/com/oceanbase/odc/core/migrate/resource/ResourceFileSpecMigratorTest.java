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

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.core.migrate.resource.factory.DefaultResourceMapperFactory;
import com.oceanbase.odc.core.migrate.resource.factory.EntityMapperFactory;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.ResourceConfig;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.repository.DataRecordRepository;
import com.oceanbase.odc.core.migrate.tool.EnvInitializer;
import com.oceanbase.odc.core.migrate.tool.TestValueEncoderFactory;
import com.oceanbase.odc.core.migrate.tool.TestValueGeneratorFactory;

import lombok.NonNull;

/**
 * {@link ResourceSpecMigrator}
 *
 * @author yh263208
 * @date 2022-04-23 22:16
 * @since ODC_release_3.3.1
 */
public class ResourceFileSpecMigratorTest {

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
    public void save_savePermissionRoleRelation_totalSaved() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);

        ResourceSpecMigrator initializer =
                new ResourceSpecMigrator(new DataRecordRepository(dataSource), getFactory(manager));
        initializer.migrate(defaultEntity);

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

    private EntityMapperFactory<ResourceSpec, List<DataRecord>> getFactory(@NonNull ResourceManager manager) {
        ResourceConfig config = ResourceConfig.builder()
                .valueEncoderFactory(new TestValueEncoderFactory())
                .valueGeneratorFactory(new TestValueGeneratorFactory())
                .dataSource(dataSource).build();
        return new DefaultResourceMapperFactory(config, manager);
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
