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
 * @date 2022/12/5 17:30
 */
public interface RolePermissionRepository
        extends JpaRepository<RolePermissionEntity, Long>, JpaSpecificationExecutor<RolePermissionEntity> {

    List<RolePermissionEntity> findByRoleId(Long roleId);

    RolePermissionEntity findByPermissionIdAndRoleId(Long permissionId, Long roleId);

    @Query(value = "select rp.* from iam_role_permission rp where role_id in (:roleIds)", nativeQuery = true)
    List<RolePermissionEntity> findByRoleIds(@Param("roleIds") Collection<Long> roleIds);

    @Query(value = "select rp.* from iam_role_permission rp inner join iam_role r on r.id=rp.role_id where rp.role_id "
            + "in (:roleIds) and r.is_enabled=:enabled",
            nativeQuery = true)
    List<RolePermissionEntity> findByRoleIdsAndEnabled(@Param("roleIds") Collection<Long> roleIds,
            @Param("enabled") Boolean enabled);

    @Query(value = "select rp.* from iam_role_permission rp join (select id as pid, type from iam_permission) p "
            + "on rp.permission_id=p.pid and type=:permissionType where role_id=:roleId",
            nativeQuery = true)
    List<RolePermissionEntity> findByRoleIdAndType(@Param("roleId") Long roleId,
            @Param("permissionType") String permissionType);

    @Transactional
    int deleteByPermissionId(Long permissionId);

    @Transactional
    int deleteByRoleId(Long roleId);

    @Modifying
    @Transactional
    @Query(value = "delete from iam_role_permission where permission_id in (:ids)", nativeQuery = true)
    int deleteByPermissionIds(@Param("ids") Collection<Long> ids);

}
