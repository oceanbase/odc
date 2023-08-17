create table if not exists `regulation_approval_flow_config`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
  `approval_expire_interval_seconds` int NOT NULL,
  `wait_execution_expire_interval_seconds` int NOT NULL,
  `execution_expire_interval_seconds` int NOT NULL,
  `organization_id` bigint NOT NULL,
  `creator_id` bigint NOT NULL,
  constraint pk_regulation_approval_flow_config_id PRIMARY KEY (`id`),
  constraint uk_regulation_approval_flow_config_organization_id_name UNIQUE KEY (`organization_id`, `name`)
);

create table if not exists `regulation_approval_flow_node_config`(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Id for a usertask config',
    `approval_flow_config_id` bigint(20) NOT NULL COMMENT 'references to regulation_approval_flow_config.id',
    `resource_role_id` bigint(20) default NULL COMMENT 'references to iam_resource_role.id',
    `external_approval_id` bigint(20) DEFAULT NULL,
    `is_auto_approval` tinyint(1) NOT NULL DEFAULT '0',
    `sequence_number` int NOT NULL DEFAULT '0' COMMENT 'identify the order of this approval node in a approval flow config, 0 means the first node',
    constraint pk_regulation_approval_flow_node_config_id PRIMARY KEY (`id`),
    constraint uk_regulation_approval_flow_node_config_config_id_order UNIQUE KEY (`approval_flow_config_id`, `sequence_number`)
);

create table if not exists `regulation_risklevel`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `level` int NOT NULL,
  `style` varchar(64) NOT NULL DEFAULT 'RED' COMMENT 'environment style for front-end, enum value: GREEN, ORANGE, RED',
  `approval_flow_config_id` bigint(20) NOT NULL COMMENT 'reference to regulation_approval_flow_config.id',
  `organization_id` bigint(20) NOT NULL,
  constraint pk_regulation_risklevel_id PRIMARY KEY (`id`),
  constraint uk_regulation_risklevel_organization_id_level UNIQUE KEY (`organization_id`, `level`),
  constraint uk_regulation_risklevel_organization_id_name UNIQUE KEY (`organization_id`, `name`),
  constraint uk_regulation_risklevel_orgid_level_config_id UNIQUE KEY (`organization_id`, `level`, `approval_flow_config_id`)
);

create table if not exists `regulation_riskdetect_rule`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) default NULL,
  `risk_level_id` bigint(20) NOT NULL COMMENT 'reference to regulation_risklevel.id',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
  `creator_id` bigint(20) NOT NULL,
  `organization_id` bigint(20) NOT NULL,
  `value_json` varchar(2048) default NULL,
  constraint pk_regulation_riskdetect_rule_id PRIMARY KEY (`id`),
  constraint uk_regulation_riskdetect_rule_organization_id UNIQUE KEY (`organization_id`, `risk_level_id`)
);