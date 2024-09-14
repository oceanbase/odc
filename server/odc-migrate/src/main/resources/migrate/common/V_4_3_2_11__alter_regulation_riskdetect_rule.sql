--
-- Alter column `value_json` size from varchar(2048) to MEDIUMTEXT in `regulation_riskdetect_rule` table
--
ALTER TABLE `regulation_riskdetect_rule` CHANGE COLUMN `value_json` `value_json_old` VARCHAR(2048);
ALTER TABLE `regulation_riskdetect_rule` ADD COLUMN `value_json` MEDIUMTEXT DEFAULT NULL;