TRUNCATE TABLE `notification_event`;
TRUNCATE TABLE `notification_channel`;
TRUNCATE TABLE `notification_policy`;
TRUNCATE TABLE `notification_message`;
TRUNCATE TABLE `notification_policy_channel_relation`;

ALTER TABLE `notification_channel` ADD COLUMN `project_id` bigint(20) NOT NULL comment 'project id, references collaboration_project.id';
ALTER TABLE `notification_channel` ADD COLUMN `description` varchar(512) DEFAULT NULL COMMENT 'description';
ALTER TABLE `notification_channel` ADD KEY `notification_channel_project_id` (`project_id`);

ALTER TABLE `notification_policy` MODIFY `title_template` text DEFAULT NULL comment 'deprecated';
ALTER TABLE `notification_policy` MODIFY `content_template` text DEFAULT NULL comment 'deprecated';
ALTER TABLE `notification_policy` MODIFY `to_recipients` varchar(2048) DEFAULT NULL comment 'deprecated';
ALTER TABLE `notification_policy` MODIFY `cc_recipients` varchar(2048) DEFAULT NULL comment 'deprecated';
ALTER TABLE `notification_policy` DROP KEY `uk_notification_policy_organization_id_match_expression`;
ALTER TABLE `notification_policy` ADD COLUMN `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Flag bit, mark whether the triggered rule is enabled';
ALTER TABLE `notification_policy` ADD COLUMN `project_id` bigint(20) NOT NULL comment 'project id, references collaboration_project.id';
ALTER TABLE `notification_policy` ADD COLUMN `event_metadata_id` bigint(20) NOT NULL comment 'notification event metadata id, reference to notification_event_metadata.id';
ALTER TABLE `notification_policy` ADD KEY `notification_policy_project_id` (`project_id`);

ALTER TABLE `notification_event` ADD COLUMN `project_id` bigint(20) NOT NULL comment 'project id, references collaboration_project.id';
ALTER TABLE `notification_event` DROP KEY `idx_notification_event_org_id_status`;

ALTER TABLE `notification_message` ADD COLUMN `project_id` bigint(20) NOT NULL comment 'project id, references collaboration_project.id';
ALTER TABLE `notification_message` ADD COLUMN `error_message` text DEFAULT NULL comment 'error message';
ALTER TABLE `notification_message` ADD COLUMN `event_trigger_time` datetime NOT NULL comment 'time when the event triggered';
ALTER TABLE `notification_message` ADD COLUMN `last_sent_time` datetime DEFAULT NULL comment 'the last attempt to send current message';
ALTER TABLE `notification_message` MODIFY `to_recipients` varchar(2048) DEFAULT NULL comment 'deprecated';
ALTER TABLE `notification_message` MODIFY `cc_recipients` varchar(2048) DEFAULT NULL comment 'deprecated';
ALTER TABLE `notification_message` DROP KEY `idx_notification_message`;
ALTER TABLE `notification_message` ADD KEY `notification_message_channel_id`(`channel_id`);
ALTER TABLE `notification_message` ADD KEY `notification_message_project_id`(`project_id`);

ALTER TABLE `notification_policy_channel_relation` DROP KEY `idx_notification_policy_channel_relation_org_id_policy_id`;
ALTER TABLE `notification_policy_channel_relation` ADD KEY `idx_notification_policy_id`(`notification_policy_id`);

CREATE TABLE IF NOT EXISTS `notification_policy_metadata`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `event_category` varchar(128) NOT NULL comment 'notification event category, such as task',
  `event_name` varchar(128) NOT NULL comment 'notification event name',
  `match_expression_json` varchar(2048) comment 'indicate if a event matches the expression, json string',
  CONSTRAINT `pk_notification_event_metadata_id` PRIMARY KEY (`id`)
) comment = 'notification policy metadata';