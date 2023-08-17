---
--- v4.0.0
---

--- data masking rule
CREATE TABLE IF NOT EXISTS `data_masking_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_update_time` datetime DEFAULT NULL COMMENT '用户修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `name` varchar(256) NOT NULL COMMENT '脱敏规则名称',
  `is_enabled` tinyint(1) NOT NULL COMMENT '是否启用',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为内置脱敏规则',
  `type` varchar(32) NOT NULL COMMENT '脱敏规则类型，optional values: HASH, MASK, NULL, PSEUDO, ROUNDING, SUBSTITUTION',
  CONSTRAINT pk_data_masking_rule_id PRIMARY KEY (`id`),
  CONSTRAINT uk_data_masking_rule_organization_id_creator_id_name UNIQUE KEY (`organization_id`, `creator_id`, `name`)
) COMMENT = '脱敏规则记录表';

CREATE TABLE IF NOT EXISTS `data_masking_rule_property` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `key_string` VARCHAR(64) NOT NULL COMMENT 'property的key',
  `value_string` VARCHAR(512) DEFAULT NULL COMMENT 'property的value',
  `rule_id` bigint NOT NULL COMMENT '关联的规则 ID, references data_masking_rule(id)',
  CONSTRAINT pk_data_masking_rule_property_id PRIMARY KEY (`id`),
  CONSTRAINT uk_data_masking_rule_property_organization_id_rule_id_key UNIQUE KEY (`organization_id`, `rule_id`, `key_string`)
) COMMENT = '脱敏规则属性表';

CREATE TABLE IF NOT EXISTS `data_masking_rule_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `is_mask` tinyint(1) DEFAULT NULL COMMENT '此段是否遮掩 / 替换',
  `type` varchar(32) DEFAULT NULL COMMENT '分段类型，optional values: DIGIT, DIGIT_PERCENTAGE, LEFT_OVER, DELIMITER',
  `segments_type` varchar(32) DEFAULT NULL COMMENT '整体分段类型，optional values: CUSTOM, PRE_1_POST_1, PRE_3_POST_2, PRE_3_POST_4, ALL, PRE_3, POST_4',
  `replaced_characters` varchar(32) DEFAULT NULL COMMENT '替换字符',
  `delimiter` varchar(16) DEFAULT NULL COMMENT '分段使用的分隔符',
  `digit_number` int DEFAULT NULL COMMENT '分段位数',
  `digit_percentage` int DEFAULT NULL COMMENT '分段位数比例',
  `ordinal` int DEFAULT NULL COMMENT '此段在所有分段中的顺序，从0开始',
  `rule_id` bigint NOT NULL COMMENT '关联的规则 ID, references data_masking_rule(id)',
  CONSTRAINT pk_data_masking_rule_segment_id PRIMARY KEY (`id`)
) COMMENT = '脱敏分段表';

--- data masking policy
CREATE TABLE IF NOT EXISTS `data_masking_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `name` varchar(256) NOT NULL COMMENT '脱敏策略名称',
  CONSTRAINT pk_data_masking_policy_id PRIMARY KEY (`id`),
  CONSTRAINT uk_data_masking_policy_organization_id_creator_id_name UNIQUE KEY (`organization_id`, `creator_id`, `name`)
) COMMENT = '脱敏策略表';

CREATE TABLE IF NOT EXISTS `data_masking_rule_applying` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `policy_id` bigint NOT NULL COMMENT '脱敏策略 ID, references data_masking_policy(id)',
  `rule_id` bigint DEFAULT NULL COMMENT '脱敏规则 ID, references data_masking_rule(id)',
  `includes` text NOT NULL COMMENT '脱敏包含列表达式',
  `excludes` text DEFAULT NULL COMMENT '脱敏排除列表达式',
  `priority` int NOT NULL COMMENT '脱敏规则应用在所属脱敏策略中的排序，从 0 开始，数值越小表示顺位靠前',
  CONSTRAINT pk_data_masking_rule_applying_id PRIMARY KEY (`id`)
) COMMENT = '脱敏规则应用表';