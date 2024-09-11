CREATE TABLE IF NOT EXISTS `collaboration_worksheet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `project_id` bigint NOT NULL COMMENT 'project id, FK refer to collaboration_project.id',
  `creator_id` bigint NOT NULL COMMENT 'creator id，FK refer to iam_user.id',
  `path` varchar(1024) NOT NULL COMMENT 'fully qualified path of worksheet',
  `path_level` int NOT NULL COMMENT 'the number of levels in path',
  `object_id` varchar(1024) DEFAULT NULL COMMENT 'objectName of cloud object storage,or id of local object storage',
  `extension` varchar(64) DEFAULT NULL COMMENT 'worksheet extension',
  `size` bigint DEFAULT NULL COMMENT 'The total size of the file, measured in bytes',
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'edit version,for edit conflict check',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_collaboration_worksheet_project_id_path` (`project_id`,`path`),
  UNIQUE KEY `uk_collaboration_worksheet_object_id` (`object_id`),
  KEY `idx_collaboration_worksheet_project_id_update_time` (`project_id`,`update_time`),
  KEY `idx_collaboration_worksheet_project_id_path_level_path` (`project_id`,`path_level`,`path`)
) COMMENT = 'worksheet for project';