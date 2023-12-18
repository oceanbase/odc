create table if not exists `regulation_default_rule_applying`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ruleset_name` varchar(256) NOT NULL COMMENT 'reference to regulation_ruleset.name',
  `rule_metadata_id` bigint(20) NOT NULL COMMENT 'reference to regulation_rule_metadata.id',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `level` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'SQL_CHECK result level from 1 to 3, and 0 indeicates the rule type is SQL_CONSOLE',
  `applied_dialect_types` varchar(1024) COMMENT 'list values seperated by comma, which indicates what dialect types this rule applied to, null means applied to all. e.g., MYSQL,ORACLE',
  `properties_json` text NOT NULL COMMENT 'all properties of this rule, json format',
  CONSTRAINT `pk_regulation_default_rule_applying_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_regulation_default_rule_applying_name_id` UNIQUE KEY (`ruleset_name`, `rule_metadata_id`)
) COMMENT = 'the default value of rule applyings';