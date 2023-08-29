ALTER TABLE `table_partition_plan` ADD COLUMN `database_id` bigint NULL ;
ALTER TABLE `table_partition_plan` ADD COLUMN `database_partition_plan_id` bigint NULL ;
ALTER TABLE `connection_partition_plan` ADD COLUMN `database_id` bigint NULL ;
ALTER TABLE `connection_partition_plan` ADD COLUMN `schedule_id` bigint NULL ;

