CREATE TABLE IF NOT EXISTS `schedule_changelog` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID of the change log',
`schedule_id` bigint NOT NULL COMMENT 'ID of the related schedule, refer to schedule_schedule.id',
`flow_instance_id` bigint NULL COMMENT 'ID of the related flow, refer to flow_instance.id',
`previous_parameters` text NULL COMMENT 'JSON of the schedule parameters before the change',
`new_parameters` text NULL COMMENT 'JSON of the schedule parameters after the change',
`type` varchar(120) NOT NULL COMMENT 'The type of change performed',
`status` varchar(120) NOT NULL COMMENT 'The current status of the change',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time'
);


CREATE TABLE IF NOT EXISTS `schedule_latest_task_mapping` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'The primary key identifier for this link record',
`schedule_id` bigint NOT NULL COMMENT 'Identifier of the schedule, refer to schedule_schedule.id',
`schedule_task_id` bigint NULL COMMENT 'Identifier of the latest task within this schedule, can be NULL if no task has been run yet'
) COMMENT='This table maintains the link to the latest task for each schedule for quick reference';

