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


alter table `flow_instance` add index `flow_instance_parent_ins_id`(`parent_instance_id`);

alter table `flow_instance_node_task` add index `flow_instance_node_task_flow_instance_id`(`flow_instance_id`);

alter table `flow_instance` add index `flow_instance_parent_oid_ct_id`(`organization_id`,`create_time`,`id`);

alter table `flow_instance_node` add index `flow_instance_node_intsance_id`(`instance_id`);

alter table `flow_instance` add index `flow_instance_process_definition_id`(`process_definition_id`);

CREATE or replace  VIEW `list_flow_instance_view` AS
select
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
    `flow_instance_node_task`.`task_type` AS `task_type`
from
    (
        `flow_instance` join `flow_instance_node_task` on  ( `flow_instance`.`id` = `flow_instance_node_task`.`flow_instance_id`)
    )
group by `flow_instance`.`id`,`flow_instance_node_task`.`task_type`