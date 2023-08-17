CREATE TABLE IF NOT EXISTS `notification_event`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `trigger_time` datetime NOT NULL comment 'time when the event triggered',
  `status` varchar(128) NOT NULL comment 'status enum, may CREATED, THROWN, CONVERTED, etc.',
  CONSTRAINT pk_notification_event_id PRIMARY KEY (`id`),
  KEY idx_notification_event_org_id_status(`organization_id`,`status`)
) comment = 'notification events';

CREATE TABLE IF NOT EXISTS `notification_event_label`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `event_id`  bigint(20) NOT NULL comment 'event id',
  `key_string` varchar(128) NOT NULL comment 'event label key',
  `value_string` varchar(2048) NOT NULL comment 'event label value',
  CONSTRAINT pk_notification_event_label_id PRIMARY KEY (`id`),
  CONSTRAINT uk_notification_event_event_id_key UNIQUE KEY(`event_id`,`key_string`)
) comment = 'notification events labels';

CREATE TABLE IF NOT EXISTS `notification_channel`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `name` varchar(128) NOT NULL comment 'channel name',
  `type` varchar(128) NOT NULL comment 'channel type, may DingTalkGroupBot, SMS, etc.',
  CONSTRAINT pk_notification_channel_id PRIMARY KEY (`id`),
  CONSTRAINT uk_notification_channel_org_id_name UNIQUE KEY(`organization_id`,`name`)
) comment = 'notification channel configs';

CREATE TABLE IF NOT EXISTS `notification_channel_property`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `channel_id` bigint(20) NOT NULL comment 'channel config id, reference to notification_channel.id',
  `key_string` varchar(128) NOT NULL comment 'property key',
  `value_string` TEXT NOT NULL comment 'prioperty value',
  CONSTRAINT pk_notification_channel_property_id PRIMARY KEY (`id`),
  CONSTRAINT uk_notification_channel_property_channel_id_key UNIQUE KEY(`channel_id`, `key_string`),
  KEY idx_notification_channel_property_channel_id(`channel_id`)
) comment = 'notification channel config properties';

CREATE TABLE IF NOT EXISTS `notification_policy`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `title_template` text NOT NULL comment 'notification message title template, which could contains variables',
  `content_template` text NOT NULL comment 'notification message content template, which could contains variables',
  `match_expression_json` varchar(2048) NOT NULL comment 'indicate if a event matches the expression, json string',
  `to_recipients` varchar(2048) NOT NULL default '' comment 'odc users who will receive this message',
  `cc_recipients` varchar(2048) NOT NULL default '' comment 'odc users who will receive this message by copy',
  CONSTRAINT pk_notification_policy_id PRIMARY KEY (`id`),
  CONSTRAINT uk_notification_policy_organization_id_match_expression UNIQUE KEY(`organization_id`,`match_expression_json`)
) comment = 'notification policy';

CREATE TABLE IF NOT EXISTS `notification_policy_channel_relation`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `notification_policy_id` bigint(20) NOT NULL comment 'notification policy id, reference to notification_policy.id',
  `channel_id` bigint(20) NOT NULL comment 'channel config id, reference to notification_channel.id',
  CONSTRAINT pk_notification_policy_channel_relation_id PRIMARY KEY (`id`),
  CONSTRAINT uk_notification_policy_channel_relation_policy_id_channel_id UNIQUE KEY(`organization_id`,`notification_policy_id`, `channel_id`),
  KEY idx_notification_policy_channel_relation_org_id_policy_id(`organization_id`,`notification_policy_id`)
) comment = 'notification policy and channel config relations';

CREATE TABLE IF NOT EXISTS `notification_message`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL comment 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL comment 'organization id, references iam_organization.id',
  `title` VARCHAR(512) NOT NULL comment 'message title',
  `content` TEXT NOT NULL comment 'message content',
  `event_id` bigint(20) NOT NULL comment 'event id which is converted from; reference to notification_event.id',
  `channel_id` bigint(20) NOT NULL comment 'channel id, reference to notification_channel.id',
  `status` varchar(128) NOT NULL comment 'MessageSendingStatus enum, may CREATED, SENT_SUCCESSFULLY, SENT_FAILED, SENDING, etc.',
  `retry_times` bigint(20) NOT NULL comment 'descirbe how many times spent on resending this message',
  `max_retry_times` bigint(20) NOT NULL comment 'descirbe max times of resending this message',
  `to_recipients` varchar(2048) NOT NULL default '[]' comment 'odc users who will receive this message',
  `cc_recipients` varchar(2048) NOT NULL default '[]' comment 'odc users who will receive this message by copy',
  CONSTRAINT pk_notification_message_id PRIMARY KEY (`id`),
  KEY idx_notification_message(`id`, `channel_id`, `status`)
) comment = 'notification message';
