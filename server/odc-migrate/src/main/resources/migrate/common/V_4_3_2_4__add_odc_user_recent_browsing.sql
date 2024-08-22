CREATE TABLE IF NOT EXISTS `odc_user_recent_browsing` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_id` bigint NOT NULL COMMENT 'project id, FK refer to collaboration_project.id',
  `user_id` bigint NOT NULL COMMENT 'user id，FK refer to iam_user.id',
  `item_type` varchar(32) NOT NULL COMMENT 'browsing item type, contains: WROKSHEET,GIT_REPO',
  `item_id` bigint NOT NULL COMMENT 'browsing item id',
  `browse_time` datetime  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'browsing time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_user_item` (`project_id`,`user_id`,`item_type`, `item_id`),
  KEY `index_project_user_item_type_browse_time` (`user_id`, `project_id`, `item_type`, `browse_time`)
) COMMENT = 'user recent browsing time table';
