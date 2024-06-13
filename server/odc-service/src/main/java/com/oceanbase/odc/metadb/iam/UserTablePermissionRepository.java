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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.oceanbase.odc.metadb.flow.ReadOnlyRepository;

/**
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:26
 * @Version 1.0
 */

public interface UserTablePermissionRepository extends ReadOnlyRepository<UserTablePermissionEntity, Long> {

    List<UserTablePermissionEntity> findByProjectIdAndIdIn(Long projectId, Collection<Long> ids);

    List<UserTablePermissionEntity> findByDatabaseIdIn(Collection<Long> databaseIds);

    @Query(value = "select v.* from list_user_table_permission_view v where v.expire_time > now() "
            + "and v.user_id = :userId and v.database_id in (:databaseIds)", nativeQuery = true)
    List<UserTablePermissionEntity> findNotExpiredByUserIdAndDatabaseIdIn(@Param("userId") Long userId,
            @Param("databaseIds") Collection<Long> databaseIds);

    @Query(value = "select v.* from list_user_table_permission_view v where v.expire_time > now() "
            + "and v.user_id = :userId and v.table_id in (:tableIds)", nativeQuery = true)
    List<UserTablePermissionEntity> findNotExpiredByUserIdAndTableIdIn(@Param("userId") Long userId,
            @Param("tableIds") Collection<Long> tableIds);

    @Query(value = "select v.* from list_user_table_permission_view v where v.user_id = :userId "
            + "and v.project_id = :projectId", nativeQuery = true)
    List<UserTablePermissionEntity> findByUserIdAndProjectId(@Param("userId") Long userId,
            @Param("projectId") Long projectId);

}
