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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import com.oceanbase.odc.core.migrate.resource.mapper.TableSpecDataSpecsMapper;
import com.oceanbase.odc.core.migrate.resource.mapper.TableTemplateDataRecordMapper;
import com.oceanbase.odc.core.migrate.resource.model.DataRecord;
import com.oceanbase.odc.core.migrate.resource.model.DataSpec;
import com.oceanbase.odc.core.migrate.resource.model.ResourceSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableSpec;
import com.oceanbase.odc.core.migrate.resource.model.TableTemplate;
import com.oceanbase.odc.core.migrate.resource.repository.DataRecordRepository;
import com.oceanbase.odc.core.migrate.tool.EnvInitializer;
import com.oceanbase.odc.core.migrate.tool.TestSpecEntityDataSpecsMappers;
import com.oceanbase.odc.core.migrate.tool.TestValueEncoder;

import lombok.NonNull;

/**
 * Test cases for {@link DataRecordRepository}
 *
 * @author yh263208
 * @date 2022-04-22 14:52
 * @since ODC_release_3.3.1
 */
public class TableTemplateDataRecordMapperTest {

    private static final String JDBC_URL = "jdbc:h2:mem:test;MODE=MySQL";

    private DataSource dataSource;

    private final Long organizationId = 1000L;
    private final Long creatorId = 2000L;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        dataSource = new SingleConnectionDataSource(JDBC_URL, false);
        EnvInitializer.init(dataSource);
    }

    @Test
    public void generate_withoutValueRef_returnOneRecord() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);
        TableTemplate template = defaultEntity.getTemplates().get(0);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(getMapper(defaultEntity, manager));
        List<DataRecord> recordList = factory.entityToModel(template);

        Assert.assertEquals(1, recordList.size());
        recordList.get(0).getData().forEach(spec -> {
            if ("organization_id".equals(spec.getName())) {
                Assert.assertEquals(organizationId, spec.getValue());
            } else if ("creator_id".equals(spec.getName())) {
                Assert.assertEquals(creatorId, spec.getValue());
            } else if ("id".equals(spec.getName())) {
                Assert.assertNull(spec.getValue());
            }
        });
    }

    @Test
    public void generate_withoutIdSetAndIdNotNull_expThrown() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);
        TableTemplate template = defaultEntity.getTemplates().get(1);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(getMapper(defaultEntity, manager));
        thrown.expectMessage("Data can not be null");
        thrown.expect(NullPointerException.class);
        factory.entityToModel(template);
    }

    @Test
    public void generate_passwordEncrypt_passwordEncrypted() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);
        TableTemplate template = defaultEntity.getTemplates().get(2);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(getMapper(defaultEntity, manager));

        List<DataRecord> recordList = factory.entityToModel(template);
        List<DataSpec> specs = recordList.get(0).getData().stream()
                .filter(spec -> "password".equals(spec.getName())).collect(Collectors.toList());
        Assert.assertEquals(1, specs.size());
        List<TableSpec> entities = template.getSpecs().stream().filter(entity -> "password".equals(entity.getName()))
                .collect(Collectors.toList());
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(TestValueEncoder.ENCODED_STR, specs.get(0).getValue().toString());
    }

    @Test
    public void generate_fieldValueRef_successRef() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);
        TableTemplate template = defaultEntity.getTemplates().get(2);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(getMapper(defaultEntity, manager));

        List<DataRecord> recordList = factory.entityToModel(template);
        Assert.assertEquals(1, recordList.size());
        recordList.get(0).getData().forEach(spec -> {
            if ("id".equals(spec.getName())) {
                Assert.assertEquals(1000, spec.getValue());
            }
        });
    }

    @Test
    public void generate_fieldValueRefCrossFile_successRef() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);
        TableTemplate template = manager.findBySuffix("test_generate_factory_1.yml").get(0).getTemplates().get(0);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(getMapper(defaultEntity, manager));

        List<DataRecord> recordList = factory.entityToModel(template);
        Assert.assertNull(template.getSpecs().get(2).getValue());
        Assert.assertEquals(1, recordList.size());
        recordList.get(0).getData().forEach(spec -> {
            if ("id".equals(spec.getName())) {
                Assert.assertEquals(1000, spec.getValue());
            }
        });
    }

    @Test
    public void generate_dbValueRef_successRef() throws IOException {
        ResourceManager manager = getResourceManager();
        ResourceSpec defaultEntity = getDefaultResourceEntity(manager);
        TableTemplate template = defaultEntity.getTemplates().get(3);
        TableTemplateDataRecordMapper factory = new TableTemplateDataRecordMapper(getMapper(defaultEntity, manager));

        List<DataRecord> recordList = factory.entityToModel(template);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Long roleCount =
                jdbcTemplate.query("select count(1) from iam_role", (resultSet, i) -> resultSet.getLong(1)).get(0);
        Long userCount =
                jdbcTemplate.query("select count(1) from iam_user", (resultSet, i) -> resultSet.getLong(1)).get(0);
        Assert.assertEquals(roleCount * userCount, recordList.size());
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
        parameters.putIfAbsent("ORGANIZATION_ID", organizationId);
        parameters.putIfAbsent("USER_ID", creatorId);
        return new ResourceManager(parameters, "migrate/resource/test_generate_factory.yml",
                "migrate/resource/test_generate_factory_1.yml");
    }

}
