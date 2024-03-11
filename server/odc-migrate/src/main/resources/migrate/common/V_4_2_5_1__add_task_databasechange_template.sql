CREATE TABLE IF NOT EXISTS `task_databasechange_template`(
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID for multiple_database_template',
  `create_time` datetime NOT NULL COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT NULL COMMENT 'Record modification time',
  `name` varchar(1024) NOT NULL COMMENT 'Name for multiple_database_template',
  `creator_id` bigint(20) NOT NULL COMMENT 'Reference iam_user(id)',
  `organization_id` bigint(20) NOT NULL COMMENT 'Reference iam_user_organization(id)',
  `database_sequences` varchar(10240) NOT NULL COMMENT 'Database Execution sequence',
  CONSTRAINT `pk_multiple_database_template` PRIMARY KEY(`id`),
  UNIQUE KEY `uk_creator_id_name` (`creator_id`,`name`)
);