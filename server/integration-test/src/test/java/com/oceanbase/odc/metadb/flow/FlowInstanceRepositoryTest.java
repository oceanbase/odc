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

import java.util.List;
import java.util.Optional;
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
import com.oceanbase.odc.core.shared.constant.FlowStatus;

/**
 * Test cases for {@link FlowInstanceRepository}
 *
 * @author yh263208
 * @date 2022-02-07 14:47
 * @since ODC_release_3.3.0
 */
public class FlowInstanceRepositoryTest extends ServiceTestEnv {

    @Autowired
    private FlowInstanceRepository repository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void clearAll() {
        repository.deleteAll();
    }

    @Test
    public void testSaveFlowInstanceEntity() {
        FlowInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(getById(entity.getId()), entity);
    }

    @Test
    public void testDeleteFlowInstanceEntity() {
        FlowInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.delete(entity);
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testUpdateFlowInstanceById() {
        FlowInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        entity.setStatus(FlowStatus.EXECUTION_SUCCEEDED);
        entity.setName(UUID.randomUUID().toString());
        entity.setProcessDefinitionId(UUID.randomUUID().toString());
        entity.setProcessInstanceId(UUID.randomUUID().toString());
        Assert.assertEquals(1, repository.update(entity));

        Optional<FlowInstanceEntity> newEntity = repository.findById(entity.getId());
        Assert.assertTrue(newEntity.isPresent());
        Assert.assertEquals(entity, newEntity.get());
    }

    @Test
    public void testQueryBySpecification() {
        Long[] ids = insertBatch(100);
        Long id = ids[0];
        Specification<FlowInstanceEntity> specification = Specification.where(FlowInstanceSpecs.idEquals(id));
        List<FlowInstanceEntity> entities = repository.findAll(specification);
        Assert.assertEquals(entities.size(), 1);
        Optional<FlowInstanceEntity> optional = repository.findById(id);
        Assert.assertEquals(optional.orElse(null), entities.get(0));
    }

    @Test
    public void testPagedFindAll() {
        insertBatch(100);
        Specification<FlowInstanceEntity> specification =
                Specification.where(FlowInstanceSpecs.nameEquals("Test Flow Instance"));
        Page<FlowInstanceEntity> entities =
                repository.findAll(specification, PageRequest.of(1, 15, Sort.by(Direction.ASC, "createTime")));
        Assert.assertEquals(entities.getTotalElements(), 100);
        Assert.assertEquals(entities.getSize(), 15);
    }

    private FlowInstanceEntity createEntity() {
        FlowInstanceEntity entity = new FlowInstanceEntity();
        entity.setCreatorId(1L);
        entity.setProjectId(1L);
        entity.setFlowConfigId(2L);
        entity.setFlowConfigSnapshotXml("Test snapshot");
        entity.setName("Test Flow Instance");
        entity.setOrganizationId(1L);
        entity.setProcessDefinitionId("test_process_definition_id");
        entity.setStatus(FlowStatus.CREATED);
        entity.setProcessInstanceId("test_process_instance_id");
        return entity;
    }

    private FlowInstanceEntity getById(Long id) {
        Optional<FlowInstanceEntity> optional = repository.findById(id);
        return optional.orElse(null);
    }

    private Long[] insertBatch(int batchSize) {
        Long[] ids = new Long[batchSize];
        for (int i = 0; i < batchSize; i++) {
            FlowInstanceEntity entity = createEntity();
            repository.save(entity);
            ids[i] = entity.getId();
        }
        return ids;
    }

}
