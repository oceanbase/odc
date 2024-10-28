CREATE TABLE IF NOT EXISTS `history_resource_last_access` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Record organization ID',
  `project_id` bigint NOT NULL COMMENT 'project id, FK refer to collaboration_project.id',
  `user_id` bigint NOT NULL COMMENT 'user id，FK refer to iam_user.id',
  `resource_type` varchar(32) NOT NULL COMMENT 'resource type, contains: ODC_WORKSHEET,ODC_GIT_REPOSITORY,etc.',
  `resource_id` bigint NOT NULL COMMENT 'resource id',
  `last_access_time` datetime  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'last access time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_history_resource_latest_access_org_proj_user_resource` (`organization_id`,`project_id`,`user_id`,`resource_type`, `resource_id`),
  KEY `index_history_resource_latest_access_org_proj_user_rt_lat` (`organization_id`,`project_id`,`user_id`, `resource_type`, `last_access_time`)
) COMMENT = 'user last access resource time table';
