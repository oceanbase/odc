alter table connect_database modify column connection_id bigint(20) DEFAULT NULL COMMENT 'refernce to connect_connection.id';
alter table connect_database add column `type` varchar(32)  NOT NULL DEFAULT 'PHYSICAL' COMMENT 'optional value: PHYSICAL, LOGICAL';
alter table connect_database add column `alias` varchar(256)  DEFAULT NULL COMMENT 'alias name for database';

create table if not exists `connect_logical_db_meta`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `dialect_type` varchar(64) NOT NULL COMMENT 'physical database dialect type',
    `environment_id` bigint(20) NOT NULL COMMENT 'reference to collaboration_environment.id',
    `connect_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id';
);

create table if not exists `connect_logical_db_physical_db`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `pyhisical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is PHYSICAL',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id'
);

create table if not exists `connect_logical_table`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `name` varchar(256) NOT NULL COMMENT 'logical table name',
    `expression` varchar(1024) NOT NULL COMMENT 'logical table expression, e.g., db_[0-3].tb_[0-3]',
    `structure_signature_sha1` varchar(64) NOT NULL COMMENT 'identify the logical table structure',
    `last_sync_time` datetime DEFAULT NULL COMMENT 'logical table last sync time',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id'
);

create table if not exists `connect_logical_db_logical_table`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `logical_table_id` bigint(20) NOT NULL COMMENT 'reference to database_logical_table.id',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id'
);
