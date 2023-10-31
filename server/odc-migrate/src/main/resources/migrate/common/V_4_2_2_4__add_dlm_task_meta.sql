CREATE TABLE IF NOT EXISTS `dlm_task_generator` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id for task generator',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'record insertion time',
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'record modification time',
`generator_id` varchar(120) NOT NULL COMMENT 'id for task generator',
`job_id` varchar(120) NOT NULL COMMENT 'job id for dlm task',
`processed_data_size` bigint(20) NOT NULL COMMENT 'the data size of task generator processed',
`processed_row_count` bigint(20) NOT NULL COMMENT 'number of rows processed by task generator',
`status` varchar(64) NOT NULL COMMENT 'status of the task generator',
`type` varchar(32) NOT NULL COMMENT 'type of the task generator',
`primary_key_save_point` varchar(512) DEFAULT NULL COMMENT 'save point for primary key',
`partition_save_point` varchar(512) DEFAULT NULL COMMENT 'save point for partition',
`task_count` bigint(20) NOT NULL DEFAULT '0' COMMENT 'number of tasks',
CONSTRAINT pk_dlm_task_generator_id PRIMARY KEY (id),
UNIQUE KEY `pk_dlm_task_generator_generatora_id` (`generator_id`),
UNIQUE KEY `pk_dlm_task_generator_job_id` (`job_id`),
);

CREATE TABLE IF NOT EXISTS `dlm_task_unit` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id for task unit',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'record insertion time',
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'record modification time',
`task_index` bigint(20) NOT NULL COMMENT 'index of the task',
`job_id` varchar(120) NOT NULL COMMENT 'job id for dlm task',
`generator_id` varchar(120) NOT NULL COMMENT 'id for task generator',
`status` varchar(64) NOT NULL COMMENT 'status of the task unit',
`lower_bound_primary_key` varchar(512) DEFAULT NULL COMMENT 'lower bound for primary key',
`upper_bound_primary_key` varchar(512) DEFAULT NULL COMMENT 'upper bound for primary key',
`primary_key_cursor` varchar(512) DEFAULT NULL COMMENT 'cursor for primary key',
`partition_name` varchar(512) DEFAULT NULL COMMENT 'name of the partition',
CONSTRAINT pk_dlm_task_unit_id PRIMARY KEY (id),
UNIQUE KEY `idx_dlm_task_unit_job_id_generator_id_task_index` (`job_id`, `generator_id`, `task_index`)
);