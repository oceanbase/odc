-- Record application integration info
CREATE TABLE `integration_integration` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `organization_id` bigint(20) NOT NULL,
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `encrypted` tinyint(1) NOT NULL DEFAULT '0',
  `algorithm` varchar(32) NOT NULL DEFAULT 'RAW',
  `secret` varchar(512) DEFAULT NULL,
  `salt` varchar(32) DEFAULT NULL COMMENT 'Used to connect the random value used by the encryption and decryption algorithm of the secret field',
  `configuration` mediumtext NOT NULL COMMENT 'Configuration parameters for integrating',
  `description` varchar(1024) DEFAULT NULL,
  CONSTRAINT `pk_odc_integration_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_odc_integration_organization_id_type_name` UNIQUE KEY (`organization_id`, `type`, `name`)
) COMMENT = 'Record application integration info';
