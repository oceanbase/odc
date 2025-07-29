-- 1. delete old unique key
ALTER TABLE `partitionplan_table` DROP INDEX `uk_partitionplan_table_schedule_id_partitionplan_id_table_name`;
