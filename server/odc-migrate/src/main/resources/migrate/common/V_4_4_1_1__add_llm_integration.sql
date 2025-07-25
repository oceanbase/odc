CREATE TABLE IF NOT EXISTS `integration_llm_model`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'auto-increment id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, reference `iam_organization`.id',
  `creator_id` bigint(20) NOT NULL COMMENT 'user id of the creator',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  `provider_name` varchar(128) NOT NULL COMMENT 'model provider name',
  `model_name` varchar(128) NOT NULL COMMENT 'model name',
  `display_name` varchar(128) DEFAULT NULL COMMENT 'model display name',
  `model_type` varchar(16) NOT NULL COMMENT 'model type, chat or embedding',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'flag bit, mark whether the model is enabled',
  `is_deprecated` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'flag bit, mark whether the model is deprecated',
  `is_custom` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'flag bit, mark whether the model is user customed',
  `is_function_calling_support` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'flag bit, mark whether the model supports function calling',
  `properties_json` text DEFAULT NULL COMMENT 'model properties, json format',
  `salt` varchar(32) DEFAULT NULL COMMENT 'used to decrypt properties',
  `max_token` int DEFAULT NULL COMMENT 'model max token num',
  `used_token` bigint(20) NOT NULL DEFAULT 0 COMMENT 'used token num',
  `context_size` int DEFAULT NULL COMMENT 'model context size',
  `description` text DEFAULT NULL COMMENT 'model description',
  CONSTRAINT `uk_integration_llm_model_organization_provider_model` UNIQUE KEY (`organization_id`, `provider_name`, `model_name`),
  Key `idx_integration_llm_model_provider_model` (`provider_name`, `model_name`)
) COMMENT='llm model config';

CREATE TABLE IF NOT EXISTS `integration_llm_provider`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'auto-increment id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, reference `iam_organization`.id',
  `creator_id` bigint(20) NOT NULL COMMENT 'user id of the creator',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  `name` varchar(128) NOT NULL COMMENT 'provider name',
  `properties_json` text DEFAULT NULL COMMENT 'provider properties, json format',
  `salt` varchar(32) DEFAULT NULL COMMENT 'used to decrypt properties',
  `description` text DEFAULT NULL COMMENT 'provider description',
  CONSTRAINT `uk_integration_llm_provider_organization_id_name` UNIQUE KEY (`organization_id`, `name`)
) COMMENT='llm provider config';

CREATE TABLE IF NOT EXISTS `integration_ai_config`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT 'auto-increment id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'latest update time',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, reference `iam_organization`.id',
  `last_modifier_id` bigint(20) DEFAULT NULL COMMENT 'user id of the last modifier',
  `is_ai_enabled` tinyint(1) DEFAULT '0' COMMENT 'flag bit, mark whether ai is enabled',
  `is_chat_enabled` tinyint(1) DEFAULT '1' COMMENT 'flag bit, mark whether chat is enabled',
  `is_copilot_enabled` tinyint(1) DEFAULT '1' COMMENT 'flag bit, mark whether copilot is enabled, including nl2sql and sql rewrite',
  `is_completion_enabled` tinyint(1) DEFAULT '1' COMMENT 'flag bit, mark whether sql completion is enabled',
  `default_embedding_model` varchar(256) DEFAULT NULL COMMENT 'default embedding model and factory, e.g. text-embedding-v3@tongyi',
  `default_llm_model` varchar(256) DEFAULT NULL COMMENT 'default llm model and factory, e.g. deepseek-r1@deepseek',
  `default_chat_model` varchar(256) DEFAULT NULL COMMENT 'default chat model and factory, e.g. qwen-max@openai, must support function calling',
  KEY `idx_integration_ai_config_organization_id` (`organization_id`)
) COMMENT='AI config of organization';