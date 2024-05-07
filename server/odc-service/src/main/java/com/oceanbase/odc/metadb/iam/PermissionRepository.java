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
package com.oceanbase.odc.metadb.iam;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;
import com.oceanbase.odc.core.shared.constant.PermissionType;
import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Find all <code>PermissionEntity</code> for a specific <code>User</code>
 *
 * @author yh263208
 * @date 2021-08-02 19:10
 * @since ODC-release_3.2.0
 */
public interface PermissionRepository
        extends OdcJpaRepository<PermissionEntity, Long>, JpaSpecificationExecutor<PermissionEntity> {

    @Query(value = "select p.* from (select id from iam_user where id=:userId) u inner join iam_user_role u_r "
            + "on u.id=u_r.user_id inner join (select id from iam_role where is_enabled=:roleStatus) r "
            + "on u_r.role_id=r.id inner join iam_role_permission r_p on r.id=r_p.role_id inner join "
            + "(select * from iam_permission where organization_id=:organizationId) p on r_p.permission_id=p.id "
            + "where p.expire_time > now() union all select p.* from (select id from iam_user where id=:userId) u "
            + "inner join iam_user_permission u_p on u.id=u_p.user_id inner join (select * from iam_permission where "
            + "organization_id=:organizationId) p on u_p.permission_id=p.id where p.expire_time > now()",
            nativeQuery = true)
    List<PermissionEntity> findByUserIdAndRoleStatusAndOrganizationId(@Param("userId") Long userId,
            @Param("roleStatus") Boolean roleStatus, @Param("organizationId") Long organizationId);

    @Query(value = "select p.* from (select id from iam_user where id=:userId and is_enabled=:userStatus) u " +
            "inner join iam_user_role u_r on u.id=u_r.user_id inner join (select id from iam_role where "
            + "is_enabled=:roleStatus) r on u_r.role_id=r.id inner join iam_role_permission r_p on r.id=r_p.role_id "
            + "inner join (select * from iam_permission where organization_id=:organizationId) p on "
            + "r_p.permission_id=p.id where p.expire_time > now() union all select p.* from (select id from iam_user "
            + "where id=:userId and is_enabled=:userStatus) u inner join iam_user_permission u_p on u.id=u_p.user_id "
            + "inner join (select * from iam_permission where organization_id=:organizationId) p on u_p.permission_id=p.id "
            + "where p.expire_time > now()",
            nativeQuery = true)
    List<PermissionEntity> findByUserIdAndUserStatusAndRoleStatusAndOrganizationId(@Param("userId") Long userId,
            @Param("userStatus") Boolean userStatus, @Param("roleStatus") Boolean roleStatus,
            @Param("organizationId") Long organizationId);

    @Query(value = "select p.* from iam_role r inner join iam_role_permission r_p on r.id=r_p.role_id inner " +
            "join iam_permission p on r_p.permission_id=p.id where r.id in (:roleIds) and p.expire_time > now()",
            nativeQuery = true)
    List<PermissionEntity> findByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Query(value = "select p.* from iam_permission p inner join iam_user_permission up on p.id = up.permission_id "
            + "where up.user_id in (:userIds) and p.organization_id = :organizationId and p.expire_time > now()",
            nativeQuery = true)
    List<PermissionEntity> findByUserIdsAndOrganizationId(@Param("userIds") Collection<Long> userIds,
            @Param("organizationId") Long organizationId);

    List<PermissionEntity> findByIdIn(@Param("ids") Collection<Long> ids);

    List<PermissionEntity> findByTypeAndOrganizationId(PermissionType type, Long organizationId);

    List<PermissionEntity> findByOrganizationId(Long organizationId);

    List<PermissionEntity> findByResourceTypeAndResourceIdIn(ResourceType resourceType, Collection<Long> resourceIds);

    List<PermissionEntity> findByOrganizationIdAndResourceIdentifier(Long organizationId, String resourceIdentifier);

    Optional<PermissionEntity> findByOrganizationIdAndActionAndResourceIdentifier(Long organizationId, String action,
            String resourceIdentifier);

    @Query(value = "select p.* from iam_permission p", nativeQuery = true)
    List<PermissionEntity> findAllNoCareExpireTime();

    @Query(value = "select p.* from iam_permission p where p.expire_time <:expireTime",
            nativeQuery = true)
    List<PermissionEntity> findByExpireTimeBefore(@Param("expireTime") Date expireTime);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_permission p where p.id in (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_permission p where 1=1", nativeQuery = true)
    void deleteAll();

    default List<PermissionEntity> batchCreate(List<PermissionEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("iam_permission")
                .field(PermissionEntity_.action)
                .field(PermissionEntity_.resourceIdentifier)
                .field(PermissionEntity_.type)
                .field(PermissionEntity_.creatorId)
                .field(PermissionEntity_.organizationId)
                .field("is_builtin")
                .field(PermissionEntity_.expireTime)
                .field(PermissionEntity_.authorizationType)
                .field(PermissionEntity_.ticketId)
                .field(PermissionEntity_.resourceType)
                .field(PermissionEntity_.resourceId)
                .build();
        List<Function<PermissionEntity, Object>> getter = valueGetterBuilder()
                .add(PermissionEntity::getAction)
                .add(PermissionEntity::getResourceIdentifier)
                .add((PermissionEntity e) -> e.getType().name())
                .add(PermissionEntity::getCreatorId)
                .add(PermissionEntity::getOrganizationId)
                .add(PermissionEntity::getBuiltIn)
                .add(PermissionEntity::getExpireTime)
                .add((PermissionEntity e) -> e.getAuthorizationType().name())
                .add(PermissionEntity::getTicketId)
                .add((PermissionEntity e) -> e.getResourceType().name())
                .add(PermissionEntity::getResourceId)
                .build();

        return batchCreate(entities, sql, getter, PermissionEntity::setId);
    }

}
