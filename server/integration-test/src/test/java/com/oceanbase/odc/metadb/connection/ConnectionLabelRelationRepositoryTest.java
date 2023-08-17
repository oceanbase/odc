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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;

import cn.hutool.core.lang.Assert;

/**
 * Test cases for {@link ConnectionLabelRelationRepository}
 *
 * @author yh263208
 * @date 2022-11-30 19:41
 * @since ODC_release_4.1.0
 */
public class ConnectionLabelRelationRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ConnectionLabelRelationRepository repository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    public void create_createConnectionLabelRelation_createSucceed() {
        ConnectionLabelRelationEntity entity = getRelation();
        Assert.notNull(repository.save(entity).getId());
    }

    @Test
    public void findByUserIdAndConnectionId_findExistsRelations_getSucceed() {
        Long userId = 1000L;
        Long connectionId = 2000L;
        ConnectionLabelRelationEntity e1 = getRelation();
        e1.setUserId(userId);
        e1.setConnectionId(connectionId);
        ConnectionLabelRelationEntity e2 = getRelation();
        e2.setUserId(userId);
        e2.setConnectionId(connectionId);
        List<ConnectionLabelRelationEntity> expect = repository.saveAll(Arrays.asList(e1, e2));

        List<ConnectionLabelRelationEntity> actual = repository.findByUserIdAndConnectionId(userId, connectionId);
        Assert.equals(expect.size(), actual.size());
    }

    @Test
    public void findByUserIdAndConnectionId_findNotExistsRelation_getEmpty() {
        ConnectionLabelRelationEntity entity = getRelation();
        repository.save(entity);

        List<ConnectionLabelRelationEntity> actual = repository.findByUserIdAndConnectionId(
                entity.getUserId(), entity.getConnectionId() + 1000);
        Assert.isTrue(actual.isEmpty());
    }

    @Test
    public void findByUserId_findExistsRelations_getSucceed() {
        Long userId = 1000L;
        ConnectionLabelRelationEntity e1 = getRelation();
        e1.setUserId(userId);
        ConnectionLabelRelationEntity e2 = getRelation();
        e2.setUserId(userId);
        List<ConnectionLabelRelationEntity> expect = repository.saveAll(Arrays.asList(e1, e2));

        List<ConnectionLabelRelationEntity> actual = repository.findByUserId(userId);
        Assert.equals(expect.size(), actual.size());
    }

    @Test
    public void findByUserId_findNotExistsRelation_getEmpty() {
        ConnectionLabelRelationEntity entity = getRelation();
        repository.save(entity);

        List<ConnectionLabelRelationEntity> actual = repository.findByUserId(entity.getUserId() + 1000);
        Assert.isTrue(actual.isEmpty());
    }

    @Test
    public void deleteByLabelId_deleteAnExistsRelation_deleteSucceed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByLabelId(entity.getLabelId()));
    }

    @Test
    public void deleteByLabelId_deleteNotExistsRelation_deleteFailed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByLabelId(entity.getLabelId() + 100));
    }

    @Test
    public void deleteByConnectionId_deleteAnExistsRelation_deleteSucceed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByConnectionId(entity.getConnectionId()));
    }

    @Test
    public void deleteByConnectionId_deleteNotExistsRelation_deleteFailed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByConnectionId(entity.getConnectionId() + 100));
    }

    @Test
    public void deleteByUserIdConnectionId_deleteAnExistsRelation_deleteSucceed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByUserIdAndConnectionId(
                entity.getUserId(), entity.getConnectionId()));
    }

    @Test
    public void deleteByUserIdConnectionId_deleteNotExistsRelation_deleteFailed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByUserIdAndConnectionId(
                entity.getUserId(), entity.getConnectionId() + 100));
    }

    @Test
    public void deleteByUserId_deleteAnExistsRelation_deleteSucceed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByUserId(entity.getUserId()));
    }

    @Test
    public void deleteByUserId_deleteNotExistsRelation_deleteFailed() {
        ConnectionLabelRelationEntity entity = getRelation();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByUserId(entity.getUserId() + 100));
    }

    @Test
    public void deleteByConnectionIds_deleteExistsEntities_deleteSucceed() {
        ConnectionLabelRelationEntity e1 = getRelation();
        e1.setConnectionId(1L);
        ConnectionLabelRelationEntity e2 = getRelation();
        e2.setConnectionId(2L);
        ConnectionLabelRelationEntity e3 = getRelation();
        e3.setConnectionId(3L);
        repository.saveAll(Arrays.asList(e1, e2, e3));

        int affectRows = repository.deleteByConnectionIds(new HashSet<>(Arrays.asList(1L, 2L, 3L)));
        Assert.equals(3, affectRows);
    }

    private ConnectionLabelRelationEntity getRelation() {
        ConnectionLabelRelationEntity entity = TestRandom.nextObject(ConnectionLabelRelationEntity.class);
        entity.setId(null);
        return entity;
    }
}
