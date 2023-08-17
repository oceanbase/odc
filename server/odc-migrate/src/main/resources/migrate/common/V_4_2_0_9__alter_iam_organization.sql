-- iam_user and iam_organization relation
CREATE TABLE IF NOT EXISTS `iam_user_organization` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `user_id` bigint(20) NOT NULL COMMENT 'reference to iam_user.id',
  `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
  CONSTRAINT `pk_iam_user_organization_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_iam_user_organization_user_id_organization_id` UNIQUE KEY (`user_id`, `organization_id`)
);


-- add column on iam_organization to represent the type of organization
ALTER TABLE `iam_organization` ADD COLUMN `type` varchar(128) NOT NULL DEFAULT 'TEAM' COMMENT 'organization type, enum values: TEAM, INDIVIDUAL';

alter table `iam_organization` add column `display_name` VARCHAR(256) COMMENT 'i18n alias for display';
update `iam_organization` set `display_name` = '${com.oceanbase.odc.builtin-resource.iam.organization.team.display-name}' where `type` = 'TEAM';
update `iam_organization` set `description` = '${com.oceanbase.odc.builtin-resource.iam.organization.team.description}' where `type` = 'TEAM';
update `iam_organization` set `display_name` = '${com.oceanbase.odc.builtin-resource.iam.organization.individual.display-name}' where `type` = 'INDIVIDUAL';
update `iam_organization` set `description` = '${com.oceanbase.odc.builtin-resource.iam.organization.individual.description}' where `type` = 'INDIVIDUAL';
