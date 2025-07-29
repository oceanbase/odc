-- add column database_names, datasource_names, cluster_names, tenant_names to flow_instance
ALTER TABLE `flow_instance` ADD COLUMN `database_names` varchar(1000) COMMENT 'Database names, will be concatenated by , if there are multiple dbs.';
ALTER TABLE `flow_instance` ADD COLUMN `datasource_names` varchar(1000) COMMENT 'Datasource names, will be concatenated by , if there are multiple datasources.';
ALTER TABLE `flow_instance` ADD COLUMN `cluster_names` varchar(1000) COMMENT 'Cluster names, will be concatenated by , if there are multiple clusters.';
ALTER TABLE `flow_instance` ADD COLUMN `tenant_names` varchar(1000) COMMENT 'Tenant names, will be concatenated by , if there are multiple tenants.';

create or replace view list_flow_instance_view as select /*+use_merge(flow_instance flow_instance_node_task)*/
  `flow_instance`.`id` AS `id`,
  `flow_instance`.`create_time` AS `create_time`,
  `flow_instance`.`update_time` AS `update_time`,
  `flow_instance`.`name` AS `name`,
  `flow_instance`.`flow_config_id` AS `flow_config_id`,
  `flow_instance`.`creator_id` AS `creator_id`,
  `flow_instance`.`organization_id` AS `organization_id`,
  `flow_instance`.`process_definition_id` AS `process_definition_id`,
  `flow_instance`.`process_instance_id` AS `process_instance_id`,
  `flow_instance`.`status` AS `status`,
  `flow_instance`.`flow_config_snapshot_xml` AS `flow_config_snapshot_xml`,
  `flow_instance`.`description` AS `description`,
  `flow_instance`.`parent_instance_id` AS `parent_instance_id`,
  `flow_instance`.`project_id` AS `project_id`,
  `flow_instance`.`database_names` AS `database_names`,
  `flow_instance`.`datasource_names` AS `datasource_names`,
  `flow_instance`.`cluster_names` AS `cluster_names`,
  `flow_instance`.`tenant_names` AS `tenant_names`,
  `flow_instance_node_task`.`task_type` AS `task_type`
from
  (
      `flow_instance` join `flow_instance_node_task` on  ( `flow_instance`.`id` = `flow_instance_node_task`.`flow_instance_id`)
  );