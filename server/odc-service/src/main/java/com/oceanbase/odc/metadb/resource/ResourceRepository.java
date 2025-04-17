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
package com.oceanbase.odc.metadb.resource;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceState;

import jakarta.transaction.Transactional;

/**
 * jdbc query for resource_resource table
 *
 * @author longpeng.zlp
 * @date 2024/8/14 17:51
 */
public interface ResourceRepository extends JpaRepository<ResourceEntity, Long>,
        JpaSpecificationExecutor<ResourceEntity> {
    @Transactional
    @Query(value = "update `resource_resource` set "
            + " `status`=:#{#status.name()} "
            + " where `region` = :#{#resourceID.getResourceLocation().getRegion()} and `group_name` = :#{#resourceID.getResourceLocation().getGroup()}  "
            + " and `resource_type` = :#{#resourceID.getType()} "
            + " and `namespace` = :#{#resourceID.getNamespace()} and `name` = :#{#resourceID.getIdentifier()}",
            nativeQuery = true)
    @Modifying
    int updateResourceStatus(@Param("resourceID") ResourceID resourceID,
            @Param("status") ResourceState resourceState);

    @Transactional
    @Query(value = "update resource_resource set status=:#{#status.name()} where id=:id", nativeQuery = true)
    @Modifying
    int updateStatusById(@Param("id") Long id, @Param("status") ResourceState status);

    @Transactional
    @Query(value = "update resource_resource set status=:#{#status.name()} where id in (:ids)", nativeQuery = true)
    @Modifying
    int updateStatusByIdIn(@Param("ids") Collection<Long> ids, @Param("status") ResourceState status);

    @Transactional
    @Query(value = "delete from `resource_resource`  "
            + " where `region` = :#{#resourceID.getResourceLocation().getRegion()} and `group_name` = :#{#resourceID.getResourceLocation().getGroup()}  "
            + " and `resource_type` = :#{#resourceID.getType()} "
            + " and `namespace` = :#{#resourceID.getNamespace()} and `name` = :#{#resourceID.getIdentifier()} limit 1",
            nativeQuery = true)
    @Modifying
    int deleteResource(@Param("resourceID") ResourceID resourceID);

    @Query(value = "SELECT * FROM `resource_resource` "
            + "WHERE `region` = :#{#resourceID.getResourceLocation().getRegion()} and `group_name` = :#{#resourceID.getResourceLocation().getGroup()}  "
            + " and `resource_type` = :#{#resourceID.getType()} "
            + " and `namespace` = :#{#resourceID.getNamespace()} and `name` = :#{#resourceID.getIdentifier()}",
            nativeQuery = true)
    Optional<ResourceEntity> findByResourceID(@Param("resourceID") ResourceID resourceID);
}
