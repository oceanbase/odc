---
--- v3.2.0
---
CREATE TABLE IF NOT EXISTS `connect_connection` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for database connection',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `visible_scope` varchar(64) NOT NULL DEFAULT 'PRIVATE' COMMENT 'Visible scope, enum: PRIVATE, ORGANIZATION',
  `owner_id` bigint(20) NOT NULL COMMENT 'Owner id',
  `name` varchar(64) NOT NULL COMMENT 'Name for database connection',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id',
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references odc_user_info(id)',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references odc_user_info(id)',
  `dialect_type` varchar(64) NOT NULL COMMENT 'Dialect type of ob, enumeration values:OB_MYSQL,OB_ORACLE,ORACLE,MYSQL,OB_SHARDING',
  `host` varchar(64) NOT NULL COMMENT 'Database connection address',
  `port` int(11) DEFAULT NULL COMMENT 'Database connection port, not required for public cloud connection',
  `cluster_name` varchar(256) DEFAULT NULL COMMENT 'Cluster name of OceanBase, not required for public cloud connection',
  `tenant_name` varchar(256) DEFAULT NULL COMMENT 'Tenant name of OceanBase, not required for public cloud connection',
  `username` varchar(128) NOT NULL COMMENT 'Database username',
  `password` varchar(256) DEFAULT NULL COMMENT 'Database password',
  `default_schema` varchar(1024) DEFAULT NULL COMMENT 'Schema name of the default connection',
  `sys_tenant_username` varchar(32) DEFAULT NULL COMMENT 'Username under the sys tenant',
  `sys_tenant_password` varchar(256) DEFAULT NULL COMMENT 'Password of the user under the sys tenant',
  `readonly_username` varchar(32) DEFAULT NULL COMMENT 'Username of read only account for readonly db session',
  `readonly_password` varchar(256) DEFAULT NULL COMMENT 'Password of read only account for readonly db session',
  `config_url` varchar(256) DEFAULT NULL COMMENT '[Deprecated], for OCJ connection type',
  `query_timeout_seconds` int(11) DEFAULT NULL COMMENT 'Query timeout',
  `properties_json` varchar(1024) DEFAULT NULL COMMENT 'Extension field, no specific purpose',
  `is_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Flag bit, mark whether the connection is enabled',
  `is_password_saved` tinyint(1) NOT NULL DEFAULT 1 COMMENT 'Save password or not, 1 save, 0 not save',
  `cipher` varchar(16) NOT NULL DEFAULT 'RAW' COMMENT 'The algorithm used for encryption and decryption of the connection password field, optional value RAW/AES256SALT',
  `salt` varchar(32) NOT NULL COMMENT 'Used to connect the random value used by the encryption and decryption algorithm of the password field',
  CONSTRAINT pk_connect_connection PRIMARY KEY (`id`),
  CONSTRAINT uk_connect_connection_visible_scope_owner_id_name UNIQUE KEY (visible_scope, owner_id, name)
);

CREATE TABLE IF NOT EXISTS `connect_session_history` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time for metadb audit',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time for metadb audit',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization ID, PK to iam_organization(id)',
  `connection_id` bigint(20) NOT NULL COMMENT 'Connection ID, FK to connect_connection(id)',
  `connect_time` datetime(6) NOT NULL COMMENT 'Session create time, generated by application',
  `user_id` bigint(20) NOT NULL COMMENT 'User ID, FK to odc_user_info(id)',
  `client_address` bigint(20) NOT NULL COMMENT 'Client address, e.g. IP address',
  `server` bigint(20) NOT NULL COMMENT 'ODC server node name, may hostname or IP address or docker name',
  CONSTRAINT pk_connect_session_history PRIMARY KEY (`id`),
  CONSTRAINT uk_connect_session_history_connection_id_time_user UNIQUE KEY (`organization_id`, `connection_id`, `connect_time`, `user_id`)
);

CREATE TABLE IF NOT EXISTS `iam_resource_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Resource group ID',
  `name` varchar(256) NOT NULL COMMENT 'Resource group name',
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references odc_user_info(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references odc_user_info(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `description` varchar(1024) DEFAULT NULL COMMENT 'Notice info',
  `is_enabled` tinyint(1) NOT NULL COMMENT 'Flag bit, mark whether the resource group is enabled',
  CONSTRAINT pk_iam_resource_group PRIMARY KEY (`id`),
  CONSTRAINT uk_iam_resource_group_name_organization_id UNIQUE KEY (`organization_id`, `name`)
);

CREATE TABLE IF NOT EXISTS `iam_resource_group_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `resource_id` bigint(20) NOT NULL,
  `resource_type` varchar(64) NOT NULL,
  `resource_group_id` bigint(20) NOT NULL,
  `creator_id` bigint(20) NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_iam_resource_group_resource_id PRIMARY KEY (`id`),
  CONSTRAINT uk_iam_resource_group_resource_id_res_id UNIQUE KEY (`resource_group_id`, `resource_type`,`resource_id`)
);

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

CREATE TABLE IF NOT EXISTS `iam_organization`(
 `id` BIGINT NOT NULL AUTO_INCREMENT,
 `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 `unique_identifier` VARCHAR(128) NOT NULL COMMENT 'Unique identifier, may from external system (e.g. a GUID of main account in public aliyun), or UUID generated by ODC',
 `secret` VARCHAR(256) NOT NULL COMMENT 'Secret for public connection encryption',
 `name` VARCHAR(256) NOT NULL COMMENT 'Name',
 `creator_id` BIGINT NULL DEFAULT NULL COMMENT 'UserID of creator, may NULL',
 `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
 `description` VARCHAR(512) NULL DEFAULT NULL COMMENT 'Description',
 CONSTRAINT pk_iam_organization_id PRIMARY KEY (`id`),
 CONSTRAINT uk_iam_organization_unique_identifier UNIQUE KEY (`unique_identifier`),
 CONSTRAINT uk_iam_organization_name UNIQUE KEY (`name`)
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

-- need periodically clean or rotate
CREATE TABLE IF NOT EXISTS `iam_login_history`(
 `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
 `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
 `organization_id` bigint NULL COMMENT 'FK to iam_organization.id',
 `user_id` bigint NULL COMMENT 'FK to iam_user.id, can be null for not exists user login attempt',
 `account_name` varchar(128) NOT NULL COMMENT 'Account name',
 `login_time` datetime NOT NULL COMMENT 'Login time',
 `is_success` tinyint(1) NOT NULL COMMENT 'Is login success, 1:success, 0:failed',
 `failed_reason` varchar(64) NULL  COMMENT 'Login failed reason',
 CONSTRAINT pk_iam_login_history_id PRIMARY KEY(`id`),
 KEY idx_iam_login_history_user_id_login_time (`user_id`,`login_time`)
);

-- need init enterprise_id in iam_user


-- for odc system configuration
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
  `creator_id` bigint(20) NOT NULL COMMENT 'user id of the creator',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  CONSTRAINT `pk_system_configuration_id` PRIMARY KEY  (`id`),
  UNIQUE KEY `uk_system_configuration_key_label` (`application`, `profile`, `label`, `key`)
) COMMENT = 'ODC system configuration, for odc administrator';


-- for organization configuration
CREATE TABLE IF NOT EXISTS `config_organization_configuration`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'auto-increment id',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id',
  `key` varchar(256) NOT NULL COMMENT 'config key',
  `value` varchar(1024) DEFAULT NULL COMMENT 'config value',
  `label` varchar(128) DEFAULT 'master' COMMENT 'label name',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `description` varchar(1024) DEFAULT NULL COMMENT 'description of the config',
  `creator_id` bigint(20) COMMENT 'user id of the creator',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  CONSTRAINT `pk_organization_configuration_id` PRIMARY KEY  (`id`),
  UNIQUE KEY `uk_organization_configuration_organization_id_key` (`organization_id`, `key`)
) COMMENT='Organization configuration, for organization administrator';
