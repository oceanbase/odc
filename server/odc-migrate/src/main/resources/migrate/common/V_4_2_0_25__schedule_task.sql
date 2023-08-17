create table if not exists `schedule_task` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'UUID',
`job_name` varchar(200) NOT NULL COMMENT 'References qrtz_job_details(job_name)',
`job_group` varchar(200) NOT NULL COMMENT 'References qrtz_job_details(job_group)',
`parameters_json` text NOT NULL COMMENT 'Task parameters json string.',
`status` varchar(120) NOT NULL COMMENT 'Task status.',
`executor` varchar(1024) DEFAULT  NULL COMMENT 'Task executor.',
`fire_time` datetime NOT NULL COMMENT 'The job fire time.',
`progress_percentage` decimal(6,3) NOT NULL DEFAULT 0.0 COMMENT 'Task progress.',
`result_json` text DEFAULT NULL COMMENT 'Task result.',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Date of task creation.',
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Date of task modification.',
PRIMARY KEY (`id`),
KEY `idx_schedule_task_job_name_job_group` (`job_name`, `job_group`)
);