--
--  v2.4.0
--
CREATE TABLE IF NOT EXISTS `odc_snippet` (
`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
`user_id` BIGINT,
`prefix` VARCHAR(64),
`body` TEXT,
`description` VARCHAR(256),
`type` VARCHAR(32),
`create_time` TIMESTAMP,
`modify_time` TIMESTAMP,
UNIQUE KEY(`user_id`, `prefix`)
);

CREATE TABLE IF NOT EXISTS `odc_session_label`(
`id` BIGINT NOT NULL AUTO_INCREMENT,
`user_id` BIGINT NOT NULL,
`label_name` VARCHAR(32) NOT NULL,
`label_color` VARCHAR(16) NOT NULL,
`gmt_create` TIMESTAMP,
`gmt_modify` TIMESTAMP,
PRIMARY KEY(`id`),
UNIQUE KEY(`user_id`, `label_name`)
);
