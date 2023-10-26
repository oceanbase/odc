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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionLabelRelationEntity;
import com.oceanbase.odc.metadb.connection.ConnectionLabelRelationRepository;
import com.oceanbase.odc.service.connection.model.PropertiesKeys;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test cases for {@link V4103ConnectionLabelMigrate}
 *
 * @author yh263208
 * @date 2022-12-01 16:07
 * @since ODC_release_4.1.0
 */
public class V4103ConnectionLabelMigrateTest extends ServiceTestEnv {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private ConnectionConfigRepository repository;
    @Autowired
    private ConnectionLabelRelationRepository labelRelationRepository;

    @Before
    public void setUp() {
        repository.deleteAll();
        labelRelationRepository.deleteAll();
    }

    @After
    public void clear() {
        repository.deleteAll();
        labelRelationRepository.deleteAll();
    }

    @Test
    public void migrate_labelIdNotExists_migrateSucceed() {
        ConnectionEntity connectionEntity = getConnectionEntiy();
        connectionEntity.setVisibleScope(ConnectionVisibleScope.PRIVATE);
        connectionEntity.setProperties(new HashMap<>());
        repository.save(connectionEntity);

        V4103ConnectionLabelMigrate migrate = new V4103ConnectionLabelMigrate();
        migrate.migrate(dataSource);

        List<ConnectionLabelRelationEntity> entityList = labelRelationRepository.findAll();
        Assert.assertTrue(entityList.isEmpty());
    }

    @Test
    public void migrate_labelIdExists_migrateSucceed() {
        ConnectionEntity connectionEntity = getConnectionEntiy();
        connectionEntity.setVisibleScope(ConnectionVisibleScope.PRIVATE);
        Map<String, String> map = new HashMap<>();
        long labelId = 12L;
        map.put(PropertiesKeys.LABEL_ID, Long.toString(labelId));
        connectionEntity.setProperties(map);
        connectionEntity = repository.save(connectionEntity);

        V4103ConnectionLabelMigrate migrate = new V4103ConnectionLabelMigrate();
        migrate.migrate(dataSource);

        List<ConnectionLabelRelationEntity> actual =
                labelRelationRepository.findByUserId(connectionEntity.getCreatorId());
        actual.forEach(e -> {
            e.setId(null);
            e.setCreateTime(null);
            e.setUpdateTime(null);
        });
        List<ConnectionLabelRelationEntity> expect = new ArrayList<>();
        ConnectionLabelRelationEntity entity = new ConnectionLabelRelationEntity();
        entity.setUserId(connectionEntity.getCreatorId());
        entity.setConnectionId(connectionEntity.getId());
        entity.setLabelId(labelId);
        expect.add(entity);
        Assert.assertEquals(expect, actual);
    }

    private ConnectionEntity getConnectionEntiy() {
        return TestRandom.nextObject(ConnectionEntity.class);
    }

}
