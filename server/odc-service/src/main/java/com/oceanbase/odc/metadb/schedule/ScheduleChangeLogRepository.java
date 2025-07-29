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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // query latest schedule change log by schedule ids for list schedules operation
    @Query(value = "select a.id as id , a.schedule_id as schedule_id, a.flow_instance_id as flow_instance_id, a.status as status, a.create_time as create_time from schedule_changelog as a inner join  (select schedule_id, max(create_time) as latest_time from schedule_changelog where schedule_id in (:scheduleIds) group by schedule_id) as b on a.create_time = b.latest_time  and a.schedule_id = b.schedule_id",
            nativeQuery = true)
    List<ScheduleChangeLogSummary> findLatestScheduleChangeByScheduleIDs(
            @Param("scheduleIds") Collection<Long> scheduleIds);

    // query latest schedule change log by schedule changelog ids for list schedules operation
    @Query(value = "select  id,  schedule_id, flow_instance_id, status, create_time from schedule_changelog where id in (:scheduleChangeLogIds)",
            nativeQuery = true)
    List<ScheduleChangeLogSummary> findScheduleChangeByScheduleChangeLogIDs(
            @Param("scheduleChangeLogIds") Collection<Long> scheduleChangeLogIds);

    interface ScheduleChangeLogSummary {
        Long getId();

        Long getScheduleId();

        Long getFlowInstanceId();

        ScheduleChangeStatus getStatus();

        Date getCreateTime();
    }
}
