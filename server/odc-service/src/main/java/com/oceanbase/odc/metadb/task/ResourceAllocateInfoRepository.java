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
package com.oceanbase.odc.metadb.task;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author yaobin
 * @date 2023-12-06
 * @since 4.2.4
 */
@Repository
public interface ResourceAllocateInfoRepository extends JpaRepository<ResourceAllocateInfoEntity, Long>,
        JpaSpecificationExecutor<ResourceAllocateInfoEntity> {

    @Transactional
    @Query(value = "update resource_allocate_info set "
            + " resource_allocate_state='AVAILABLE', endpoint = :endpointToSet, resource_id = :resourceIdToSet"
            + " where task_id=:idToFind", nativeQuery = true)
    @Modifying
    int updateEndpointByTaskId(@Param("endpointToSet") String endpoint, @Param("resourceIdToSet") Long resourceId,
            @Param("idToFind") Long id);

    @Transactional
    @Query(value = "update resource_allocate_info set "
            + " resource_allocate_state='CREATING_RESOURCE', endpoint = :endpointToSet, resource_id = :resourceIdToSet"
            + " where task_id=:idToFind", nativeQuery = true)
    @Modifying
    int updateResourceIdByTaskId(@Param("endpointToSet") String endpoint, @Param("resourceIdToSet") Long resourceId,
            @Param("idToFind") Long id);

    @Transactional
    @Query(value = "update resource_allocate_info set "
            + " resource_allocate_state=:stateToSet"
            + " where task_id=:idToFind", nativeQuery = true)
    @Modifying
    int updateResourceAllocateStateByTaskId(@Param("stateToSet") String state, @Param("idToFind") Long id);

    @Transactional
    @Query(value = "update resource_allocate_info set "
            + " resource_usage_state=:stateToSet"
            + " where task_id =:idToFind", nativeQuery = true)
    @Modifying
    int updateResourceUsageStateByTaskId(@Param("stateToSet") String state, @Param("idToFind") Long id);


    @Query(value = "SELECT * FROM resource_allocate_info WHERE task_id = ?1", nativeQuery = true)
    Optional<ResourceAllocateInfoEntity> findByTaskIdNative(Long id);

    @Query(value = "SELECT * FROM resource_allocate_info WHERE id = ?1", nativeQuery = true)
    Optional<ResourceAllocateInfoEntity> findByIdNative(Long id);
}
