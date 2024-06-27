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
package com.oceanbase.odc.metadb.schedule;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;

/**
 * @Author：tinker
 * @Date: 2024/6/20 15:44
 * @Descripition:
 */
public class LatestTaskMappingRepositoryTest extends ServiceTestEnv {

    @Autowired
    private LatestTaskMappingRepository repository;

    @Test
    public void listByScheduleIds() {
        LatestTaskMappingEntity entity = create();
        List<LatestTaskMappingEntity> entities = repository.findByScheduleIdIn(Collections.singleton(1L));
        Assert.equals(1, entities.size());
        Assert.equals(entity, entities.get(0));
        delete(entity);
    }

    private LatestTaskMappingEntity create() {
        LatestTaskMappingEntity entity = new LatestTaskMappingEntity();
        entity.setScheduleId(1L);
        entity.setLatestScheduleTaskId(99L);
        return repository.save(entity);
    }

    private void delete(LatestTaskMappingEntity entity) {
        repository.delete(entity);
    }
}
