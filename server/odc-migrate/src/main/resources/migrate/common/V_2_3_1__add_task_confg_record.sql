--
--  v2.3.1
--
CREATE TABLE IF NOT EXISTS `odc_task_config`(
`id` INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
`task_type` VARCHAR(32),
`task_action` VARCHAR(2048) NOT NULL,
`before_action` VARCHAR(2048),
`after_action` VARCHAR(2048),
`task_desc` VARCHAR(128),
`gmt_create` TIMESTAMP,
`gmt_modify` TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `odc_task_record`(
`id` INT(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
`user_id` INT(11) NOT NULL,
`task_name` VARCHAR(128) NOT NULL,
`task_type` VARCHAR(32) NOT NULL,
`task_status` VARCHAR(16) NOT NULL,
`task_detail` TEXT,
`extend_info` VARCHAR(2048),
`gmt_create` TIMESTAMP,
`gmt_modify` TIMESTAMP,
KEY(`user_id`, `task_type`),
KEY(`task_status`)
);

ALTER TABLE odc_session_manager ADD COLUMN extend_info VARCHAR(1024);
