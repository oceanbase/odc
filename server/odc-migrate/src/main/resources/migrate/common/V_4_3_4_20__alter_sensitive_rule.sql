-- Add AI-related columns to table `data_security_sensitive_rule`
alter table `data_security_sensitive_rule`
  add column `ai_sensitive_types` text default null comment 'A list of sensitive data types for AI rules, stored as a JSON array string.';

alter table `data_security_sensitive_rule`
  add column `ai_confidence_threshold` integer default 80 comment 'Confidence threshold for AI rules, with a value range of 0-100.';

alter table `data_security_sensitive_rule`
  add column `ai_custom_prompt` text default null comment 'User-defined custom prompt for AI rules.';

