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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author gaoda.xy
 * @date 2022/12/5 19:20
 */
public interface UserRoleRepository
        extends JpaRepository<UserRoleEntity, Long>, JpaSpecificationExecutor<UserRoleEntity> {

    List<UserRoleEntity> findByUserId(Long userId);

    List<UserRoleEntity> findByRoleId(Long roleId);

    List<UserRoleEntity> findByUserIdAndRoleIdAndOrganizationId(Long userId, Long roleId, Long organizationId);

    List<UserRoleEntity> findByRoleIdIn(Collection<Long> roleIds);

    List<UserRoleEntity> findByOrganizationIdAndUserIdIn(Long organizationId, Collection<Long> userIds);

    @Modifying
    @Transactional
    @Query(value = "update iam_user_role set role_id=:roleId where id=:id", nativeQuery = true)
    int updateRoleIdById(@Param("id") Long id, @Param("roleId") Long roleId);

    List<UserRoleEntity> findByUserIdIn(Collection<Long> userIds);

    @Transactional
    int deleteByUserId(Long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_user_role where organization_id=:organizationId and user_id=:userId",
            nativeQuery = true)
    int deleteByOrganizationIdAndUserId(@Param("organizationId") Long organizationId, @Param("userId") Long userId);

    @Transactional
    int deleteByRoleId(Long roleId);

    @Transactional
    int deleteByUserIdAndRoleId(Long userId, Long roleId);

}
