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
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.config.jpa.OdcJpaRepository;

public interface UserRepository extends OdcJpaRepository<UserEntity, Long> {

    List<UserEntity> findByIdIn(Collection<Long> ids);

    @Query(value = "select u.* from iam_user u where u.account_name in (:accountNames)", nativeQuery = true)
    List<UserEntity> findByAccountNameIn(@Param("accountNames") Collection<String> accountNames);

    boolean existsByIdAndOrganizationId(Long id, Long organizationId);

    default List<UserEntity> partitionFindById(Collection<Long> ids, int size) {
        return partitionFind(ids, size, this::findByIdIn);
    }

    @Query(value = "select u.* from (select * from iam_permission where id=:permissionId) as p inner join iam_role_permission "
            + "as rp on p.id=rp.permission_id inner join (select * from iam_role where is_enabled=:enabled) as r on "
            + "rp.role_id=r.id inner join iam_user_role as ur on r.id=ur.role_id inner join (select * from iam_user where "
            + "organization_id=:organizationId) as u on ur.user_id=u.id union all select u.* from "
            + "(select * from iam_permission WHERE id=:permissionId) as p inner join iam_user_permission up on "
            + "p.id=up.permission_id inner join (select * from iam_user where organization_id=:organizationId) as u "
            + "on up.user_id = u.id", nativeQuery = true)
    List<UserEntity> findByPermissionIdAndOrganizationId(@Param("permissionId") Long permissionId,
            @Param("organizationId") Long organizationId, @Param("enabled") Boolean enabled);

    Optional<UserEntity> findByAccountName(String accountName);

    List<UserEntity> findByOrganizationId(Long organizationId);

    Optional<UserEntity> findByOrganizationIdAndAccountName(Long organizationId, String accountName);

    @Query(value = "select u.* from iam_user u INNER JOIN iam_organization o "
            + "ON u.organization_id = o.id "
            + "WHERE u.account_name = :accountName AND o.unique_identifier = :uniqueIdentifier", nativeQuery = true)
    Optional<UserEntity> findByUniqueIdentifierAndAccountName(@Param("uniqueIdentifier") String uniqueIdentifier,
            @Param("accountName") String accountName);

    @Query(value = "select distinct u.* from iam_user u left join iam_user_role ur on u.id=ur.user_id left join iam_role r "
            + "on r.id=ur.role_id where u.is_enabled=:enabled and u.id in (:userIds)",
            nativeQuery = true)
    List<UserEntity> findByUserIdsAndEnabled(@Param("userIds") Collection<Long> userIds,
            @Param("enabled") Boolean enabled);

    @Query(value = "select distinct u.* from iam_user u left join iam_user_role ur on u.id=ur.user_id left join iam_role r "
            + "on r.id=ur.role_id where r.id in (:roleIds)",
            nativeQuery = true)
    List<UserEntity> findByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Query(value = "select distinct u.* from iam_user u left join iam_user_role ur on u.id=ur.user_id left join iam_role r "
            + "on r.id=ur.role_id where u.id in (:userIds)",
            nativeQuery = true)
    List<UserEntity> findByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query(value = "select u.* from iam_user u where u.id not in (select distinct user_id from iam_user_role)",
            nativeQuery = true)
    List<UserEntity> findByWithoutAnyRoles();

    @Query(value = "select distinct u.* from iam_user u left join iam_user_role ur on u.id=ur.user_id left join iam_role r "
            + "on r.id=ur.role_id where u.is_enabled=:enabled and (r.id in (:roleIds) and r.is_enabled=:enabled)",
            nativeQuery = true)
    List<UserEntity> findByRoleIdsAndEnabled(@Param("roleIds") Collection<Long> roleIds,
            @Param("enabled") Boolean enabled);

    @Query(value = "select distinct u.* from iam_user u left join iam_user_role ur on u.id=ur.user_id left join iam_role r "
            + "on r.id=ur.role_id where u.is_enabled=:enabled and (u.id in (:userIds) or (r.id in (:roleIds) "
            + "and r.is_enabled=:enabled))",
            nativeQuery = true)
    List<UserEntity> findByUserIdsOrRoleIds(@Param("userIds") Collection<Long> userIds,
            @Param("roleIds") Collection<Long> roleIds, @Param("enabled") Boolean enabled);

    @Modifying
    @Transactional
    @Query(value = "update iam_user set password=:#{#user.password}, is_active=:#{#user.active}, "
            + "user_update_time=now() where id=:#{#user.id}",
            nativeQuery = true)
    int updatePassword(@Param("user") UserEntity user);

    @Modifying
    @Transactional
    @Query(value = "update iam_user u set u.name=:name Where u.id=:id", nativeQuery = true)
    int updateUsernameById(@Param("id") Long id, @Param("name") String name);

    @Modifying
    @Transactional
    @Query(value = "update iam_user set name=:#{#user.name}, is_active=:#{#user.active}, is_enabled=:#{#user.enabled}, "
            + "description=:#{#user.description}, user_update_time=now() where id=:#{#user.id}",
            nativeQuery = true)
    int update(@Param("user") UserEntity user);

}
