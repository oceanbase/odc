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
public interface SupervisorEndpointRepository extends JpaRepository<SupervisorEndpointEntity, Long>,
        JpaSpecificationExecutor<SupervisorEndpointEntity> {

    @Transactional
    @Query(value = "update supervisor_endpoint set "
            + " status=:statusToSet, loads = :loadToSet"
            + " where host=:hostToFind and port=:portToFind and resource_id = :resourceIdToFind", nativeQuery = true)
    @Modifying
    int updateStatusLoadByHostPortAndResourceId(@Param("hostToFind") String host, @Param("portToFind") Integer port,
            @Param("resourceIdToFind") Long resourceID,
            @Param("loadToSet") Integer load,
            @Param("statusToSet") String status);

    @Transactional
    @Query(value = "update supervisor_endpoint set "
            + " status=:statusToSet"
            + " where host=:hostToFind and port=:portToFind and resource_id = :resourceIdToFind", nativeQuery = true)
    @Modifying
    int updateStatusByHostPortAndResourceId(@Param("hostToFind") String host, @Param("portToFind") Integer port,
            @Param("resourceIdToFind") Long resourceID,
            @Param("statusToSet") String status);

    @Transactional
    @Query(value = "update supervisor_endpoint set "
            + " status=:statusToSet"
            + " where id=:idToFind", nativeQuery = true)
    @Modifying
    int updateStatusById(@Param("idToFind") Long id,
            @Param("statusToSet") String status);

    @Transactional
    @Query(value = "update supervisor_endpoint set "
            + "loads = loads + :loadToAdd"
            + " where host=:hostToFind and port=:portToFind and resource_id = :resourceIdToFind", nativeQuery = true)
    @Modifying
    int addLoadByHostPortAndResourceId(@Param("hostToFind") String host, @Param("portToFind") Integer port,
            @Param("resourceIdToFind") Long resourceID,
            @Param("loadToAdd") Integer loadToAdd);

    @Query(value = "SELECT * FROM supervisor_endpoint WHERE id = ?1", nativeQuery = true)
    Optional<SupervisorEndpointEntity> findByIdNative(Long id);

    @Query(value = "SELECT * FROM supervisor_endpoint WHERE host = :hostToFind and port = :portToFind and resource_id = :resourceIdToFind",
            nativeQuery = true)
    Optional<SupervisorEndpointEntity> findByHostPortAndResourceId(@Param("hostToFind") String host,
            @Param("portToFind") Integer port, @Param("resourceIdToFind") Long resourceID);
}
