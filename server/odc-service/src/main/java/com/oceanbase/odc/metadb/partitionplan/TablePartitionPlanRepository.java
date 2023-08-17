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
import java.util.Optional;

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
            + "where is_config_enabled = true and connection_id=:id", nativeQuery = true)
    int disableConfigByConnectionId(@Param("id") Long connectionId);

    @Transactional
    @Modifying
    @Query(value = "update table_partition_plan set is_config_enabled = true "
            + "where flow_instance_id=:flowInstanceId", nativeQuery = true)
    int enableConfigByFlowInstanceId(@Param("flowInstanceId") Long flowInstanceId);


    @Query(value = "select * from table_partition_plan where connection_id=:id and schema_name=:schemaName"
            + " and table_name=:tableName and is_config_enabled=true", nativeQuery = true)
    Optional<TablePartitionPlanEntity> findEnTablePlan(@Param("id") Long connectionId,
            @Param("schemaName") String schemaName,
            @Param("tableName") String tableName);


    @Query(value = "select * from table_partition_plan where flow_instance_id=:flowInstanceId and is_config_enabled = true and is_auto_partition = true",
            nativeQuery = true)
    List<TablePartitionPlanEntity> findValidPlanByFlowInstanceId(@Param("flowInstanceId") Long flowInstanceId);

    @Query(value = "select * from table_partition_plan ap "
            + "where ap.connection_id=:connectionId and ap.is_config_enabled = true", nativeQuery = true)
    List<TablePartitionPlanEntity> findValidPlanByConnectionId(@Param("connectionId") Long connectionId);

    List<TablePartitionPlanEntity> findByFlowInstanceId(Long flowInstanceId);

}
