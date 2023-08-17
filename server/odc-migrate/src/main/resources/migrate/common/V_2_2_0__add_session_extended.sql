--
--  v2.2.0
--
ALTER TABLE odc_sql_script ADD COLUMN script_type VARCHAR(8) DEFAULT 'SQL' AFTER script_text;

CREATE TABLE IF NOT EXISTS `odc_session_extended` (
 `sid` INT(11) NOT NULL PRIMARY KEY,
 `session_timeout` INT(11),
 `sys_user` VARCHAR(32),
 `sys_user_password` VARCHAR(64),
 `gmt_create` TIMESTAMP NULL DEFAULT NULL,
 `gmt_modify` TIMESTAMP NULL DEFAULT NULL
);
