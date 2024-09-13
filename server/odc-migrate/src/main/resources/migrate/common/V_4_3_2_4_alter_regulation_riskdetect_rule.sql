--
-- Alter column `value_json` size from varchar(2048) to varchar(10240) in `regulation_riskdetect_rule` table
--
alter table `regulation_riskdetect_rule` modify column `value_json` varchar(10240) default null comment 'json of rule value';
