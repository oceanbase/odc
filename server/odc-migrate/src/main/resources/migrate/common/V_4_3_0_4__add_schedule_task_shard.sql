CREATE TABLE IF NOT EXISTS `schedule_task_shard` (
  `id` bigint AUTO_INCREMENT  NOT NULL COMMENT 'ID of the schedule task unit',
  `schedule_task_id` bigint NOT NULL COMMENT 'ID of the related schedule task',
  `type` varchar(120) NOT NULL COMMENT 'Task type',
  `task_parameters` text NOT NULL COMMENT 'Task parameters',
  `execution_detail` text NOT NULL COMMENT 'Task execution detail',
  `start_time` date NULL COMMENT 'Task start time',
  `end_time` date NULL COMMENT 'Task end time',
  `status` varchar(120) NOT NULL COMMENT 'Task status',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  INDEX `idx_schedule_task_shard_schedule_task_id`  (`schedule_task_id`)
);