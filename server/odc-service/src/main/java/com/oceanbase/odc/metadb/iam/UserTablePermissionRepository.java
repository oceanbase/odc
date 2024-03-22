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
 * ClassName: UserTablePermissionRepository.java Package: com.oceanbase.odc.metadb.iam Description:
 *
 * @Author: fenghao
 * @Create 2024/3/11 20:26
 * @Version 1.0
 */

public interface UserTablePermissionRepository extends ReadOnlyRepository<UserTablePermissionEntity, Long> {

    List<UserTablePermissionEntity> findByProjectIdAndIdIn(Long projectId, Collection<Long> ids);

    List<UserTablePermissionEntity> findByDatabaseIdIn(Collection<Long> databaseIds);

    @Query(value = "select"
            + "  i_p.`id` as `id`,"
            + "  i_up.`user_id` as `user_id`,"
            + "  i_p.`action` as `action`,"
            + "  i_p.`authorization_type` as `authorization_type`,"
            + "  i_p.`ticket_id` as `ticket_id`,"
            + "  i_p.`create_time` as `create_time`,"
            + "  i_p.`expire_time` as `expire_time`,"
            + "  i_p.`creator_id` as `creator_id`,"
            + "  i_p.`organization_id` as `organization_id`,"
            + "  c_d.`project_id` as `project_id`,"
            + "  c_d.`id` as `database_id`,"
            + "  c_d.`name` as `database_name`,"
            + "  c_c.`id` as `data_source_id`,"
            + "  c_c.`name` as `data_source_name`,"
            + "  c_t.`name` as `table_name`"
            + "from"
            + "  `iam_permission` as i_p"
            + "  inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`"
            + "  inner join `connect_table` as c_t on  c_t.`id` = i_p.`resource_id`"
            + "  inner join `connect_database` as c_d on c_t.database_id = c_d.`id`"
            + "  inner join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`"
            + "  where i_p.`resource_type` = 'ODC_TABLE'"
            + "  and i_up.user_id = :userId"
            + "  and c_d.`id` in(:databaseIds)", nativeQuery = true)
    List<UserTablePermissionEntity> findNotExpiredByUserIdAndDatabaseIdIn(@Param("userId") Long userId,
            @Param("databaseIds") Collection<Long> databaseIds);

    @Query(value = "select"
            + "  i_p.`id` as `id`,"
            + "  i_up.`user_id` as `user_id`,"
            + "  i_p.`action` as `action`,"
            + "  i_p.`authorization_type` as `authorization_type`,"
            + "  i_p.`ticket_id` as `ticket_id`,"
            + "  i_p.`create_time` as `create_time`,"
            + "  i_p.`expire_time` as `expire_time`,"
            + "  i_p.`creator_id` as `creator_id`,"
            + "  i_p.`organization_id` as `organization_id`,"
            + "  c_d.`project_id` as `project_id`,"
            + "  c_d.`id` as `database_id`,"
            + "  c_d.`name` as `database_name`,"
            + "  c_c.`id` as `data_source_id`,"
            + "  c_c.`name` as `data_source_name`,"
            + "  c_t.`name` as `table_name`"
            + "from"
            + "  `iam_permission` as i_p"
            + "  inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`"
            + "  inner join `connect_table` as c_t on  c_t.`id` = i_p.`resource_id`"
            + "  inner join `connect_database` as c_d on c_t.database_id = c_d.`id`"
            + "  inner join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`"
            + "  where i_p.`resource_type` = 'ODC_TABLE'"
            + "  and i_up.user_id = :userId"
            + "  and c_d.`project_id` = :projectId", nativeQuery = true)
    List<UserTablePermissionEntity> findByUserIdAndProjectId(@Param("userId") Long userId,
            @Param("projectId") Long projectId);

    @Query(value = "select"
            + "  i_p.`id` as `id`,"
            + "  i_up.`user_id` as `user_id`,"
            + "  i_p.`action` as `action`,"
            + "  i_p.`authorization_type` as `authorization_type`,"
            + "  i_p.`ticket_id` as `ticket_id`,"
            + "  i_p.`create_time` as `create_time`,"
            + "  i_p.`expire_time` as `expire_time`,"
            + "  i_p.`creator_id` as `creator_id`,"
            + "  i_p.`organization_id` as `organization_id`,"
            + "  c_d.`project_id` as `project_id`,"
            + "  c_d.`id` as `database_id`,"
            + "  c_d.`name` as `database_name`,"
            + "  c_c.`id` as `data_source_id`,"
            + "  c_c.`name` as `data_source_name`,"
            + "  c_t.`name` as `table_name`"
            + "from"
            + "  `iam_permission` as i_p"
            + "  inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`"
            + "  inner join `connect_table` as c_t on  c_t.`id` = i_p.`resource_id`"
            + "  inner join `connect_database` as c_d on c_t.database_id = c_d.`id`"
            + "  inner join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`"
            + "  where i_p.`resource_type` = 'ODC_TABLE'"
            + "  and i_up.user_id= :userId"
            + "  and c_d.`id` = :databaseId"
            + "  and c_t.`name` = :tableName", nativeQuery = true)
    List<UserTablePermissionEntity> findNotExpiredByUserIdAndDatabaseIdAndTableName(@Param("userId") Long userId,
            @Param("databaseId") Long databaseId, @Param("tableName") String tableName);

}
