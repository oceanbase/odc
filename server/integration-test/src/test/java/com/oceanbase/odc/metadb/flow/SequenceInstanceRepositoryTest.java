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
package com.oceanbase.odc.metadb.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

/**
 * Test cases for {@link SequenceInstanceRepository}
 *
 * @author yh263208
 * @date 2022-02-09 19:46
 * @since ODC_release_3.3.0
 */
public class SequenceInstanceRepositoryTest extends ServiceTestEnv {

    @Autowired
    private SequenceInstanceRepository repository;
    private final AtomicLong atomicLong = new AtomicLong(1000);

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void clearAll() {
        repository.deleteAll();
    }

    @Test
    public void testSaveSequenceInstanceEntity() {
        SequenceInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(getById(entity.getId()), entity);
    }

    @Test
    public void bulkSave_saveSeveralEntities_saveSucceed() {
        List<SequenceInstanceEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(createEntity());
        }
        repository.batchCreate(entities);
        Assert.assertEquals(10, repository.findAll().size());
    }

    @Test
    public void testDeleteSequenceInstanceEntity() {
        SequenceInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.delete(entity);
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testDeleteByNodeInstanceId() {
        SequenceInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        Assert.assertEquals(1, repository.deleteByNodeInstanceId(entity.getSourceNodeInstanceId()));
        Assert.assertEquals(0, repository.findAll().size());
    }

    @Test
    public void testDeleteByNodeInstanceId_1() {
        SequenceInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        Assert.assertEquals(1, repository.deleteByNodeInstanceId(entity.getTargetNodeInstanceId()));
        Assert.assertEquals(0, repository.findAll().size());
    }

    @Test
    public void testDeleteByFlowInstanceId() {
        SequenceInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        Assert.assertEquals(1, repository.deleteByFlowInstanceId(entity.getFlowInstanceId()));
        Assert.assertEquals(0, repository.findAll().size());
    }

    @Test
    public void testDeleteBySourceNodeInstanceId() {
        SequenceInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        Assert.assertEquals(1, repository.deleteBySourceNodeInstanceId(entity.getSourceNodeInstanceId()));
        Assert.assertEquals(0, repository.findAll().size());
    }

    private SequenceInstanceEntity createEntity() {
        SequenceInstanceEntity entity = new SequenceInstanceEntity();
        entity.setFlowInstanceId(1L);
        entity.setTargetNodeInstanceId(atomicLong.incrementAndGet());
        entity.setSourceNodeInstanceId(atomicLong.incrementAndGet());
        return entity;
    }

    private SequenceInstanceEntity getById(Long id) {
        Optional<SequenceInstanceEntity> optional = repository.findById(id);
        return optional.orElse(null);
    }
}
