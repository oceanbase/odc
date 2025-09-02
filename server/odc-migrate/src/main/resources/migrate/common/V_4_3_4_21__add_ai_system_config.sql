
INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.ai.enabled', 'false', 'Whether AI feature is enabled, disabled by default')
ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.ai.api-key', '', 'AI API key, required when AI feature is enabled')
ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.ai.base-url', 'https://api.openai.com', 'AI API base URL, defaults to OpenAI official API endpoint')
ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.ai.model', 'gpt-3.5-turbo', 'AI model to use, defaults to gpt-3.5-turbo')
ON DUPLICATE KEY UPDATE `id`=`id`;