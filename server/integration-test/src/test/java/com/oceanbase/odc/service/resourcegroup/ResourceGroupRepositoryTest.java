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
package com.oceanbase.odc.service.resourcegroup;

import java.util.List;
import java.util.Optional;
import java.util.Random;

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
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupRepository;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupSpecs;

/**
 * Test object for <code>ResourceGroupRepository</code>
 *
 * @author yh263208
 * @date 2021-07-27 11:29
 * @since ODC_release_3.2.0
 */
public class ResourceGroupRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ResourceGroupRepository repository;

    @Before
    public void setUp() {
        repository.deleteAll();
    }

    @After
    public void clearAll() {
        repository.deleteAll();
    }

    @Test
    public void testSaveResourceGroupEntity() {
        ResourceGroupEntity entity = getEntity();
        repository.save(entity);
        Assert.assertEquals(getById(entity.getId()), entity);
    }

    @Test
    public void testDeleteResourceGroupEntity() {
        ResourceGroupEntity entity = getEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.delete(entity);
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testUpdateResurceGroup() {
        ResourceGroupEntity entity = getEntity();
        repository.save(entity);
        modify(entity, "Testing", "Modify");
        Assert.assertEquals(repository.updateById(entity), 1);
        Optional<ResourceGroupEntity> optional = repository.findById(entity.getId());
        Assert.assertEquals(entity, optional.orElse(null));
    }

    @Test
    public void testQueryBySpecification() {
        Long[] ids = insertBatch(100);
        Long id = ids[0];
        Specification<ResourceGroupEntity> specification =
                Specification.where(ResourceGroupSpecs.idEqual(id));
        List<ResourceGroupEntity> entities = repository.findAll(specification);
        Assert.assertEquals(entities.size(), 1);
        Optional<ResourceGroupEntity> optional = repository.findById(id);
        Assert.assertEquals(optional.orElse(null), entities.get(0));
    }

    @Test
    public void testPagedFindAll() {
        insertBatch(100);
        Specification<ResourceGroupEntity> specification =
                Specification.where(ResourceGroupSpecs.nameEqual("This is a test resource group"));
        Page<ResourceGroupEntity> entities =
                repository.findAll(specification, PageRequest.of(1, 15, Sort.by(Direction.ASC, "createTime")));
        Assert.assertEquals(entities.getTotalElements(), 100);
        Assert.assertEquals(entities.getSize(), 15);
    }

    private ResourceGroupEntity getEntity() {
        ResourceGroupEntity entity = new ResourceGroupEntity();
        entity.setDescription("This is a Test object");
        entity.setOrganizationId(new Random().nextLong());
        entity.setCreatorId(new Random().nextInt());
        entity.setName("This is a test resource group");
        entity.setEnabled(false);
        return entity;
    }

    private ResourceGroupEntity getById(Long id) {
        Optional<ResourceGroupEntity> optional = repository.findById(id);
        return optional.orElse(null);
    }

    private Long[] insertBatch(int batchSize) {
        Long[] ids = new Long[batchSize];
        for (int i = 0; i < batchSize; i++) {
            ResourceGroupEntity entity = getEntity();
            repository.save(entity);
            ids[i] = entity.getId();
        }
        return ids;
    }

    private void modify(ResourceGroupEntity entity, String newName, String newDesp) {
        entity.setName(newName);
        entity.setEnabled(!entity.isEnabled());
        entity.setDescription(newDesp);
    }

}
