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
 * @Author：tianke
 * @Date: 2022/9/16 16:42
 * @Descripition:
 */
public interface ConnectionPartitionPlanRepository extends JpaRepository<ConnectionPartitionPlanEntity, Long>,
        JpaSpecificationExecutor<ConnectionPartitionPlanEntity> {

    @Transactional
    @Modifying
    @Query(value = "update connection_partition_plan set is_config_enabled = true where flow_instance_id=:flowInstanceId",
            nativeQuery = true)
    int enableConfigByFlowInstanceId(@Param("flowInstanceId") Long flowInstanceId);

    @Query(value = "select * from connection_partition_plan "
            + "where connection_id=:connectionId and is_config_enabled=true", nativeQuery = true)
    Optional<ConnectionPartitionPlanEntity> findValidPlanByConnectionId(@Param("connectionId") Long connectionId);

    Optional<ConnectionPartitionPlanEntity> findByFlowInstanceId(Long flowInstanceId);

    @Query(value = "select * from connection_partition_plan "
            + "where is_config_enabled=true", nativeQuery = true)
    List<ConnectionPartitionPlanEntity> findAllValidPlan();

    @Transactional
    @Modifying
    @Query(value = "update connection_partition_plan set is_config_enabled = false "
            + "where connection_id=:connectionId", nativeQuery = true)
    int disableConfigByConnectionId(@Param("connectionId") Long connectionId);

}
