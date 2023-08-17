-- 触发事件的元信息
CREATE TABLE IF NOT EXISTS `automation_event_metadata`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Automation triggered event ID',
  `name` varchar(256) NOT NULL COMMENT 'Automation trigger event name',
  `variable_names` varchar(1024) COMMENT 'Variable names needed by condition',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '',
  `is_hidden` tinyint(1) NOT NULL COMMENT 'Mark whether event is hidden or not',
  `description` varchar(1024) DEFAULT NULL,
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references iam_user(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_auto_trigger_event_id PRIMARY KEY (`id`),
  CONSTRAINT uk_auto_trigger_event_name UNIQUE KEY (`name`)
) COMMENT = 'Automation triggered event metadata';

-- 自动触发规则表，存储触发规则信息 --
CREATE TABLE IF NOT EXISTS `automation_rule`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Automation triggered rule ID',
  `name` varchar(256) NOT NULL COMMENT 'Automation triggered rule name',
  `event_id` bigint(20) NOT NULL COMMENT 'Automation triggered event id, reference automation_event_metadata(id)',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Flag bit, mark whether the triggered rule is enabled',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Flag bit, mark whether the triggered rule is built-in',
  `description` varchar(1024) DEFAULT NULL COMMENT 'Notice info',
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references iam_user(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_auto_trigger_rule_id PRIMARY KEY (`id`),
  CONSTRAINT uk_auto_trigger_rule_name_organization_id UNIQUE KEY (`organization_id`, `name`)
) COMMENT = 'Automation rules';

-- 触发条件信息
CREATE TABLE IF NOT EXISTS `automation_condition`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Automation triggered condition ID',
  `rule_id` bigint(20) NOT NULL COMMENT 'Rule id, reference automation_rule(id)',
  `object` varchar(128) NOT NULL COMMENT 'Automation triggered condition match object',
  `expression` varchar(512) NOT NULL COMMENT 'Automation triggered condition match expression',
  `operation` varchar(64) NOT NULL COMMENT 'Automation triggered condition match operation',
  `value` varchar(128) NOT NULL COMMENT 'Automation triggered condition match value',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Flag bit, mark whether the condition is enabled',
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references iam_user(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_auto_trigger_condition_id PRIMARY KEY (`id`)
) COMMENT = 'Automation conditions';

-- 触发后的行为信息
CREATE TABLE IF NOT EXISTS `automation_action`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Automation triggered action ID',
  `rule_id` bigint(20) NOT NULL COMMENT 'Rule id, reference automation_rule(id)',
  `action` varchar(64) NOT NULL COMMENT 'Action name',
  `args_json_array` varchar(1024) COMMENT 'Base argument needed by action',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Flag bit, mark whether the action is enabled',
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'Last modifier id, references iam_user(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT pk_auto_trigger_action_id PRIMARY KEY (`id`)
) COMMENT = 'Automation actions';