CREATE TABLE IF NOT EXISTS `dlm_job` (
  `id` bigint AUTO_INCREMENT NOT NULL COMMENT 'ID of the dlm job object',
  `schedule_task_id` bigint NOT NULL COMMENT 'ID of the related schedule task, refer to schedule_task.id',
  `dlm_job_id` varchar(120) NOT NULL COMMENT 'Unique identifier of the dlm job object',
  `table_name` varchar(120) NOT NULL COMMENT 'Name of the source table',
  `fire_time` varchar(120) NOT NULL COMMENT 'The actual time the trigger fired',
  `target_table_name` varchar(120) NULL COMMENT 'Name of the target table',
  `source_datasource_info` varchar(2048) NOT NULL COMMENT 'Info of the source datasource',
  `target_datasource_info` varchar(2048) NULL COMMENT 'Info of the target datasource',
  `execution_detail` text NULL COMMENT 'JSON formatted for task execution details',
  `status` varchar(120) NOT NULL COMMENT 'Current status of the dlm job',
  `type` varchar(120) NOT NULL COMMENT 'Type of the dlm job',
  `parameters` text NOT NULL COMMENT 'JSON format of various parameters/settings for the dlm job',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
INDEX `idx_dlm_job_schedule_task_id` (`schedule_task_id`),
CONSTRAINT `pk_dlm_job_id` PRIMARY KEY (`id`),
CONSTRAINT `uk_dlm_job_dlm_job_id` UNIQUE (`dlm_job_id`)
);