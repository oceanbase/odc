---
--- v4.1.0 Build relationships directly between user and permission
---
CREATE TABLE IF NOT EXISTS `iam_user_permission`(
	`id` bigint(20) NOT NULL AUTO_INCREMENT,
	`user_id` bigint(20) NOT NULL COMMENT 'User id, references iam_user(id)',
	`permission_id` bigint(20) NOT NULL COMMENT 'Permission id, references iam_permission(id)',
	`creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
	`organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
	`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
	`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
	CONSTRAINT pk_iam_user_permission_id PRIMARY KEY (`id`),
	CONSTRAINT uk_iam_user_permission_user_id_permission_id UNIQUE KEY(`user_id`,`permission_id`)
) COMMENT = 'Record the relationship between users and permissions';
