--
--  v3.2.2.1
--
-- rename old column to another name but not drop in case migration failed
ALTER TABLE `odc_task_info` CHANGE COLUMN `parameters` `parameters_old` VARCHAR(1024);
ALTER TABLE `odc_task_info` ADD COLUMN `parameters` MEDIUMTEXT;

ALTER TABLE `odc_sql_script` CHANGE COLUMN `script_text` `script_text_old` TEXT;
ALTER TABLE `odc_sql_script` ADD COLUMN `script_text` MEDIUMTEXT;


-- remove desktop_user for web version ODC
DELETE from iam_user where `id` = 0;
