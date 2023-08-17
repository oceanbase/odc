--
--  v2.4.1
--
CREATE TABLE IF NOT EXISTS `odc_task_info`(
`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
`user_id` BIGINT NOT NULL,
`connection_name` VARCHAR(30) NOT NULL,
`task_name` VARCHAR(110) NOT NULL,
`task_type` VARCHAR(32) NOT NULL,
`create_time` TIMESTAMP NOT NULL,
`executor` VARCHAR(128) NOT NULL,
`update_time` TIMESTAMP,
`parameters` VARCHAR(1024),
`description` VARCHAR(2048),
`status` VARCHAR(16),
`progress_percentage` number(3,2),
`result` MEDIUMTEXT,
UNIQUE KEY(`user_id`, `task_type`,`task_name`)
);

ALTER TABLE `odc_user_info` ADD COLUMN `cipher` VARCHAR(16) NOT NULL DEFAULT 'RAW' COMMENT '用于用户密码字段加解密的算法，可选值 RAW/BCRYPT';
ALTER TABLE `odc_session_manager` ADD COLUMN `cipher` VARCHAR(16) NOT NULL DEFAULT 'RAW' COMMENT '用于连接密码字段加解密的算法，可选值 RAW/AES256SALT';
ALTER TABLE `odc_session_manager` ADD COLUMN `salt` VARCHAR(32)  COMMENT '用于连接密码字段加解密算法使用的随机值';

--
-- add lock table, here use UPPER CASE for follow spring-integration-jdbc's lock implementation
--
CREATE TABLE IF NOT EXISTS DISTRIBUTED_LOCK (
  LOCK_KEY CHAR(36) NOT NULL,
  REGION VARCHAR(100) NOT NULL,
  CLIENT_ID CHAR(36),
  CREATED_DATE DATETIME(6) NOT NULL,
  constraint DISTRIBUTED_LOCK_PK primary key (LOCK_KEY, REGION)
);

