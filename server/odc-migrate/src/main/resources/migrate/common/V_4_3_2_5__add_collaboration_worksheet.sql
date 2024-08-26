CREATE TABLE IF NOT EXISTS `collaboration_worksheet` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `project_id` bigint NOT NULL COMMENT 'project id, FK refer to collaboration_project.id',
  `creator_id` bigint NOT NULL COMMENT 'creator id，FK refer to iam_user.id',
  `path` varchar(1024) NOT NULL COMMENT 'fully qualified path of worksheet',
  `level_num` int NOT NULL COMMENT 'the number of levels in path',
  `object_id` varchar(1024) DEFAULT NULL COMMENT 'objectName of cloud object storage,or id of local object storage',
  `extension` varchar(64) DEFAULT NULL COMMENT 'worksheet extension',
  `total_length` bigint DEFAULT NULL COMMENT 'The total size of the file, measured in bytes',
  `version` bigint NOT NULL DEFAULT 0 COMMENT 'edit version,for edit conflict check',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_id_path` (`project_id`,`path`),
  UNIQUE KEY `uk_object_id` (`object_id`),
  KEY `idx_project_id_level_num_path` (`project_id`,`level_num`,`path`)
) COMMENT = 'worksheet for project';