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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionEntity;
import com.oceanbase.odc.metadb.resourcegroup.ResourceGroupConnectionRepository;
import com.oceanbase.odc.test.tool.TestRandom;

/**
 * {@link ResourceGroupConnectionRepositoryTest}
 *
 * @author yh263208
 * @date 2022-12-20 21:48
 * @since ODC_release_4.1.0
 */
public class ResourceGroupConnectionRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ResourceGroupConnectionRepository repository;

    @Before
    public void init() {
        repository.deleteAll();
    }

    @After
    public void clear() {
        repository.deleteAll();
    }

    @Test
    public void deleteByResourceTypeAndResourceIds_serveralEntityExist_deleteSucceed() {
        ResourceGroupConnectionEntity e1 = getEntity();
        e1.setResourceType(ResourceType.ODC_CONNECTION.name());
        e1.setResourceId(1L);
        ResourceGroupConnectionEntity e2 = getEntity();
        e2.setResourceType(ResourceType.ODC_CONNECTION.name());
        e2.setResourceId(2L);
        ResourceGroupConnectionEntity e3 = getEntity();
        e3.setResourceType(ResourceType.ODC_PRIVATE_CONNECTION.name());
        e3.setResourceId(1L);

        repository.saveAll(Arrays.asList(e1, e2, e3));
        int affectRows = repository.deleteByResourceTypeAndResourceIds(
                ResourceType.ODC_CONNECTION.name(), new HashSet<>(Arrays.asList(1L, 2L)));
        Assert.assertEquals(2, affectRows);
    }

    private ResourceGroupConnectionEntity getEntity() {
        ResourceGroupConnectionEntity entity = TestRandom.nextObject(ResourceGroupConnectionEntity.class);
        entity.setId(null);
        return entity;
    }
}
