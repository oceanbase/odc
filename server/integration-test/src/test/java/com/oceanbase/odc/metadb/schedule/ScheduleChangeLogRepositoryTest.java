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

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.schedule.model.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;

/**
 * @Author：tinker
 * @Date: 2024/6/20 15:44
 * @Descripition:
 */
public class ScheduleChangeLogRepositoryTest extends ServiceTestEnv {

    @Autowired
    private ScheduleChangeLogRepository repository;

    @Test
    public void findByScheduleId() {
        ScheduleChangeLogEntity entity = create();
        List<ScheduleChangeLogEntity> byScheduleId = repository.findByScheduleId(entity.getScheduleId());
        Assert.equals(1, byScheduleId.size());
        Assert.equals(entity.getScheduleId(), byScheduleId.get(0).getScheduleId());
        delete(entity);
    }

    @Test
    public void findByIdAndScheduleId() {
        ScheduleChangeLogEntity entity = create();
        Optional<ScheduleChangeLogEntity> optional =
                repository.findByIdAndScheduleId(entity.getId(), entity.getScheduleId());
        Assert.isTrue(optional.isPresent());
        Assert.equals(entity.getScheduleId(), optional.get().getScheduleId());
        delete(entity);
    }

    private ScheduleChangeLogEntity create() {
        ScheduleChangeLogEntity entity = new ScheduleChangeLogEntity();
        entity.setType(OperationType.CREATE);
        entity.setScheduleId(1L);
        entity.setStatus(ScheduleChangeStatus.PREPARING);
        return repository.save(entity);
    }

    private void delete(ScheduleChangeLogEntity entity) {
        repository.delete(entity);
    }


}
