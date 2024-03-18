CREATE TABLE IF NOT EXISTS `task_databasechange_changingorder_template`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID task_databasechange_changingorder_template',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Time of creation',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Time of modification',
  `name` varchar(1024) NOT NULL COMMENT 'Name for multiple_database_template',
  `creator_id` bigint(20) NOT NULL COMMENT 'Reference iam_user(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Reference iam_user_organization(id)',
  `database_sequences` varchar(1024) NOT NULL COMMENT 'Database Execution sequence',
  CONSTRAINT `pk_task_databasechange_changingorder_template_id` PRIMARY KEY(`id`),
  UNIQUE KEY `uk_task_databasechange_changingorder_template_creator_id_name` (`creator_id`,`name`)
);