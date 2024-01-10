DROP TABLE IF EXISTS `notification_channel`;
CREATE TABLE IF NOT EXISTS `notification_channel`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL COMMENT 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, references iam_organization.id',
  `name` varchar(128) NOT NULL COMMENT 'channel name',
  `type` varchar(128) NOT NULL COMMENT 'channel type, may DingTalk, SMS, etc.',
  `project_id` bigint(20) NOT NULL COMMENT 'project id, references collaboration_project.id',
  `description` varchar(512) DEFAULT NULL COMMENT 'description',
  CONSTRAINT `pk_notification_channel_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_notification_channel_project_id_name` UNIQUE KEY(`project_id`,`name`)
) COMMENT = 'notification channel configs';

DROP TABLE IF EXISTS `notification_policy`;
CREATE TABLE IF NOT EXISTS `notification_policy`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `title_template` text DEFAULT NULL comment 'notification message title template, which could contains variables',
  `content_template` text DEFAULT NULL comment 'notification message content template, which could contains variables',
  `match_expression` varchar(2048) NOT NULL comment 'indicate if a event matches the expression',
  `to_users` varchar(2048) DEFAULT NULL comment 'odc users who will receive this message',
  `cc_users` varchar(2048) DEFAULT NULL comment 'odc users who will receive this message by copy',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Flag bit, mark whether the triggered rule is enabled',
  `project_id` bigint(20) NOT NULL COMMENT 'project id, references collaboration_project.id',
  `policy_metadata_id` bigint(20) NOT NULL COMMENT 'notification policy metadata id, reference to notification_policy_metadata.id',
  CONSTRAINT `pk_notification_policy_id` PRIMARY KEY (`id`),
  KEY `idx_notification_policy_project_id` (`project_id`)
) COMMENT = 'notification policy';

CREATE TABLE IF NOT EXISTS `notification_policy_metadata`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `event_category` varchar(128) NOT NULL comment 'notification event category, such as TASK',
  `event_name` varchar(128) NOT NULL comment 'notification event name',
  `match_expression` varchar(2048) comment 'indicate if a event matches the expression',
  CONSTRAINT `pk_notification_policy_metadata_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_policy_metadata_event_category_name` UNIQUE KEY (`event_category`, `event_name`)
) comment = 'notification policy metadata';

DROP TABLE IF EXISTS `notification_event`;
CREATE TABLE IF NOT EXISTS `notification_event`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `project_id` bigint(20) NOT NULL comment 'project id, references collaboration_project.id',
  `trigger_time` datetime NOT NULL comment 'time when the event triggered',
  `status` varchar(128) NOT NULL comment 'status enum, may CREATED, THROWN, CONVERTED, etc.',
  CONSTRAINT `pk_notification_event_id` PRIMARY KEY (`id`),
  KEY `idx_notification_event_status_time`(`status`, `trigger_time`)
) comment = 'notification events';

DROP TABLE IF EXISTS `notification_message`;
CREATE TABLE IF NOT EXISTS `notification_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL COMMENT 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, references iam_organization.id',
  `title` varchar(512) NOT NULL COMMENT 'message title',
  `content` text NOT NULL COMMENT 'message content',
  `channel_id` bigint(20) NOT NULL COMMENT 'channel id, reference to notification_channel.id',
  `status` varchar(128) NOT NULL COMMENT 'MessageSendingStatus enum, may CREATED, SENT_SUCCESSFULLY, SENT_FAILED, SENDING, etc.',
  `retry_times` bigint(20) DEFAULT 0 NOT NULL COMMENT 'describe how many times spent on resending this message',
  `max_retry_times` bigint(20) NOT NULL COMMENT 'describe max times of resending this message',
  `project_id` bigint(20) NOT NULL COMMENT 'project id, references collaboration_project.id',
  `to_recipients` varchar(2048) DEFAULT NULL comment 'odc users who will receive this message',
  `cc_recipients` varchar(2048) DEFAULT NULL comment 'odc users who will receive this message by copy',
  `error_message` varchar(2048) DEFAULT NULL comment 'reason for message sending failure',
  `last_sent_time` datetime DEFAULT NULL COMMENT 'the last attempt to send current message',
  CONSTRAINT pk_notification_message_id PRIMARY KEY (`id`),
  KEY `idx_status_retry_times_max_retry_times` (`status`, `retry_times`, `max_retry_times`),
  KEY `idx_notification_message_channel_id` (`channel_id`),
  KEY `idx_notification_message_project_id` (`project_id`)
) COMMENT = 'notification message';

DROP TABLE IF EXISTS `notification_policy_channel_relation`;
CREATE TABLE IF NOT EXISTS `notification_policy_channel_relation`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `notification_policy_id` bigint(20) NOT NULL comment 'notification policy id, reference to notification_policy.id',
  `channel_id` bigint(20) NOT NULL comment 'channel config id, reference to notification_channel.id',
  CONSTRAINT `pk_notification_policy_channel_relation_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_notification_policy_channel_relation_policy_id_channel_id` UNIQUE KEY(`organization_id`,`notification_policy_id`, `channel_id`),
  KEY `idx_notification_policy_id`(`notification_policy_id`)
) comment = 'notification policy and channel config relations';

CREATE TABLE IF NOT EXISTS `notification_message_sending_history`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `message_id` bigint(20) NOT NULL comment 'message id, references notification_message.id',
  `status` varchar(128) NOT NULL COMMENT 'MessageSendingStatus enum, may SENT_SUCCESSFULLY or SENT_FAILED',
  `error_message` text DEFAULT NULL COMMENT 'error message',
  CONSTRAINT `pk_notification_message_sending_history_id` PRIMARY KEY (`id`),
  KEY `idx_notification_message_id_status_create_time`(`message_id`, `status`, `create_time`)
) comment = 'notification message sending history';