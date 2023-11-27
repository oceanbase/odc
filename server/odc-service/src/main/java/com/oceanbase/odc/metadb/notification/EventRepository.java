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
package com.oceanbase.odc.metadb.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.service.notification.model.EventStatus;

public interface EventRepository extends JpaRepository<EventEntity, Long>,
        JpaSpecificationExecutor<EventEntity> {
    @Query(value = "update notification_event set `status`=:#{#status.name()} where `id`=:id", nativeQuery = true)
    @Transactional
    @Modifying
    int updateStatusById(@Param("id") Long id, @Param("status") EventStatus status);

    @Query(value = "update notification_event set `status`=:#{#status.name()}, retry_times=retry_times+1 where `id`=:id",
            nativeQuery = true)
    @Transactional
    @Modifying
    int updateStatusAndRetryTimesById(@Param("id") Long id, @Param("status") EventStatus status);


    @Query(value = "select * from notification_event where `status`=:#{#status.name()} order by trigger_time limit 1 for update nowait",
            nativeQuery = true)
    Optional<EventEntity> findByStatusForUpdate(@Param("status") EventStatus status);

    @Query(value = "select * from notification_event where `status`=:#{#status.name()} order by trigger_time limit :limit for update nowait",
            nativeQuery = true)
    List<EventEntity> findNByStatusForUpdate(@Param("status") EventStatus status, @Param("limit") Integer limit);

    @Transactional
    @Modifying
    @Query(value = "update notification_event set `status`=:#{#status.name()} where `id` in (:ids)", nativeQuery = true)
    int updateStatusByIds(@Param("status") EventStatus status, @Param("ids") Collection<Long> ids);


}
