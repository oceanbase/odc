CREATE TABLE IF NOT EXISTS `schedule_task_shard` (
  `id` bigint AUTO_INCREMENT  NOT NULL COMMENT 'ID of the schedule task shard',
  `schedule_task_id` bigint NOT NULL COMMENT 'ID of the related schedule task,refer to schedule_task.id',
  `schedule_task_type` varchar(120) NOT NULL COMMENT 'Type of the related schedule task',
  `parameters` text NOT NULL COMMENT 'Task shard parameters',
  `execution_detail` text NOT NULL COMMENT 'Task shard execution detail',
  `start_time` datetime NULL COMMENT 'Task shard start time',
  `end_time` datetime NULL COMMENT 'Task shard end time',
  `status` varchar(120) NOT NULL COMMENT 'Task status',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
CONSTRAINT `pk_schedule_task_shard_id` PRIMARY KEY (`id`),
INDEX `idx_schedule_task_shard_schedule_task_id`  (`schedule_task_id`)
);