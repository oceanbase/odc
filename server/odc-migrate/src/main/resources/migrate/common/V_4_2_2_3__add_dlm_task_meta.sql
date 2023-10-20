CREATE TABLE IF NOT EXISTS `dlm_task_generator` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
`generator_id` varchar(120) NOT NULL,
`job_id` varchar(120) NOT NULL,
`processed_data_size` bigint(20) NOT NULL,
`processed_row_count` bigint(20) NOT NULL,
`status` varchar(64) NOT NULL,
`type` varchar(32) NOT NULL,
`primary_key_save_point` varchar(512) DEFAULT NULL,
`partition_save_point` varchar(512) DEFAULT NULL,
`task_count` bigint(20) NOT NULL DEFAULT '0',
PRIMARY KEY (`id`),
KEY `pk_generatora_id` (`generator_id`)
)
CREATE TABLE IF NOT EXISTS `dlm_task_unit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
`task_index` bigint(20) NOT NULL,
`job_id` varchar(120) NOT NULL,
`generator_id` varchar(120) NOT NULL,
`status` varchar (64) NOT NULL,
`lower_bound_primary_key` varchar (512),
`upper_bound_primary_key` varchar (512),
`primary_key_cursor` varchar (512),
`partition_name` varchar (512),
PRIMARY KEY (`id`)
);