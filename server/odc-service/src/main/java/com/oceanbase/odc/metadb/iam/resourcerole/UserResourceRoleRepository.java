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
import java.util.function.Function;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.ResourceType;

public interface UserResourceRoleRepository extends OdcJpaRepository<UserResourceRoleEntity, Long> {

    List<UserResourceRoleEntity> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    List<UserResourceRoleEntity> findByUserId(Long userId);

    List<UserResourceRoleEntity> findByResourceId(Long resourceId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role t where t.user_id =:userId", nativeQuery = true)
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role t where t.resource_id =:resourceId", nativeQuery = true)
    int deleteByResourceId(@Param("resourceId") Long resourceId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role i_urr where i_urr.resource_id = :resourceId and i_urr.resource_role_id "
            + "in (select i_rr.id from iam_resource_role i_rr where i_rr.resource_type = :#{#resourceType.name()})",
            nativeQuery = true)
    int deleteByResourceTypeAndId(@Param("resourceType") ResourceType resourceType,
            @Param("resourceId") Long resourceId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role i_urr where i_urr.resource_id in (:resourceIds) and i_urr.resource_role_id "
            + "in (select i_rr.id from iam_resource_role i_rr where i_rr.resource_type = :#{#resourceType.name()})",
            nativeQuery = true)
    int deleteByResourceTypeAndIdIn(@Param("resourceType") ResourceType resourceType,
            @Param("resourceIds") Collection<Long> resourceIds);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role i_urr where i_urr.user_id = :userId and i_urr.resource_id = :resourceId "
            + "and i_urr.resource_role_id in (select i_rr.id from iam_resource_role i_rr where i_rr.resource_type = :#{#resourceType.name()})",
            nativeQuery = true)
    int deleteByUserIdAndResourceIdAndResourceType(@Param("userId") Long userId, @Param("resourceId") Long resourceId,
            @Param("resourceType") ResourceType resourceType);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_resource_role i_urr where i_urr.user_id = :userId and i_urr.resource_id in (:resourceIds) "
            + "and i_urr.resource_role_id in (select i_rr.id from iam_resource_role i_rr where i_rr.resource_type = :#{#resourceType.name()})",
            nativeQuery = true)
    int deleteByUserIdAndResourceIdInAndResourceType(@Param("userId") Long userId,
            @Param("resourceIds") Collection<Long> resourceIds, @Param("resourceType") ResourceType resourceType);

    @Query(nativeQuery = true,
            value = "select t.* from iam_user_resource_role t where concat(t.resource_id, ':',  t.resource_role_id) in (:resourceRoleIdentifiers)")
    List<UserResourceRoleEntity> findByResourceIdsAndResourceRoleIdsIn(
            @Param("resourceRoleIdentifiers") Set<String> resourceRoleIdentifiers);

    @Query(value = "select i_urr.* from iam_user_resource_role i_urr inner join iam_resource_role i_rr on "
            + "i_urr.resource_role_id = i_rr.id where i_rr.resource_type = :#{#resourceType.name()} and i_urr.resource_id = :resourceId",
            nativeQuery = true)
    List<UserResourceRoleEntity> listByResourceTypeAndId(
            @Param("resourceType") ResourceType resourceType, @Param("resourceId") Long resourceId);

    @Query(value = "select i_urr.* from iam_user_resource_role i_urr inner join iam_resource_role i_rr on "
            + "i_urr.resource_role_id = i_rr.id where i_rr.resource_type = :#{#resourceType.name()} and i_urr.resource_id in (:resourceIds)",
            nativeQuery = true)
    List<UserResourceRoleEntity> listByResourceTypeAndIdIn(
            @Param("resourceType") ResourceType resourceType, @Param("resourceIds") Collection<Long> resourceIds);

    @Query(value = "select i_urr.* from iam_user_resource_role i_urr inner join iam_resource_role i_rr on i_urr.resource_role_id = i_rr.id "
            + "where i_rr.resource_type = :#{#resourceType.name()} and i_rr.role_name = :roleName and i_urr.resource_id = :resourceId",
            nativeQuery = true)
    List<UserResourceRoleEntity> findByResourceIdAndTypeAndName(@Param("resourceId") Long resourceId,
            @Param("resourceType") ResourceType resourceType, @Param("roleName") String roleName);

    @Query(value = "select i_urr.* from iam_user_resource_role i_urr inner join iam_resource_role i_rr on i_urr.resource_role_id = i_rr.id "
            + "where i_rr.resource_type = :#{#resourceType.name()} and i_urr.user_id = :userId",
            nativeQuery = true)
    List<UserResourceRoleEntity> findByUserIdAndResourceType(@Param("userId") Long userId,
            @Param("resourceType") ResourceType resourceType);

    default List<UserResourceRoleEntity> batchCreate(List<UserResourceRoleEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("iam_user_resource_role")
                .field(UserResourceRoleEntity_.userId)
                .field(UserResourceRoleEntity_.resourceId)
                .field(UserResourceRoleEntity_.resourceRoleId)
                .field(UserResourceRoleEntity_.organizationId)
                .build();
        List<Function<UserResourceRoleEntity, Object>> getter = valueGetterBuilder()
                .add(UserResourceRoleEntity::getUserId)
                .add(UserResourceRoleEntity::getResourceId)
                .add(UserResourceRoleEntity::getResourceRoleId)
                .add(UserResourceRoleEntity::getOrganizationId)
                .build();
        return batchCreate(entities, sql, getter, UserResourceRoleEntity::setId);
    }
}
