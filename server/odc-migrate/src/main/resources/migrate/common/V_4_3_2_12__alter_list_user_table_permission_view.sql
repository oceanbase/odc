/*
 * Copyright (c) 2024 OceanBase.
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

-- update: left join connect_connection because the logical database not belongs to any connection
create or replace view `list_user_database_permission_view` as
select
  u_p.`id` as `id`,
  u_p.`user_id` as `user_id`,
  u_p.`action` as `action`,
  u_p.`authorization_type` as `authorization_type`,
  u_p.`ticket_id` as `ticket_id`,
  u_p.`create_time` as `create_time`,
  u_p.`expire_time` as `expire_time`,
  u_p.`creator_id` as `creator_id`,
  u_p.`organization_id` as `organization_id`,
  c_d.`project_id` as `project_id`,
  c_d.`id` as `database_id`,
  c_d.`name` as `database_name`,
  c_c.`id` as `data_source_id`,
  c_c.`name` as `data_source_name`
from
  (
    select
      i_p.`id` as `id`,
      i_up.`user_id` as `user_id`,
      i_p.`action` as `action`,
      i_p.`authorization_type` as `authorization_type`,
      i_p.`ticket_id` as `ticket_id`,
      i_p.`create_time` as `create_time`,
      i_p.`expire_time` as `expire_time`,
      i_p.`creator_id` as `creator_id`,
      i_p.`organization_id` as `organization_id`,
      i_p.`resource_id` as `resource_id`
    from
      `iam_permission` as i_p
      inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`
    where
      i_p.`resource_type` = 'ODC_DATABASE'
  ) as u_p
  inner join `connect_database` as c_d on u_p.`resource_id` = c_d.`id`
  left join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`;


--
-- Add `list_user_table_permission_view` view for query user table permissions list
--
create or replace view `list_user_table_permission_view` as
select
  u_p.`id` as `id`,
  u_p.`user_id` as `user_id`,
  u_p.`action` as `action`,
  u_p.`authorization_type` as `authorization_type`,
  u_p.`ticket_id` as `ticket_id`,
  u_p.`create_time` as `create_time`,
  u_p.`expire_time` as `expire_time`,
  u_p.`creator_id` as `creator_id`,
  u_p.`organization_id` as `organization_id`,
  c_d.`project_id` as `project_id`,
  d_so.`id` as `table_id`,
  d_so.`name` as `table_name`,
  c_d.`id` as `database_id`,
  c_d.`name` as `database_name`,
  c_c.`id` as `data_source_id`,
  c_c.`name` as `data_source_name`
from
  (
    select
      i_p.`id` as `id`,
      i_up.`user_id` as `user_id`,
      i_p.`action` as `action`,
      i_p.`authorization_type` as `authorization_type`,
      i_p.`ticket_id` as `ticket_id`,
      i_p.`create_time` as `create_time`,
      i_p.`expire_time` as `expire_time`,
      i_p.`creator_id` as `creator_id`,
      i_p.`organization_id` as `organization_id`,
      i_p.`resource_id` as `resource_id`
    from
      `iam_permission` as i_p
      inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`
    where
      i_p.`resource_type` = 'ODC_TABLE'
  ) as u_p
  inner join `database_schema_object` as d_so on u_p.`resource_id` = d_so.`id`
  inner join `connect_database` as c_d on d_so.`database_id` = c_d.`id`
  left join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`;
