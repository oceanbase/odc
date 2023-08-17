-- below collaboration module ddl
CREATE TABLE IF NOT EXISTS `collaboration_project` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) NOT NULL,
  `is_archived` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'flag if the project is archived, default not archived',
  `description` varchar(2048) DEFAULT NULL,
  `creator_id` bigint(20) NOT NULL,
  `last_modifier_id` bigint(20) NOT NULL,
  `organization_id` bigint(20) NOT NULL,
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
  CONSTRAINT `pk_collaboration_project_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_collaboration_project_org_id_name` UNIQUE KEY (`organization_id`, `name`)
);

CREATE TABLE IF NOT EXISTS `collaboration_environment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `style` varchar(64) NOT NULL DEFAULT 'RED' COMMENT 'environment style for front-end, enum value: GREEN, YELLOW, RED',
  `ruleset_id` bigint(20) NOT NULL COMMENT 'reference to regulation_ruleset.id',
  `creator_id` bigint(20) NOT NULL,
  `last_modifier_id` bigint(20) NOT NULL,
  `organization_id` bigint(20) NOT NULL,
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0',
  CONSTRAINT `pk_collaboration_environment_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_collaboration_org_id_name` UNIQUE KEY (`organization_id`, `name`)
);

CREATE TABLE IF NOT EXISTS `connect_database` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `database_id` varchar(256) NOT NULL,
  `is_existed` tinyint(1) NOT NULL DEFAULT '1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `name` varchar(256) NOT NULL,
  `organization_id` bigint(20) NOT NULL,
  `project_id` bigint(20) DEFAULT NULL COMMENT 'refernce to collaboration_project.id, null means this database does not belong to any project',
  `connection_id` bigint(20) NOT NULL COMMENT 'refernce to connect_connection.id',
  `environment_id` bigint(20) NOT NULL COMMENT 'refernce to collaboration_environment.id',
  `sync_status` varchar(64) NOT NULL COMMENT 'synchronizing status, enum values: PENDING, SUCCEEDED, FAILED',
  `last_sync_time` datetime NOT NULL COMMENT 'last synchronizing time',
  `charset_name` varchar(128) NOT NULL COMMENT 'database charset name',
  `collation_name` varchar(128) NOT NULL COMMENT 'database collation name',
  `table_count` bigint(20) COMMENT 'table count which belongs to the database',
  CONSTRAINT `pk_connect_database_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_connect_database_connection_id_db_id` UNIQUE KEY (`connection_id`, `database_id`)
);

-- visible scope would be deprecated since 4.2.0, so drop the not null constraint of it
alter table `connect_connection` modify `visible_scope` varchar(64) DEFAULT 'ORGANIZATION' COMMENT '[deprecated since ODC 4.2.0] Visible scope, enum: PRIVATE, ORGANIZATION';

-- owner_id would be deprecated since 4.2.0, so drop the not null constraint of it
alter table `connect_connection` modify `owner_id` bigint(20) COMMENT '[deprecated since ODC 4.2.0] Owner id';

-- add environment_id
alter table `connect_connection` add column `environment_id` bigint(20) NOT NULL DEFAULT -1 COMMENT 'refernce to collaboration_environment, default value is -1 which means not belongs to any env and needs to migrate';

-- visible scope would be deprecated since 4.2.0, so drop the unique key of `connect_connection`
-- TODO: add unique key which consists of organization_id and name, and pay attention to the condition where name conflicts in existed data
alter table `connect_connection` drop index uk_connect_connection_visible_scope_owner_id_name;