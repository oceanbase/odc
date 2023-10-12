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

package com.oceanbase.odc.metadb.connection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * Test cases for {@link ConnectionAttributeRepository}
 *
 * @author yh263208
 * @date 2023-09-12 15:46
 * @since ODC_release_4.2.2
 */
public class ConnectionAttributeRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ConnectionAttributeRepository repository;

    @Before
    public void setUp() {
        this.repository.deleteAll();
    }

    @Test
    public void save_saveAnAttribute_savedSucceed() {
        ConnectionAttributeEntity entity = TestRandom.nextObject(ConnectionAttributeEntity.class);
        entity = this.repository.save(entity);
        Assert.assertNotNull(entity);
    }

    @Test
    public void save_saveAttributes_saveSucceed() {
        List<ConnectionAttributeEntity> entities = new ArrayList<>();
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        entities = this.repository.saveAll(entities);
        Assert.assertEquals(2, entities.size());
    }

    @Test
    public void delete_deleteByConnectionId_deleteSucceed() {
        List<ConnectionAttributeEntity> entities = new ArrayList<>();
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        Long connectionId = 100L;
        entities.forEach(e -> e.setConnectionId(connectionId));
        entities = this.repository.saveAll(entities);
        int affectRows = this.repository.deleteByConnectionId(connectionId);
        Assert.assertEquals(entities.size(), affectRows);
    }

    @Test
    public void delete_deleteByConnectionIds_deleteSucceed() {
        List<ConnectionAttributeEntity> entities = new ArrayList<>();
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        entities = this.repository.saveAll(entities);

        Set<Long> connectionIds = entities.stream().map(
                ConnectionAttributeEntity::getConnectionId).collect(Collectors.toSet());
        int affectRows = this.repository.deleteByConnectionIds(connectionIds);
        Assert.assertEquals(entities.size(), affectRows);
    }

    @Test
    public void find_findByConnectionId_findSucceed() {
        List<ConnectionAttributeEntity> entities = new ArrayList<>();
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        entities.add(TestRandom.nextObject(ConnectionAttributeEntity.class));
        Long connectionId = 100L;
        entities.forEach(e -> e.setConnectionId(connectionId));
        entities = this.repository.saveAll(entities);

        List<ConnectionAttributeEntity> results = this.repository.findByConnectionId(connectionId);
        Set<ConnectionAttributeEntity> actual = new HashSet<>(results);
        Set<ConnectionAttributeEntity> expect = new HashSet<>(entities);
        Assert.assertEquals(actual, expect);
    }

}
