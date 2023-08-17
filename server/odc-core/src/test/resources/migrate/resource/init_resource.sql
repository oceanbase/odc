CREATE TABLE IF NOT EXISTS `iam_user`(
 `id` bigint NOT NULL AUTO_INCREMENT,
 `name` varchar(128) NOT NULL,
 `account_name` varchar(128) NOT NULL,
 `organization_id` bigint NOT NULL,
 `email_address` varchar(320) DEFAULT NULL,
 `password` varchar(256) NOT NULL,
 `cipher` varchar(16) NOT NULL DEFAULT 'RAW',
 `is_active` tinyint(1) NOT NULL DEFAULT '0',
 `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
 `creator_id` bigint NOT NULL,
 `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
 `user_create_time` datetime DEFAULT NULL,
 `user_update_time` datetime DEFAULT NULL,
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 `description` varchar(512) DEFAULT NULL,
 CONSTRAINT pk_iam_user_id PRIMARY KEY(`id`),
 CONSTRAINT uk_iam_user_organization_id_account_name UNIQUE KEY(`organization_id`, `account_name`)
 ) AUTO_INCREMENT = 10000;

CREATE TABLE IF NOT EXISTS `iam_user_role`(
 `id` bigint NOT NULL AUTO_INCREMENT,
 `user_id` bigint NOT NULL,
 `role_id` bigint NOT NULL,
 `creator_id` bigint NOT NULL,
 `organization_id` bigint NOT NULL,
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 CONSTRAINT pk_iam_user_role_id PRIMARY KEY (`id`),
 CONSTRAINT uk_iam_user_role_user_id_role_id UNIQUE KEY(`user_id`, `role_id`)
);

CREATE TABLE IF NOT EXISTS `iam_role`(
 `id` bigint NOT NULL AUTO_INCREMENT,
 `name` varchar(128) NOT NULL,
 `organization_id` bigint NOT NULL,
 `type` varchar(64) NOT NULL COMMENT 'admin / internal / custom',
 `is_enabled` tinyint(1) DEFAULT '1',
 `creator_id` bigint NOT NULL,
 `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
 `user_create_time` datetime DEFAULT NULL,
 `user_update_time` datetime DEFAULT NULL,
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 `description` varchar(512) DEFAULT NULL,
 CONSTRAINT pk_iam_role_id PRIMARY KEY (`id`),
 CONSTRAINT uk_iam_role_organization_id_name UNIQUE KEY(`organization_id`, `name`)
) AUTO_INCREMENT = 10000;

CREATE TABLE IF NOT EXISTS `iam_role_permission`(
 `id` bigint NOT NULL AUTO_INCREMENT,
 `role_id` bigint NOT NULL,
 `permission_id` bigint NOT NULL,
 `creator_id` bigint NOT NULL,
 `organization_id` bigint NOT NULL,
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 CONSTRAINT pk_iam_role_permission_id PRIMARY KEY (`id`),
 CONSTRAINT uk_iam_role_permission_role_id_permission_id UNIQUE KEY(`role_id`,`permission_id`)
);

CREATE TABLE IF NOT EXISTS `iam_permission`(
 `id` bigint NOT NULL AUTO_INCREMENT,
 `organization_id` bigint NOT NULL,
 `action` varchar(64) NOT NULL COMMENT 'query / create / update / delete / read / readandwrite',
 `resource_identifier` varchar(128) NOT NULL COMMENT 'key value expression like resource_group:10',
 `type`  varchar(64) NOT NULL COMMENT 'system / public_resource',
 `creator_id` bigint NOT NULL,
 `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 `description` varchar(512) DEFAULT NULL,
 CONSTRAINT pk_iam_permission_id PRIMARY KEY (`id`)
) AUTO_INCREMENT = 10000;