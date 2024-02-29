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
package com.oceanbase.odc.metadb.iam.resourcerole;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserResourceRoleRepository
        extends JpaRepository<UserResourceRoleEntity, Long>, JpaSpecificationExecutor<UserResourceRoleEntity> {
    List<UserResourceRoleEntity> findByOrganizationIdAndUserId(Long organizationId, Long userId);


    List<UserResourceRoleEntity> findByUserId(Long userId);

    // List<UserResourceRoleEntity> findByResourceTypeAndUserId(Long userId);

    List<UserResourceRoleEntity> findByResourceId(Long resourceId);

    List<UserResourceRoleEntity> findByResourceIdIn(Collection<Long> resourceIds);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role t where t.user_id =:userId", nativeQuery = true)
    int deleteByUserId(@Param("userId") Long userId);

    @Query(nativeQuery = true,
            value = "select t.* from iam_user_resource_role t where concat(t.resource_id, ':',  t.resource_role_id) in (:resourceRoleIdentifiers)")
    List<UserResourceRoleEntity> findByResourceIdsAndResourceRoleIdsIn(
            @Param("resourceRoleIdentifiers") Set<String> resourceRoleIdentifiers);


    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role t where t.resource_id =:resourceId", nativeQuery = true)
    int deleteByResourceId(@Param("resourceId") Long resourceId);

    @Modifying
    @Transactional
    @Query(value = "delete t from iam_user_resource_role t,iam_resource_role irr where t.resource_role_id = irr.id and irr.resource_type = :resourceType and  t.resource_id =:resourceId",
            nativeQuery = true)
    int deleteByResourceTypeAndId(@Param("resourceType") String resourceType, @Param("resourceId") Long resourceId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role t where t.resource_id =:resourceId and t.user_id =:userId",
            nativeQuery = true)
    int deleteByResourceIdAndUserId(@Param("resourceId") Long resourceId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role t where t.resource_id in (:resourceIds) and t.user_id =:userId",
            nativeQuery = true)
    int deleteByUserIdAndResourceIdIn(@Param("userId") Long userId, @Param("resourceIds") Set<Long> resourceIds);

    @Query(nativeQuery = true,
            value = "select t.* from iam_user_resource_role t,iam_resource_role irr where t.resource_role_id = irr.id and irr.resource_type = :resourceType and t.resource_id = :resourceId")
    List<UserResourceRoleEntity> listByResourceTypeAndId(
            @Param("resourceType") String resourceType, @Param("resourceId") Long resourceId);

    @Query(nativeQuery = true,
            value = "select t.* from iam_user_resource_role t,iam_resource_role irr where t.resource_role_id = irr.id and irr.resource_type = :resourceType and irr.role_name = :roleName and t.resource_id = :resourceId")
    List<UserResourceRoleEntity> findByProjectIdAndResourceRole(
            @Param("resourceId") Long resourceId, @Param("resourceType") String resourceType,
            @Param("roleName") String roleName);

}
