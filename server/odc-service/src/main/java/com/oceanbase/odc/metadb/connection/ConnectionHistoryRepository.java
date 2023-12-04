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

package com.oceanbase.odc.metadb.connection;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author gaoda.xy
 * @date 2023/12/4 20:57
 */
public interface ConnectionHistoryRepository extends JpaRepository<ConnectionHistoryEntity, Long>,
        JpaSpecificationExecutor<ConnectionHistoryEntity> {

    @Transactional
    int deleteByConnectionId(Long connectionId);

    @Modifying
    @Transactional
    @Query(value = "insert into `connect_connection_access` (`connection_id`, `user_id`, `last_access_time`, "
            + "`create_time`, `update_time`) values (:connectionId, :userId, :lastAccessTime, now(), now()) "
            + "on duplicate key update last_access_time = greatest(`last_access_time`, :lastAccessTime)",
            nativeQuery = true)
    int updateOrInsert(@Param("connectionId") Long connectionId, @Param("userId") Long userId,
            @Param("lastAccessTime") Date lastAccessTime);

    @Query(value = "select max(a.`id`) as id, c.`id` as connection_id, c.`creator_id` as user_id, "
            + "coalesce(max(a.`last_access_time`), c.`update_time`) as last_access_time, coalesce(max(a.`create_time`), "
            + "c.`create_time`) as create_time, coalesce(max(a.`update_time`), c.`update_time`) as update_time from "
            + "connect_connection c left join connect_connection_access a on c.id=a.connection_id group by c.`id` "
            + "having last_access_time < timestampadd(second, -(:intervalSeconds), current_timestamp)",
            nativeQuery = true)
    List<ConnectionHistoryEntity> listInactiveConnections(@Param("intervalSeconds") Integer intervalSeconds);

    @Query(value = "select max(a.`id`) as id, c.`id` as connection_id, c.`creator_id` as user_id, "
            + "coalesce(max(a.`last_access_time`), c.`update_time`) as last_access_time, coalesce(max(a.`create_time`), "
            + "c.`create_time`) as create_time, coalesce(max(a.`update_time`), c.`update_time`) as update_time from "
            + "connect_connection c left join connect_connection_access a on c.id=a.connection_id where c.is_temp=1 "
            + "group by c.`id` having last_access_time < timestampadd(second, -:intervalSeconds, current_timestamp)",
            nativeQuery = true)
    List<ConnectionHistoryEntity> listInactiveTempConnections(@Param("intervalSeconds") Integer intervalSeconds);

}
