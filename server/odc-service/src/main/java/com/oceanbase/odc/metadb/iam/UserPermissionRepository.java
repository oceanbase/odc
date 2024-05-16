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
import java.util.List;
import java.util.function.Function;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.jpa.InsertSqlTemplateBuilder;
import com.oceanbase.odc.config.jpa.OdcJpaRepository;

/**
 * @author gaoda.xy
 * @date 2022/12/15 14:59
 */
public interface UserPermissionRepository
        extends OdcJpaRepository<UserPermissionEntity, Long>, JpaSpecificationExecutor<UserPermissionEntity> {

    List<UserPermissionEntity> findByUserId(@Param("userId") Long userId);

    List<UserPermissionEntity> findByUserIdAndOrganizationId(@Param("userId") Long userId,
            @Param("organizationId") Long organizationId);

    List<UserPermissionEntity> findByPermissionId(@Param("permissionId") Long permissionId);

    List<UserPermissionEntity> findByUserIdIn(@Param("userIds") Collection<Long> userIds);

    List<UserPermissionEntity> findByPermissionIdIn(@Param("permissionIds") Collection<Long> permissionIds);

    @Query(value = "select up.* from iam_user_permission up inner join iam_permission p on up.permission_id=p.id "
            + "where up.user_id=:userId and p.resource_identifier in (:resourceIdentifiers)",
            nativeQuery = true)
    List<UserPermissionEntity> findByUserIdAndResourceIdentifiers(@Param("userId") Long userId,
            @Param("resourceIdentifiers") Collection<String> resourceIdentifiers);

    @Query(value = "select up.* from iam_user_permission up inner join iam_permission p on up.permission_id=p.id "
            + "where up.user_id=:userId and up.organization_id=:organizationId and p.resource_identifier=:resourceIdentifier "
            + "and p.action=:action",
            nativeQuery = true)
    List<UserPermissionEntity> findByUserIdAndOrganizationIdAndResourceIdentifierAndAction(@Param("userId") Long userId,
            @Param("organizationId") Long organizationId, @Param("resourceIdentifier") String resourceIdentifier,
            @Param("action") String action);

    @Query(value = "select up.* from iam_user_permission up inner join iam_permission p on up.permission_id=p.id "
            + "where up.organization_id=:organizationId and p.resource_identifier=:resourceIdentifier",
            nativeQuery = true)
    List<UserPermissionEntity> findByOrganizationIdAndResourceIdentifier(@Param("organizationId") Long organizationId,
            @Param("resourceIdentifier") String resourceIdentifier);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_permission up where up.id in (:ids)", nativeQuery = true)
    int deleteByIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_permission up where up.user_id in (:userIds)", nativeQuery = true)
    int deleteByUserIds(@Param("userIds") Collection<Long> userIds);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_permission up where up.permission_id in (:permissionIds)", nativeQuery = true)
    int deleteByPermissionIds(@Param("permissionIds") Collection<Long> permissionIds);

    default List<UserPermissionEntity> batchCreate(List<UserPermissionEntity> entities) {
        String sql = InsertSqlTemplateBuilder.from("iam_user_permission")
                .field(UserPermissionEntity_.userId)
                .field(UserPermissionEntity_.permissionId)
                .field(UserPermissionEntity_.creatorId)
                .field(UserPermissionEntity_.organizationId)
                .build();
        List<Function<UserPermissionEntity, Object>> getter = valueGetterBuilder()
                .add(UserPermissionEntity::getUserId)
                .add(UserPermissionEntity::getPermissionId)
                .add(UserPermissionEntity::getCreatorId)
                .add(UserPermissionEntity::getOrganizationId)
                .build();
        return batchCreate(entities, sql, getter, UserPermissionEntity::setId);
    }

}
