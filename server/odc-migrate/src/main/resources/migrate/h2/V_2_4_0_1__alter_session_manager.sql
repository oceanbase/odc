--
-- v2.4.0_1 h2
--
ALTER TABLE `odc_session_manager` ALTER COLUMN `cluster` VARCHAR(64);
ALTER TABLE `odc_session_manager` ALTER COLUMN `tenant` VARCHAR(64);
ALTER TABLE `odc_session_manager` ALTER COLUMN `db_user` VARCHAR(128);
ALTER TABLE `odc_session_manager` ALTER COLUMN `default_DBName` VARCHAR(128);
