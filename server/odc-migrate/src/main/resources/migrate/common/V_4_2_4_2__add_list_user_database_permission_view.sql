create or replace view `list_user_database_permission_view` as
select
  u_p.`id` as `id`,
  u_p.`user_id` as `user_id`,
  u_p.`action` as `action`,
  u_p.`source_type` as `source_type`,
  u_p.`ticket_id` as `ticket_id`,
  u_p.`create_time` as `create_time`,
  u_p.`expire_time` as `expire_time`,
  u_p.`creator_id` as `creator_id`,
  u_p.`organization_id` as `organization_id`,
  c_d.`project_id` as `project_id`,
  c_d.`id` as `database_id`,
  c_d.`name` as `database_name`,
  c_c.`id` as `datasource_id`,
  c_c.`name` as `datasource_name`,
  c_e.`id` as `environment_id`,
  c_e.`name` as `environment_name`
from
  (
    select
      i_p.`id` as `id`,
      i_up.`user_id` as `user_id`,
      i_p.`resource_id` as `resource_id`,
      i_p.`action` as `action`,
      i_p.`source_type` as `source_type`,
      i_p.`ticket_id` as `ticket_id`,
      i_p.`create_time` as `create_time`,
      i_p.`expire_time` as `expire_time`,
      i_p.`creator_id` as `creator_id`,
      i_p.`organization_id` as `organization_id`
    from
      `iam_permission` as i_p
      inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`
    where
      i_p.`resource_type` = 'ODC_DATABASE'
  ) as u_p
  inner join `connect_database` as c_d on u_p.`resource_id` = c_d.`id`
  inner join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`
  inner join `collaboration_environment` as c_e on c_c.`environment_id` = c_e.`id`;
