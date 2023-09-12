---
--- v4.2.2
---
CREATE TABLE IF NOT EXISTS `connect_connection_attribute` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for connection attribute',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  	`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `key` varchar(1024) NOT NULL COMMENT 'Key for an attribute',
    `organization_id` bigint(20) NOT NULL COMMENT 'Organization id',
		`connection_id` bigint(20) NOT NULL COMMENT 'Related connection id, reference connect_connection(id)',
    `value` mediumtext DEFAULT NULL COMMENT 'Value for key',
    CONSTRAINT `pk_connect_connection_attributes` PRIMARY KEY (`id`),
    KEY `idx_connect_connection_attributes` (`connection_id`, `organization_id`),
    UNIQUE KEY `uk_connect_connection_attributes` (`key`, `connection_id`, `organization_id`)
);