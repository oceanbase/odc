--
-- v2.4.0_1 mysql
--
ALTER TABLE `odc_session_manager` MODIFY COLUMN `cluster` VARCHAR(64);
ALTER TABLE `odc_session_manager` MODIFY COLUMN `tenant` VARCHAR(64);
ALTER TABLE `odc_session_manager` MODIFY COLUMN `db_user` VARCHAR(128);
ALTER TABLE `odc_session_manager` MODIFY COLUMN `default_DBName` VARCHAR(128);
