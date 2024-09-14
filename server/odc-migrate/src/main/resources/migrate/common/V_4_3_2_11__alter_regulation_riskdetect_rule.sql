--
--  v4.3.2.11
--
-- alter column `value_json` size from varchar(2048) to MEDIUMTEXT
ALTER TABLE `regulation_riskdetect_rule` CHANGE COLUMN `value_json` `value_json_old` VARCHAR(2048);
ALTER TABLE `regulation_riskdetect_rule` ADD COLUMN `value_json` MEDIUMTEXT DEFAULT NULL;

-- update `value_json` column from `value_json_old`
UPDATE `regulation_riskdetect_rule` SET `value_json` = `value_json_old`;