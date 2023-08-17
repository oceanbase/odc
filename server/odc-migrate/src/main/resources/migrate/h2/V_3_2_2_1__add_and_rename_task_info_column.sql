--
--  v3.2.2.1
--
-- rename old column to another name but not drop in case migration failed
ALTER TABLE `odc_task_info` ALTER COLUMN `parameters` RENAME TO `parameters_old`;
ALTER TABLE `odc_task_info` ADD COLUMN `parameters` MEDIUMTEXT;

ALTER TABLE `odc_sql_script` ALTER COLUMN `script_text` RENAME TO `script_text_old`;
ALTER TABLE `odc_sql_script` ADD COLUMN `script_text` MEDIUMTEXT;