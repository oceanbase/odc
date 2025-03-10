-- drop table config_organization_configuration
DROP TABLE IF EXISTS `config_organization_configuration`;

-- v4.3.4: create table config_organization_configuration
CREATE TABLE IF NOT EXISTS `config_organization_configuration`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'auto-increment id',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, reference config_organization.id',
  `key` varchar(256) NOT NULL COMMENT 'config key',
  `value` varchar(1024) DEFAULT NULL COMMENT 'config value',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `description` varchar(1024) DEFAULT NULL COMMENT 'description of the config',
  `creator_id` bigint(20) COMMENT 'user id of the creator',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  CONSTRAINT `pk_organization_configuration_id` PRIMARY KEY  (`id`),
  UNIQUE KEY `uk_organization_configuration_organization_id_key` (`organization_id`, `key`)
) COMMENT='Organization configuration, for organization administrator';
