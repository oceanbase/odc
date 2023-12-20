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
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.FlowTaskExecutionStrategy;

/**
 * Test cases for {@link ServiceTaskInstanceRepository}
 *
 * @author yh263208
 * @date 2022-02-15 11:55
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class ServiceTaskInstanceRepositoryTest extends ServiceTestEnv {
    @Autowired
    private ServiceTaskInstanceRepository repository;
    private final Random random = new Random();
    @Autowired
    private NodeInstanceEntityRepository nodeInstanceEntityRepository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void clearAll() {
        repository.deleteAll();
    }

    @Test
    public void testSaveSerivceTaskInstanceEntity() {
        ServiceTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(getById(entity.getId()), entity);
    }

    @Test
    public void bulkSave_saveSeveralEntities_saveSucceed() {
        List<ServiceTaskInstanceEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(createEntity());
        }
        repository.batchCreate(entities);
        Assert.assertEquals(10, repository.findAll().size());
    }

    @Test
    public void save_ServiceTaskAndNodeInstanceEntity_ExpectGetObjectByActivityId() {
        ServiceTaskInstanceEntity entity = createEntity();
        repository.save(entity);

        NodeInstanceEntity nodeInstanceEntity = createNodeEntity(entity.getFlowInstanceId(), entity.getId());
        Optional<ServiceTaskInstanceEntity> optional = repository.findByInstanceTypeAndActivityId(
                FlowNodeType.SERVICE_TASK, nodeInstanceEntity.getActivityId(), entity.getFlowInstanceId());
        Assert.assertTrue(optional.isPresent());
    }

    @Test
    public void save_ServiceTaskAndNodeInstanceEntity_ExpectGetObjectByName() {
        ServiceTaskInstanceEntity entity = createEntity();
        repository.save(entity);

        NodeInstanceEntity nodeInstanceEntity = createNodeEntity(entity.getFlowInstanceId(), entity.getId());
        Optional<ServiceTaskInstanceEntity> optional = repository.findByInstanceTypeAndName(FlowNodeType.SERVICE_TASK,
                nodeInstanceEntity.getName(), entity.getFlowInstanceId());
        Assert.assertTrue(optional.isPresent());
    }

    @Test
    public void testDeleteServiceTaskInstanceEntity() {
        ServiceTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.delete(entity);
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testDeleteServiceTaskInstanceEntityByFlowInstanceId() {
        ServiceTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.deleteByFlowInstanceId(entity.getFlowInstanceId());
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testQueryBySpecification() {
        Long[] ids = insertBatch(100);
        Long id = ids[0];
        Specification<ServiceTaskInstanceEntity> specification =
                Specification.where(ServiceTaskInstanceSpecs.idEquals(id));
        List<ServiceTaskInstanceEntity> entities = repository.findAll(specification);
        Assert.assertEquals(entities.size(), 1);
        Optional<ServiceTaskInstanceEntity> optional = repository.findById(id);
        Assert.assertEquals(optional.orElse(null), entities.get(0));
    }

    @Test
    public void testPagedFindAll() {
        insertBatch(100);
        Specification<ServiceTaskInstanceEntity> specification = Specification
                .where(ServiceTaskInstanceSpecs.statusIn(Collections.singletonList(FlowNodeStatus.CREATED)));
        Page<ServiceTaskInstanceEntity> entities =
                repository.findAll(specification, PageRequest.of(1, 15, Sort.by(Direction.ASC, "createTime")));
        Assert.assertEquals(entities.getTotalElements(), 100);
        Assert.assertEquals(entities.getSize(), 15);
    }

    @Test
    public void testUpdateServiceTaskInstanceEntityById() {
        ServiceTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        entity.setStatus(FlowNodeStatus.FAILED);
        entity.setTargetTaskId(random.nextLong());
        Assert.assertEquals(1, repository.update(entity));

        Optional<ServiceTaskInstanceEntity> newEntity = repository.findById(entity.getId());
        Assert.assertTrue(newEntity.isPresent());
        Assert.assertEquals(entity, newEntity.get());
    }

    private NodeInstanceEntity createNodeEntity(Long flowInstanceId, Long instanceId) {
        NodeInstanceEntity entity = new NodeInstanceEntity();
        entity.setFlowInstanceId(flowInstanceId);
        entity.setInstanceId(instanceId);
        entity.setInstanceType(FlowNodeType.SERVICE_TASK);
        entity.setActivityId(UUID.randomUUID().toString());
        entity.setName(UUID.randomUUID().toString());
        entity.setFlowableElementType(FlowableElementType.SERVICE_TASK);
        entity = nodeInstanceEntityRepository.save(entity);
        return entity;
    }

    private ServiceTaskInstanceEntity createEntity() {
        ServiceTaskInstanceEntity entity = new ServiceTaskInstanceEntity();
        entity.setTargetTaskId(12L);
        entity.setFlowInstanceId(random.nextLong());
        entity.setOrganizationId(1L);
        entity.setStatus(FlowNodeStatus.CREATED);
        entity.setStrategy(FlowTaskExecutionStrategy.AUTO);
        entity.setTaskType(TaskType.ASYNC);
        entity.setWaitExecExpireIntervalSeconds(15);
        return entity;
    }

    private ServiceTaskInstanceEntity getById(Long id) {
        Optional<ServiceTaskInstanceEntity> optional = repository.findById(id);
        return optional.orElse(null);
    }

    private Long[] insertBatch(int batchSize) {
        Long[] ids = new Long[batchSize];
        for (int i = 0; i < batchSize; i++) {
            ServiceTaskInstanceEntity entity = createEntity();
            repository.save(entity);
            ids[i] = entity.getId();
        }
        return ids;
    }
}
