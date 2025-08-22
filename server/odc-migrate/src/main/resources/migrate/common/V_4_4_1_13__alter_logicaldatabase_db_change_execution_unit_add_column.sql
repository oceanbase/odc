-- add column schedule_changelog_id to relate to schedule_changelog.id
alter table `logicaldatabase_database_change_execution_unit` add column `flow_instance_id` bigint(20) NOT NULL COMMENT 'ID of the related flow instance, refer to flow_instance.id';
-- modify column schedule_task_id nullable
alter table `logicaldatabase_database_change_execution_unit` modify column `schedule_task_id` bigint(20) NULL COMMENT 'ID of the related schedule task, refer to schedule_task.id, discarded';

-- create new index
create index if not exists `uk_logical_db_change_flowid_pdi_order` on `logicaldatabase_database_change_execution_unit`(`flow_instance_id`, `physical_database_id`, `execution_order`);
-- drop index for schedule_task_id
alter table `logicaldatabase_database_change_execution_unit` drop index `uk_logical_db_change_sti_pdi_order`;
