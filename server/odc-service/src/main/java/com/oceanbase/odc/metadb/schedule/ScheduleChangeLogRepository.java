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

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.schedule.model.ScheduleChangeStatus;

import jakarta.transaction.Transactional;

/**
 * @Author：tinker
 * @Date: 2024/6/8 15:27
 * @Descripition:
 */
public interface ScheduleChangeLogRepository extends OdcJpaRepository<ScheduleChangeLogEntity, Long> {

    List<ScheduleChangeLogEntity> findByScheduleId(Long scheduleId);

    Optional<ScheduleChangeLogEntity> findByIdAndScheduleId(Long id, Long scheduleId);

    Optional<ScheduleChangeLogEntity> findByFlowInstanceId(Long flowInstanceId);


    @Transactional
    @Modifying
    @Query("update ScheduleChangeLogEntity e set e.status = ?2 where e.id = ?1")
    int updateStatusById(Long id, ScheduleChangeStatus status);

    @Transactional
    @Modifying
    @Query("update ScheduleChangeLogEntity e set e.flowInstanceId = ?2 where e.id = ?1")
    int updateFlowInstanceIdById(Long id, Long flowInstanceId);

}
