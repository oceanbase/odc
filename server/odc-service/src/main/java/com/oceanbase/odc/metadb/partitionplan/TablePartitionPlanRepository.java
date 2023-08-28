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

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @Author：tinker
 * @Date: 2022/9/16 16:42
 * @Descripition:
 */
public interface TablePartitionPlanRepository extends JpaRepository<TablePartitionPlanEntity, Long>,
        JpaSpecificationExecutor<TablePartitionPlanEntity> {

    @Transactional
    @Modifying
    @Query(value = "update table_partition_plan set is_config_enabled = false "
            + "where is_config_enabled = true and database_id=:id", nativeQuery = true)
    int disableConfigByDatabaseId(@Param("id") Long databaseId);

    @Transactional
    @Modifying
    @Query(value = "update table_partition_plan set is_config_enabled = true "
            + "where database_partition_plan_id=:databasePartitionPlanId", nativeQuery = true)
    int enableConfigByDatabasePartitionPlanId(@Param("databasePartitionPlanId") Long databasePartitionPlanId);

    @Query(value = "select * from table_partition_plan "
            + "where database_partition_plan_id=:id and is_config_enabled = true", nativeQuery = true)
    List<TablePartitionPlanEntity> findValidPlanByDatabasePartitionPlanId(@Param("id") Long databasePartitionPlanId);

    @Query(value = "select * from table_partition_plan "
            + "where database_id=:id and is_config_enabled = true", nativeQuery = true)
    List<TablePartitionPlanEntity> findValidPlanByDatabaseId(@Param("id") Long databaseId);

    List<TablePartitionPlanEntity> findByDatabasePartitionPlanId(@Param("id") Long databasePartitionPlanId);
}
