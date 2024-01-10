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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gaoda.xy
 * @date 2022/12/2 14:08
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long>, JpaSpecificationExecutor<RoleEntity> {

    List<RoleEntity> findByIdIn(Collection<Long> ids);

    List<RoleEntity> findByName(String name);

    Page<RoleEntity> findByOrganizationId(Long organizationId, Pageable pageable);

    Optional<RoleEntity> findByNameAndOrganizationId(String name, Long organizationId);

    List<RoleEntity> findByOrganizationIdAndNameIn(Long organizationId, List<String> name);

    @Query(value = "select r.* from iam_role r where r.organization_id=:organizationId and r.id in (:roleIds)",
            countQuery = "select count(*) from iam_role r where r.organization_id=:organizationId and r.id in (:roleIds)",
            nativeQuery = true)
    Page<RoleEntity> findByOrganizationIdAndRoleIds(@Param("organizationId") Long organizationId,
            @Param("roleIds") Collection<Long> roleIds, Pageable pageable);

    @Query(value = "select r.* from iam_user u inner join iam_user_role ur on u.id=ur.user_id " +
            "inner join iam_role r on r.id=ur.role_id where u.id=:userId",
            nativeQuery = true)
    List<RoleEntity> findByUserId(@Param("userId") Long userId);

    @Query(value = "select r.* from iam_user u inner join iam_user_role ur on u.id=ur.user_id " +
            "inner join iam_role r on r.id=ur.role_id where u.id=:userId and r.is_enabled=:enabled",
            nativeQuery = true)
    List<RoleEntity> findByUserIdAndEnabled(@Param("userId") Long userId, @Param("enabled") Boolean enabled);

    @Query(value = "select r.* from iam_user u inner join iam_user_role ur on u.id=ur.user_id "
            + "inner join iam_role r on r.id=ur.role_id where u.id=:userId and r.organization_id=:organizationId "
            + "and r.is_enabled=:enabled",
            nativeQuery = true)
    List<RoleEntity> findByUserIdAndOrganizationIdAndEnabled(@Param("userId") Long userId,
            @Param("organizationId") Long organizationId, @Param("enabled") Boolean enabled);

    @Query(value = "select r.* from iam_role r where id in (:roleIds) and r.is_enabled=:enabled",
            nativeQuery = true)
    List<RoleEntity> findByRoleIdsAndEnabled(@Param("roleIds") Collection<Long> roleIds,
            @Param("enabled") Boolean enabled);

    @Modifying
    @Transactional
    @Query(value = "update iam_role set name=:#{#roleEntity.name}, is_enabled=:#{#roleEntity.enabled}, "
            + "description=:#{#roleEntity.description}, user_update_time=now() where id=:#{#roleEntity.id}",
            nativeQuery = true)
    int update(@Param("roleEntity") RoleEntity roleEntity);
}
