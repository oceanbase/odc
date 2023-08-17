create table if not exists `regulation_rule_metadata`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL COMMENT 'rule type, enum values: SQL_CHECK, SQL_CONSOLE',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '1',
  CONSTRAINT `pk_regulation_rule_metadata_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_regulation_rule_metadata_type_name` UNIQUE KEY (`type`, `name`)
) COMMENT = 'rule metadata';

create table if not exists `regulation_rule_metadata_label`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(64) NOT NULL COMMENT 'rule label type, enum values: SUB_TYPE, SUPPORTED_DIALECT_TYPE',
  `value` varchar(256) NOT NULL COMMENT 'label value, may be DDL, INDEX, OB_MYSQL, OB_ORACLE, etc.',
  `rule_metadata_id` bigint(20) COMMENT 'reference to regulation_rule_metadata.id',
  CONSTRAINT `pk_regulation_rule_metadata_label_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_regulation_rule_metadata_label_type_value` UNIQUE KEY (`rule_metadata_id`, `type`, `value`)
) COMMENT = 'describe a rule label, may be rule subtype, rule supported dialect types, etc.';

create table if not exists `regulation_rule_metadata_property_metadata`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL COMMENT 'property type, enum values: BOOLEAN, INTEGER, STRING, STRING_LIST, INTEGER_LIST',
  `component_type` varchar(64) NOT NULL COMMENT 'interactive component type, enum values: ',
  `default_values` varchar(2048) NOT NULL DEFAULT '' COMMENT 'default values list, e.g., ["create_time","update_time"]',
  `candidates` varchar(2048) COMMENT 'altenative values list, e.g., ["insert", "update"]',
  `rule_metadata_id` bigint(20) COMMENT 'reference to regulation_rule_metadata.id',
  CONSTRAINT `pk_regulation_rule_metadata_property_metadata_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_regulation_rule_metadata_property_metadata_name` UNIQUE KEY (`name`)
) COMMENT = 'rule property metadata';

create table if not exists `regulation_rule_applying`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `organization_id` bigint(20) NOT NULL,
  `ruleset_id` bigint(20) NOT NULL COMMENT 'reference to regulation_ruleset.id',
  `rule_metadata_id` bigint(20) NOT NULL COMMENT 'reference to regulation_rule_metadata.id',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `level` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'SQL_CHECK result level from 1 to 3, and 0 indeicates the rule type is SQL_CONSOLE',
  `applied_dialect_types` varchar(1024) COMMENT 'list values seperated by comma, which indicates what dialect types this rule applied to, null means applied to all. e.g., MYSQL,ORACLE',
  `properties_json` text NOT NULL COMMENT 'all properties of this rule, json format',
  CONSTRAINT `pk_regulation_rule_applying_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_regulation_rule_applying_ruleset_id_rule_metadata_id` UNIQUE KEY (`ruleset_id`, `rule_metadata_id`)
) COMMENT = 'the rule applying properties';

create table if not exists `regulation_ruleset`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `organization_id` bigint(20) NOT NULL,
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `creator_id` bigint(20) NOT NULL,
  `last_modifier_id` bigint NOT NULL,
  CONSTRAINT `pk_regulation_ruleset_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_regulation_ruleset_organization_id_name` UNIQUE KEY (`organization_id`, `name`)
) COMMENT = 'ruleset';



