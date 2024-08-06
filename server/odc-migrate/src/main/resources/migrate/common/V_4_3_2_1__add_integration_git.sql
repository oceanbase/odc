CREATE TABLE IF NOT EXISTS `integration_git_repository` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator_id` bigint(20) NOT NULL COMMENT 'creator user id, references iam_user.id',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, references iam_organization.id',
  `project_id` bigint(20) DEFAULT NULL COMMENT 'project id, references collaboration_project.id',
  `description` varchar(512) DEFAULT NULL COMMENT 'description',
  `name` varchar(512) NOT NULL COMMENT 'repository name',
  `provider_type` varchar(32) NOT NULL COMMENT 'git provider type',
  `provider_url` varchar(128) NOT NULL COMMENT 'git provider url',
  `ssh_url` varchar(512) NOT NULL COMMENT 'ssh clone url, like git@github.com:xxx/xxx.git',
  `clone_url` varchar(512) NOT NULL COMMENT 'https clone url, like https://github.com/xxx/xxx.git',
  `email` varchar(128) NOT NULL COMMENT 'user email for commit',
  `token` varchar(512) NOT NULL COMMENT 'person access token',
  PRIMARY KEY (`id`),
  KEY `integration_git_repository_project_id` (`project_id`)
) COMMENT = 'integration git repository';

CREATE TABLE IF NOT EXISTS `integration_git_repository_stage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `description` varchar(512) DEFAULT NULL COMMENT 'description',
  `repo_id` bigint(20) NOT NULL COMMENT 'git repository id, references integration_git_repository.id',
  `state` varchar(32) NOT NULL COMMENT 'local repository state',
  `branch` varchar(512) NOT NULL COMMENT 'last edit branch',
  `last_commit_id` varchar(64) DEFAULT NULL COMMENT 'git commit revision number',
  `diff_patch_storage` varchar(512) DEFAULT NULL COMMENT 'storage information of patch file',
  `user_id` bigint(20) NOT NULL COMMENT 'user id, references iam_user.id',
  PRIMARY KEY (`id`),
  KEY `integration_git_edit_history_repo_id_user_id` (`repo_id`, `user_id`)
) COMMENT = 'integration git repository stage, to save the stage of workspace';