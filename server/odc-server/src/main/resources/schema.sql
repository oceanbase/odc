-- for spring cloud config initialization
CREATE TABLE IF NOT EXISTS `config_system_configuration`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'auto-increment id',
  `key` varchar(256) NOT NULL COMMENT 'system config key',
  `value` varchar(1024) DEFAULT NULL COMMENT 'system config value',
  `application` varchar(256) NOT NULL DEFAULT 'odc' COMMENT 'application name',
  `profile` varchar(256) NOT NULL DEFAULT 'default' COMMENT 'profile name',
  `label` varchar(128) NOT NULL DEFAULT 'master' COMMENT 'label name',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `description` varchar(1024) DEFAULT NULL COMMENT 'description of the config',
  `creator_id` bigint(20) DEFAULT NULL COMMENT 'user id of the creator',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  CONSTRAINT `pk_system_configuration_id` PRIMARY KEY  (`id`),
  UNIQUE KEY `uk_system_configuration_key_label` (`application`, `profile`, `label`, `key`)
) ;
