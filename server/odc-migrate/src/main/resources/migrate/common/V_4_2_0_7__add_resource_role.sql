CREATE TABLE IF NOT EXISTS `iam_user_resource_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `organization_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `resource_id` varchar(256) NOT NULL,
  `resource_role_id` bigint(20) NOT NULL COMMENT 'refernce to iam_resource_role.id',
  CONSTRAINT `pk_iam_user_resource_role_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_iam_user_resource_role_user_id_resource_id_role_id` UNIQUE KEY (`user_id`, `resource_id`, `resource_role_id`)
) COMMENT = '用户和具体某个资源的资源角色的关联表';

-- 不需要加组织 id 隔离，因为所有组织的元数据是一样的
CREATE TABLE IF NOT EXISTS `iam_resource_role` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resource_type` varchar(256) NOT NULL,
  `role_name` varchar(256) NOT NULL COMMENT 'enum values,ref to ResourceRoleName: OWNER, DBA, DEVELOPER, etc',
  `description` varchar(2048) DEFAULT NULL,
  CONSTRAINT `pk_iam_resource_role_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_iam_resource_role_resource_type_name` UNIQUE KEY (`resource_type`, `role_name`)
) COMMENT = '资源角色元数据';

CREATE TABLE IF NOT EXISTS `iam_resource_role_permission` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resource_role_id` bigint(20) NOT NULL COMMENT 'reference to iam_resource_role.id',
  `action` varchar(2048) NOT NULL,
  CONSTRAINT `pk_iam_resource_permission_id` PRIMARY KEY (`id`)
) COMMENT = '资源角色对应的权限元数据';