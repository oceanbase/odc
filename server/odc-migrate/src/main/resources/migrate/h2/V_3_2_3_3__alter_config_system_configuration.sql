-- rename `value` to `value_deprecated` and add column `value` TEXT in config_system_configuration
ALTER TABLE `config_system_configuration` ALTER COLUMN `value` RENAME TO `value_deprecated`;
ALTER TABLE `config_system_configuration` ADD COLUMN `value` TEXT DEFAULT NULL COMMENT 'system config value';
