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
package com.oceanbase.odc.metadb.partitionplan;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @Author：tianke
 * @Date: 2022/9/16 16:42
 * @Descripition:
 */
public interface DatabasePartitionPlanRepository extends JpaRepository<DatabasePartitionPlanEntity, Long>,
        JpaSpecificationExecutor<DatabasePartitionPlanEntity> {

    @Transactional
    @Modifying
    @Query(value = "update connection_partition_plan set is_config_enabled = true where flow_instance_id=:flowInstanceId",
            nativeQuery = true)
    int enableConfigByFlowInstanceId(@Param("flowInstanceId") Long flowInstanceId);

    @Query(value = "select * from connection_partition_plan "
            + "where database_id=:databaseId and is_config_enabled=true", nativeQuery = true)
    Optional<DatabasePartitionPlanEntity> findValidPlanByDatabaseId(@Param("databaseId") Long databaseId);

    Optional<DatabasePartitionPlanEntity> findByFlowInstanceId(Long flowInstanceId);

    @Transactional
    @Modifying
    @Query(value = "update connection_partition_plan set is_config_enabled = false "
            + "where database_id=:databaseId", nativeQuery = true)
    int disableConfigByDatabaseId(@Param("databaseId") Long databaseId);

    @Transactional
    @Modifying
    @Query(value = "update connection_partition_plan set schedule_id =:scheduleId "
            + "where id=:id", nativeQuery = true)
    int updateScheduleIdById(@Param("id") Long id, @Param("scheduleId") Long scheduleId);

}
