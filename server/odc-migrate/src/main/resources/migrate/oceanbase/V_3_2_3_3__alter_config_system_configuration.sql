-- rename `value` to `value_deprecated` and add column `value` since OceanBase 1.4.79 does not support syntax like this:
-- alter table `config_system_configuration` change column `value` `value` TEXT DEFAULT NULL COMMENT 'system config value';
ALTER TABLE `config_system_configuration` CHANGE COLUMN `value` `value_deprecated` varchar(1024);
ALTER TABLE `config_system_configuration` ADD COLUMN `value` TEXT DEFAULT NULL COMMENT 'system config value';