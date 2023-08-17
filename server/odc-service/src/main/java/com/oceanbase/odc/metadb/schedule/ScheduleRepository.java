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

import java.util.Set;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.service.schedule.model.QueryScheduleParams;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;

/**
 * @Author：tinker
 * @Date: 2022/11/16 14:51
 * @Descripition:
 */
public interface ScheduleRepository extends OdcJpaRepository<ScheduleEntity, Long> {

    @Transactional
    @Modifying
    @Query(value = "update schedule_schedule set status = :#{#status.name()} "
            + "where id=:id", nativeQuery = true)
    int updateStatusById(@Param("id") Long id, @Param("status") ScheduleStatus status);

    @Query("select e.creatorId from ScheduleEntity e where e.id=:id")
    Set<Long> findCreatorIdById(@Param("id") Long id);


    default Page<ScheduleEntity> find(@NotNull Pageable pageable, @NotNull QueryScheduleParams params) {
        Specification<ScheduleEntity> specification = Specification
                .where(OdcJpaRepository.between(ScheduleEntity_.createTime, params.getStartTime(), params.getEndTime())
                        .and(OdcJpaRepository.eq(ScheduleEntity_.jobType, params.getType()))
                        .and(OdcJpaRepository.in(ScheduleEntity_.projectId, params.getProjectIds()))
                        .and(OdcJpaRepository.in(ScheduleEntity_.creatorId, params.getCreatorIds()))
                        .and(OdcJpaRepository.eq(ScheduleEntity_.id, params.getId()))
                        .and(OdcJpaRepository.in(ScheduleEntity_.status, params.getStatuses())))
                .and(OdcJpaRepository.eq(ScheduleEntity_.organizationId, params.getOrganizationId()));
        return findAll(specification, pageable);
    }
}
