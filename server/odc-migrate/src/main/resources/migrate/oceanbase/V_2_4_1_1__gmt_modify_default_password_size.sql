--
-- v2.4.1_1 mysql
--
ALTER TABLE `odc_user_info`	CHANGE COLUMN `gmt_modify` `gmt_modify` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE `odc_session_manager`	CHANGE COLUMN `gmt_modify` `gmt_modify` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `odc_user_info` CHANGE COLUMN `password` `password` VARCHAR(256) NULL DEFAULT NULL;
ALTER TABLE `odc_session_manager` CHANGE COLUMN `password` `password` VARCHAR(256) NULL DEFAULT NULL;
ALTER TABLE `odc_session_extended` CHANGE COLUMN `sys_user_password` `sys_user_password` VARCHAR(256) NULL DEFAULT NULL;
