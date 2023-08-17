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
package com.oceanbase.odc.metadb.resourcegroup;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.transaction.Transactional;
import javax.validation.constraints.NotEmpty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository object for <code>ResourceGroupConnectionEntity</code>
 *
 * @author yh263208
 * @date 2021-07-27 16:08
 * @since ODC_release_3.2.0
 */
public interface ResourceGroupConnectionRepository extends JpaRepository<ResourceGroupConnectionEntity, Long>,
        JpaSpecificationExecutor<ResourceGroupConnectionEntity> {
    List<ResourceGroupConnectionEntity> findByResourceTypeAndResourceId(String resourceType, Long resourceId);

    List<ResourceGroupConnectionEntity> findByResourceGroupId(Long groupId);

    @Transactional
    @Query("delete from ResourceGroupConnectionEntity as rgc"
            + " where rgc.resourceType=:resourceType AND rgc.resourceId=:resourceId")
    @Modifying
    int deleteByResourceTypeAndResourceId(@Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId);

    @Transactional
    @Query("delete from ResourceGroupConnectionEntity as rgc"
            + " where rgc.resourceType=:resourceType AND rgc.resourceId in (:resourceIds)")
    @Modifying
    int deleteByResourceTypeAndResourceIds(@Param("resourceType") String resourceType,
            @Param("resourceIds") Set<Long> resourceIds);

    @Transactional
    @Query("delete from ResourceGroupConnectionEntity as rgc where rgc.resourceGroupId=:resourceGroupId")
    @Modifying
    int deleteByResourceGroupId(@Param("resourceGroupId") Long resourceGroupId);

    @Transactional
    @Query("delete from ResourceGroupConnectionEntity as rgc"
            + " where rgc.resourceGroupId in :resourceGroupIds"
            + " and rgc.resourceType=:resourceType and rgc.resourceId=:resourceId")
    @Modifying
    int deleteByResourceGroupIdInAndResourceTypeAndResourceId(
            @NotEmpty @Param("resourceGroupIds") Collection<Long> resourceGroupIds,
            @Param("resourceType") String resourceType, @Param("resourceId") Long resourceId);

    @Query("select distinct(rgc.resourceId) from ResourceGroupConnectionEntity as rgc"
            + " left join ResourceGroupEntity as rg on rg.id = rgc.resourceGroupId"
            + " where rg.organizationId=:organizationId and rgc.resourceType=:resourceType")
    List<Long> findResourceIdUnderAllResourceGroupOfOrg(@Param("organizationId") Long organizationId,
            @Param("resourceType") String resourceType);

}
