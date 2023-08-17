--
-- v2.4.1_1 h2
--
ALTER TABLE `odc_user_info`	ALTER COLUMN `gmt_modify` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;
ALTER TABLE `odc_session_manager` ALTER COLUMN `gmt_modify` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `odc_user_info` ALTER COLUMN `password` VARCHAR(256) NULL DEFAULT NULL;
ALTER TABLE `odc_session_manager` ALTER COLUMN `password` VARCHAR(256) NULL DEFAULT NULL;
ALTER TABLE `odc_session_extended` ALTER COLUMN `sys_user_password` VARCHAR(256) NULL DEFAULT NULL;

-- for connect password encrypt in desktop version
INSERT INTO odc_user_info(`id`, `name`,`email`,`password`,`status`,`cipher`)
 VALUES(0, 'odc_desktop_user','odc_desktop_user@oceanbase.com','odc_desktop_user',1,'RAW')
 ON DUPLICATE KEY UPDATE `id`=`id`;