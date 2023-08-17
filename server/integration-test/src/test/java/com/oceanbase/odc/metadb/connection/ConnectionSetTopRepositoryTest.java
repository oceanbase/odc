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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.test.tool.TestRandom;

import cn.hutool.core.lang.Assert;

/**
 * Test cases for {@link ConnectionSetTopRepository}
 *
 * @author yh263208
 * @date 2022-11-30 15:32
 * @since ODC_release_4.1.0
 */
public class ConnectionSetTopRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ConnectionSetTopRepository repository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @Test
    public void create_createSetTopEntity_createSucceed() {
        ConnectionSetTopEntity entity = getSetTop();
        Assert.notNull(repository.save(entity).getId());
    }

    @Test
    public void findByUserId_findAnExistsEntity_getSucceed() {
        ConnectionSetTopEntity entity = getSetTop();
        List<ConnectionSetTopEntity> expect = Collections.singletonList(repository.save(entity));

        List<ConnectionSetTopEntity> actual = repository.findByUserId(entity.getUserId());
        Assert.equals(expect, actual);
    }

    @Test
    public void findByUserId_findNotExistsEntity_getEmpty() {
        ConnectionSetTopEntity entity = getSetTop();
        repository.save(entity);

        List<ConnectionSetTopEntity> actual = repository.findByUserId(entity.getUserId() + 100);
        Assert.isTrue(actual.isEmpty());
    }

    @Test
    public void deleteByUserId_deleteAnExistsEntity_deleteSucceed() {
        ConnectionSetTopEntity entity = getSetTop();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByUserId(entity.getUserId()));
    }

    @Test
    public void deleteByUserId_deleteNotExistsEntity_deleteFailed() {
        ConnectionSetTopEntity entity = getSetTop();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByUserId(entity.getUserId() + 100));
    }

    @Test
    public void deleteByConnectionId_deleteAnExistsEntity_deleteSucceed() {
        ConnectionSetTopEntity entity = getSetTop();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByConnectionId(entity.getConnectionId()));
    }

    @Test
    public void deleteByConnectionId_deleteNotExistsEntity_deleteFailed() {
        ConnectionSetTopEntity entity = getSetTop();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByConnectionId(entity.getConnectionId() + 100));
    }

    @Test
    public void deleteByUserIdAndConnectionId_deleteAnExistsEntity_deleteSucceed() {
        ConnectionSetTopEntity entity = getSetTop();
        entity = repository.save(entity);

        Assert.equals(1, repository.deleteByUserIdAndConnectionId(entity.getUserId(), entity.getConnectionId()));
    }

    @Test
    public void deleteByConnectionIds_deleteExistsEntities_deleteSucceed() {
        ConnectionSetTopEntity e1 = getSetTop();
        e1.setConnectionId(1L);
        ConnectionSetTopEntity e2 = getSetTop();
        e2.setConnectionId(2L);
        ConnectionSetTopEntity e3 = getSetTop();
        e3.setConnectionId(3L);
        repository.saveAll(Arrays.asList(e1, e2, e3));

        int affectRows = repository.deleteByConnectionIds(new HashSet<>(Arrays.asList(1L, 2L, 3L)));
        Assert.equals(3, affectRows);
    }

    @Test
    public void deleteByUserIdAndConnectionId_deleteNotExistsEntity_deleteFailed() {
        ConnectionSetTopEntity entity = getSetTop();
        entity = repository.save(entity);

        Assert.equals(0, repository.deleteByUserIdAndConnectionId(entity.getUserId(), entity.getConnectionId() + 100));
    }

    private ConnectionSetTopEntity getSetTop() {
        ConnectionSetTopEntity entity = TestRandom.nextObject(ConnectionSetTopEntity.class);
        entity.setId(null);
        return entity;
    }

}
