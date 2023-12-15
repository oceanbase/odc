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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

/**
 * Test cases for {@link NodeInstanceEntityRepository}
 *
 * @author yh263208
 * @date 2022-02-09 19:46
 * @since ODC_release_3.3.0
 */
public class NodeInstanceEntityRepositoryTest extends ServiceTestEnv {

    @Autowired
    private NodeInstanceEntityRepository repository;
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
    public void testSaveNodeInstanceEntity() {
        NodeInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(getById(entity.getId()), entity);
    }

    @Test
    public void bulkSave_saveSeveralEntities_saveSucceed() {
        List<NodeInstanceEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(createEntity());
        }
        repository.batchCreate(entities);
        Assert.assertEquals(10, repository.findAll().size());
    }

    @Test
    public void testDeleteNodeInstanceEntity() {
        NodeInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.delete(entity);
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testFindByInstanceIdAndInstanceType() {
        NodeInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        List<NodeInstanceEntity> optional =
                repository.findByInstanceIdAndInstanceTypeAndFlowableElementType(entity.getId(),
                        entity.getInstanceType(),
                        entity.getFlowableElementType());
        Assert.assertTrue(optional.isEmpty());
        optional = repository.findByInstanceIdAndInstanceTypeAndFlowableElementType(entity.getInstanceId(),
                entity.getInstanceType(), entity.getFlowableElementType());
        Assert.assertFalse(optional.isEmpty());

        Assert.assertEquals(entity, optional.get(0));
    }

    @Test
    public void testDeleteByflowInstanceId() {
        NodeInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        Assert.assertEquals(1, repository.deleteByFlowInstanceId(entity.getFlowInstanceId()));

        Assert.assertEquals(0, repository.findAll().size());
    }

    @Test
    public void testDeleteByInstanceIdAndInstanceType() {
        NodeInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        Assert.assertEquals(0, repository.deleteByInstanceIdAndInstanceType(entity.getId(), entity.getInstanceType()));
        Assert.assertEquals(1,
                repository.deleteByInstanceIdAndInstanceType(entity.getInstanceId(), entity.getInstanceType()));

        Assert.assertEquals(0, repository.findAll().size());
    }

    @Test
    public void findByflowInstanceId_recordExists_findSucceed() {
        NodeInstanceEntity entity = createEntity();
        entity = repository.save(entity);

        List<NodeInstanceEntity> actual = repository.findByFlowInstanceId(entity.getFlowInstanceId());
        Assert.assertEquals(Collections.singletonList(entity), actual);
    }

    private NodeInstanceEntity createEntity() {
        NodeInstanceEntity entity = new NodeInstanceEntity();
        entity.setFlowInstanceId(1L);
        entity.setInstanceId(atomicLong.incrementAndGet());
        entity.setInstanceType(FlowNodeType.SERVICE_TASK);
        entity.setActivityId(UUID.randomUUID().toString());
        entity.setName(UUID.randomUUID().toString());
        entity.setFlowableElementType(FlowableElementType.SERVICE_TASK);
        return entity;
    }

    private NodeInstanceEntity getById(Long id) {
        Optional<NodeInstanceEntity> optional = repository.findById(id);
        return optional.orElse(null);
    }

}
