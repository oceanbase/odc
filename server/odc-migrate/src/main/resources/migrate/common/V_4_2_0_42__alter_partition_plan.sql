ALTER TABLE `table_partition_plan` MODIFY COLUMN `flow_instance_id` bigint(20) COMMENT '任务 ID' NULL;
ALTER TABLE `table_partition_plan` ADD COLUMN `database_id` bigint NULL ;
ALTER TABLE `table_partition_plan` ADD COLUMN `database_partition_plan_id` bigint NULL ;
ALTER TABLE `connection_partition_plan` ADD COLUMN `database_id` bigint NULL ;
ALTER TABLE `connection_partition_plan` ADD COLUMN `schedule_id` bigint NULL ;

