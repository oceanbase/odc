create table if not exists `structure_comparison`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `structure_comparison_task_id` bigint(20) NOT NULL COMMENT 'Related structure comparison task id, refer to structure_comparison_task(id)',
  `database_object_type` varchar(256) NOT NULL COMMENT 'The type of the database object to be compared',
  `database_object_name` varchar(256) NOT NULL COMMENT 'The source the database object name to be compared',
  `comparing_result` varchar(32) NOT NULL COMMENT 'Structural analysis results, optional values: ONLY_IN_SOURCE, ONLY_IN_TARGET, CONSISTENT, INCONSISTENT, MISSING_IN_SOURCE, UNSUPPORTED',
  `source_database_object_ddl` TEXT DEFAULT NULL COMMENT 'Source database object DDL',
  `target_database_object_ddl` TEXT DEFAULT NULL COMMENT 'Target database object DDL',
  `change_sql_script` LONGTEXT DEFAULT NULL COMMENT 'Change sql script to convert target database object to source database object',
  CONSTRAINT `pk_structure_comparison_id` PRIMARY KEY(`id`),
  KEY `idx_structure_comparison_id_structure_comparison_task_id`(`id`, `structure_comparison_task_id`),
  KEY `idx_structure_comparison_task_id`(`structure_comparison_task_id`),
  KEY `idx_structure_comparison_task_id_comparing_result`(`structure_comparison_task_id`, `comparing_result`),
  KEY `idx_structure_comparison_task_id_database_object_type`(`structure_comparison_task_id`, `database_object_type`)
);

create table if not exists `structure_comparison_task`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `creator_id` bigint(20) NOT NULL COMMENT 'Create user ID, references iam_user(id)',
  `flow_instance_id` bigint(20) DEFAULT NULL COMMENT 'Related flow instance id, references flow_instance(id)',
  `source_connect_database_id` bigint(20) NOT NULL COMMENT 'Source connect database id, references connect_database(id)',
  `target_connect_database_id` bigint(20) NOT NULL COMMENT 'Target connect database id, references connect_database(id)',
  `total_change_sql_script` LONGTEXT DEFAULT NULL COMMENT 'Total change sql script to convert target database to source database',
  `storage_object_id` varchar(64) default NULL COMMENT 'The storage object id of the total change script file, references objectstorage_object_metadata(object_id)',
  CONSTRAINT `pk_structure_comparison_task_id` PRIMARY KEY(`id`),
  CONSTRAINT `uk_id_flow_instance_id` UNIQUE(`id`, `flow_instance_id`)
);