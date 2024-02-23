-- v4.2.4: create table config_user_configuration
CREATE TABLE IF NOT EXISTS `config_user_configuration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'auto-increment id',
  `user_id` bigint(20) NOT NULL COMMENT 'User id, references iam_user(id)',
  `key` varchar(256) NOT NULL COMMENT 'config key',
  `value` varchar(1024) DEFAULT NULL COMMENT 'config value',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `description` varchar(1024) DEFAULT NULL COMMENT 'description of the config',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_configuration_user_id_key` (`user_id`, `key`)
);

-- migrate data from config_user to config_user_configuration
insert into config_user_configuration(user_id, `key`, `value`, create_time, update_time, description)
 select user_id, `key`,`value`, create_time, update_time, description from odc_user_configuration
 where `key` in ('sqlexecute.defaultDelimiter', 'sqlexecute.defaultObjectDraggingOption',
  'sqlexecute.defaultQueryLimit', 'sqlexecute.mysqlAutoCommitMode', 'sqlexecute.oracleAutoCommitMode');

update config_user_configuration set `key` = 'odc.sqlexecute.default.delimiter' where `key` = 'sqlexecute.defaultDelimiter';
update config_user_configuration set `key` = 'odc.sqlexecute.default.objectDraggingOption' where `key` = 'sqlexecute.defaultObjectDraggingOption';
update config_user_configuration set `key` = 'odc.sqlexecute.default.queryLimit' where `key` = 'sqlexecute.defaultQueryLimit';
update config_user_configuration set `key` = 'odc.sqlexecute.default.mysqlAutoCommitMode' where `key` = 'sqlexecute.mysqlAutoCommitMode';
update config_user_configuration set `key` = 'odc.sqlexecute.default.oracleAutoCommitMode' where `key` = 'sqlexecute.oracleAutoCommitMode';
