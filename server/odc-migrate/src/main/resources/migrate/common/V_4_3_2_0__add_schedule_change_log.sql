create table if not exists `schedule_change_log` (
   `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `schedule_id` bigint NOT NULL,
  `flow_instance_id` bigint NULL,
  `old_schedule_parameters` text NULL,
  `new_schedule_parameters` text NULL,
  `type` varchar(120) NOT NULL,
  `status` varchar(120) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time'
);


create table if not exists `schedule_latest_schedule_task_link` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`schedule_id` bigint NOT NULL,
`schedule_task_id` bigint NULL
);
