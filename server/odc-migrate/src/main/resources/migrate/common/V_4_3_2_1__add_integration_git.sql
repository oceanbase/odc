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
  `ssh_address` varchar(512) NOT NULL COMMENT 'ssh clone url, like git@github.com:xxx/xxx.git',
  `clone_address` varchar(512) NOT NULL COMMENT 'https clone url, like https://github.com/xxx/xxx.git',
  `email` varchar(128) NOT NULL COMMENT 'user email for commit',
  `personal_access_token` varchar(512) NOT NULL COMMENT 'personal access token',
  `salt` varchar(32) DEFAULT NULL COMMENT 'used to connect the random value used by the encryption and decryption algorithm of the secret field',
  PRIMARY KEY (`id`),
  KEY `integration_git_repository_organization_id_project_id` (`organization_id`, `project_id`)
) COMMENT = 'integration git repository';

CREATE TABLE IF NOT EXISTS `integration_git_repository_stage` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `description` varchar(512) DEFAULT NULL COMMENT 'description',
  `organization_id` bigint(20) NOT NULL COMMENT 'organization id, references iam_organization.id',
  `repo_id` bigint(20) NOT NULL COMMENT 'git repository id, references integration_git_repository.id',
  `state` varchar(32) NOT NULL COMMENT 'local repository state',
  `branch` varchar(512) DEFAULT NULL COMMENT 'last edit branch',
  `last_commit_id` varchar(64) DEFAULT NULL COMMENT 'git commit revision number',
  `diff_patch_storage` varchar(512) DEFAULT NULL COMMENT 'storage information of patch file',
  `user_id` bigint(20) NOT NULL COMMENT 'user id, references iam_user.id',
  PRIMARY KEY (`id`),
  KEY `integration_git_edit_stage_organization_id_repo_id_user_id` (`organization_id`, `repo_id`, `user_id`)
) COMMENT = 'integration git repository stage, to save the stage of workspace';