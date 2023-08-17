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
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.common.util.MapperUtils;
import com.oceanbase.odc.core.migrate.resource.mapper.TableSpecDataSpecsMapper;
import com.oceanbase.odc.core.migrate.resource.model.DataSpec;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec;
import com.oceanbase.odc.core.migrate.resource.util.PrefixPathMatcher;
import com.oceanbase.odc.core.migrate.tool.EnvInitializer;
import com.oceanbase.odc.core.migrate.tool.TestSpecEntityDataSpecsMappers;

import lombok.NonNull;

/**
 * Test cases fro {@link TableSpecDataSpecsMapper}
 *
 * @author yh263208
 * @date 2022-04-20 18:17
 * @since ODC_release_3.3.1
 */
public class TableSpecDataSpecsMapperTest {

    private static final String JDBC_URL = "jdbc:h2:mem:test;MODE=MySQL";

    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        dataSource = new SingleConnectionDataSource(JDBC_URL, false);
        EnvInitializer.init(dataSource);
    }

    @Test
    public void entityToModel_noValueSet_defaultValueReturn() throws IOException {
        ResourceManager manager = getResource();
        ResourceSpec resourceSpec = manager.findByUrl(manager.getResourceUrls().get(0));

        TableSpec tableSpec =
                MapperUtils.get(resourceSpec, TableSpec.class, new PrefixPathMatcher("templates.0.specs.0"));
        TableSpecDataSpecsMapper mapper = getMapper(manager);
        DataSpec dataSpec = mapper.entityToModel(tableSpec).get(0);
        Assert.assertEquals(tableSpec.getDefaultValue(), dataSpec.getValue());
    }

    @Test
    public void entityToModel_valueEncode_encodedValueReturn() throws IOException {
        ResourceManager manager = getResource();
        ResourceSpec resourceSpec = manager.findByUrl(manager.getResourceUrls().get(0));

        TableSpec tableSpec =
                MapperUtils.get(resourceSpec, TableSpec.class, new PrefixPathMatcher("templates.0.specs.2"));
        TableSpecDataSpecsMapper mapper = getMapper(manager);
        DataSpec dataSpec = mapper.entityToModel(tableSpec).get(0);
        Assert.assertNotNull(dataSpec.getValue());
    }

    @Test
    public void entityToModel_dataTypeConvert_converValueReturn() throws IOException {
        ResourceManager manager = getResource();
        ResourceSpec entity = manager.findByUrl(manager.getResourceUrls().get(0));

        TableSpec tableSpec = MapperUtils.get(entity, TableSpec.class, new PrefixPathMatcher("templates.1.specs.1"));
        TableSpecDataSpecsMapper mapper = getMapper(manager);
        DataSpec dataSpec = mapper.entityToModel(tableSpec).get(0);
        Assert.assertEquals(Integer.class, dataSpec.getValue().getClass());
    }

    @Test
    public void entityToModel_valueSet_valueReturn() throws IOException {
        Integer userId = 10086;
        Map<String, Integer> parameters = new HashMap<>();
        parameters.putIfAbsent("USER_ID", userId);
        ResourceManager manager = getResource(parameters);
        ResourceSpec entity = manager.findByUrl(manager.getResourceUrls().get(0));

        TableSpec tableSpec = MapperUtils.get(entity, TableSpec.class, new PrefixPathMatcher("templates.0.specs.0"));
        TableSpecDataSpecsMapper mapper = getMapper(manager);
        DataSpec dataSpec = mapper.entityToModel(tableSpec).get(0);
        Assert.assertEquals(userId, dataSpec.getValue());
    }

    @Test
    public void entityToModel_fieldRef_valueReturn() throws IOException {
        Integer roleId = 10086;
        Map<String, Integer> parameters = new HashMap<>();
        parameters.putIfAbsent("ROLE_ID", roleId);
        ResourceManager manager = getResource(parameters);
        ResourceSpec entity = manager.findByUrl(manager.getResourceUrls().get(0));

        TableSpec tableSpec = MapperUtils.get(entity, TableSpec.class, new PrefixPathMatcher("templates.2.specs.0"));
        TableSpecDataSpecsMapper mapper = getMapper(manager);
        DataSpec dataSpec = mapper.entityToModel(tableSpec).get(0);
        Assert.assertEquals(roleId, dataSpec.getValue());
    }

    @Test
    public void entityToModel_dbRef_valueReturn() throws IOException {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.update("delete from iam_user_role where 1=1");
        Long userId = 10087L;
        Long userId1 = 10082L;
        Assert.assertEquals(1,
                jdbcTemplate.update("insert into iam_user_role (user_id, role_id, creator_id, organization_id) values("
                        + userId + ", 10086, 1, 100)"));
        Assert.assertEquals(1,
                jdbcTemplate.update("insert into iam_user_role (user_id, role_id, creator_id, organization_id) values("
                        + userId1 + ", 10089, 1, 100)"));
        Integer organizationId = 100;
        Map<String, Integer> parameters = new HashMap<>();
        parameters.putIfAbsent("ORGANIZATION_ID", organizationId);
        ResourceManager manager = getResource(parameters);
        ResourceSpec entity = manager.findByUrl(manager.getResourceUrls().get(0));

        TableSpec tableSpec = MapperUtils.get(entity, TableSpec.class, new PrefixPathMatcher("templates.2.specs.1"));
        TableSpecDataSpecsMapper mapper = getMapper(manager);
        List<DataSpec> dataSpecs = mapper.entityToModel(tableSpec);
        Assert.assertEquals(2, dataSpecs.size());
        Set<Long> userIds = dataSpecs.stream().map(dataSpec -> (Long) dataSpec.getValue()).collect(Collectors.toSet());
        Assert.assertTrue(userIds.contains(userId));
        Assert.assertTrue(userIds.contains(userId1));
    }

    private TableSpecDataSpecsMapper getMapper(@NonNull ResourceManager manager) {
        ResourceSpec entity = manager.findByUrl(manager.getResourceUrls().get(0));
        return TestSpecEntityDataSpecsMappers.defaultMapper(entity, manager, dataSource);
    }

    private ResourceManager getResource(Map<String, ?> parameters) throws IOException {
        return new ResourceManager(parameters, "migrate/resource/iam_user_role_permission.yml");
    }

    private ResourceManager getResource() throws IOException {
        return getResource(new HashMap<>());
    }

}
