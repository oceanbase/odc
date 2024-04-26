--
-- opitmize `list_user_database_permission_view` view for query user database permissions list
--
--
create
or replace view `list_user_database_permission_view` as
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
    c_c.`name` as `data_source_name`,
    u_p.resource_id as `resource_id`
from
    (
        select
            i_p.`id` as `id`,
            i_up.`user_id` as `user_id`,
            i_p.`resource_identifier` as `resource_identifier`,
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
    inner join `connect_database` as c_d on u_p.`resource_id` = c_d.`id` inner join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`;

--
-- Add `list_user_table_permission_view` view for query user database permissions list
--



create or replace view `list_user_table_permission_view` as
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
  c_d.`project_id` as `project_id`,
  c_d.`id` as `database_id`,
  c_d.`name` as `database_name`,
  c_c.`id` as `data_source_id`,
  c_c.`name` as `data_source_name`,
  c_t.`name` as `table_name`
from
  `iam_permission` as i_p
  inner join `iam_user_permission` as i_up on i_p.`id` = i_up.`permission_id`
  inner join `connect_table` as c_t on  c_t.`id` = i_p.`resource_id`
  inner join `connect_database` as c_d on c_t.database_id = c_d.`id`
  inner join `connect_connection` as c_c on c_d.`connection_id` = c_c.`id`
  where i_p.`resource_type` = 'ODC_TABLE';