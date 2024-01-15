CREATE TABLE IF NOT EXISTS `partitionplan` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for partition plan task',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  	`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `flow_instance_id` bigint(20) NOT NULL COMMENT 'Related flow instance id, reference flow_instance(id)',
    `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Flag bit, mark whether the partition plan task is enabled',
    `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references odc_user_info(id)',
    `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references odc_user_info(id)',
    `database_id` bigint(20) NOT NULL COMMENT 'Related database id, reference connect_database(id)',
    CONSTRAINT `pk_partitionplan_id` PRIMARY KEY (`id`),
    UNIQUE KEY `uk_partitionplan_flow_instance_id` (`flow_instance_id`)
);

CREATE TABLE IF NOT EXISTS `partitionplan_table` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for partition plan table',
    `table_name` varchar(512) NOT NULL COMMENT 'Target table name',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  	`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Flag bit, mark whether the partition plan task is enabled',
  	`partition_name_invoker` varchar(64) NOT NULL COMMENT 'The name of the specific executor of the partition strategy needs to match invoker_input',
    `partition_name_invoker_parameters` varchar(1024) NOT NULL COMMENT 'The input of the specific executor of the partition strategy invoker',
    `schedule_id` bigint(20) NOT NULL COMMENT 'Related schedule id, reference schedule_schedule(id)',
  	`partitionplan_id` bigint(20) NOT NULL COMMENT 'Related partition plan id, reference partitionplan(id)',
    CONSTRAINT `pk_partitionplan_table_id` PRIMARY KEY (`id`),
    UNIQUE KEY `uk_partitionplan_table_schedule_id_partitionplan_id_table_name` (`schedule_id`, `partitionplan_id`, `table_name`)
);

CREATE TABLE IF NOT EXISTS `partitionplan_table_partitionkey` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for partition plan table',
    `partition_key` varchar(2048) NOT NULL COMMENT 'Target partition key',
    `strategy` varchar(64) NOT NULL COMMENT 'Partition strategy type, enumeration values: CREATE, DROP',
    `partition_key_invoker` varchar(64) NOT NULL COMMENT 'The name of the specific executor of the partition strategy needs to match invoker_input',
    `partition_key_invoker_parameters` varchar(1024) NOT NULL COMMENT 'The input of the specific executor of the partition strategy invoker',
  	`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  	`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `partitionplan_table_id` bigint(20) NOT NULL COMMENT 'Related partition plan table id, reference partitionplan_table(id)',
    CONSTRAINT `pk_partitionplan_table_partitionkey` PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ptp_ptid_strategy_partition_key` (`partitionplan_table_id`, `strategy`, `partition_key`)
);
