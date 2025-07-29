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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.schedule.model.ScheduleType;

import jakarta.transaction.Transactional;

public interface ScheduleRelationsRepository extends OdcJpaRepository<ScheduleRelationsEntity, Long> {

    Optional<ScheduleRelationsEntity> findByParentId(Long parentId);

    List<ScheduleRelationsEntity> findByParentIdIn(Collection<Long> parentIds);

    @Transactional
    default ScheduleRelationsEntity insertRelation(Long parentId, Long childId, ScheduleType scheduleType) {
        ScheduleRelationsEntity entity = new ScheduleRelationsEntity();
        entity.setParentId(parentId);
        entity.setChildId(childId);
        entity.setScheduleType(scheduleType);
        return save(entity);
    }

}
