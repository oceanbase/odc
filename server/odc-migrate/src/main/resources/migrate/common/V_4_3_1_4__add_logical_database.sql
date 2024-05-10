alter table connect_database modify column connection_id bigint(20) DEFAULT NULL COMMENT 'refernce to connect_connection.id';
alter table connect_database modify column connection_id bigint(20) DEFAULT NULL COMMENT 'refernce to connect_connection.id';
alter table connect_database modify column connection_id bigint(20) DEFAULT NULL COMMENT 'refernce to connect_connection.id';
alter table connect_database add column `type` varchar(32)  NOT NULL DEFAULT 'PHYSICAL' COMMENT 'optional value: PHYSICAL, LOGICAL';
alter table connect_database add column `alias` varchar(256) DEFAULT NULL COMMENT 'alias name for database';

create table if not exists `connect_logical_db_meta`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `dialect_type` varchar(64) NOT NULL COMMENT 'physical database dialect type',
    `environment_id` bigint(20) NOT NULL COMMENT 'reference to collaboration_environment.id',
    `connect_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connect_logical_db_meta_db_id` (`connect_database_id`)
);

create table if not exists `connect_logical_db_physical_db`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `pyhisical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is PHYSICAL',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connect_logical_db_physical_db_ldi_pdi` (`logical_database_id`, `pyhisical_database_id`)
);

create table if not exists `connect_logical_table`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `name` varchar(256) NOT NULL COMMENT 'logical table name',
    `expression` varchar(1024) NOT NULL COMMENT 'logical table expression, e.g., db_[0-3].tb_[0-3]',
    `last_sync_time` datetime DEFAULT NULL COMMENT 'logical table last sync time',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connect_logical_table_ldi_name` (`logical_database_id`, `name`)
);

create table if not exists `connect_logical_table_physical_table`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_table_id` bigint(20) NOT NULL COMMENT 'reference to database_logical_table.id',
    `physical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is PHYSICAL',
    `physical_database_name` varchar(256) NOT NULL COMMENT 'reference to connect_database.name whose type is PHYSICAL',
    `physical_table_name` varchar(256) NOT NULL COMMENT 'physical table name',
    `is_consistent` tinyint(1) NOT NULL DEFAULT 1 COMMENT '0: inconsistent, 1: consistent',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connect_logical_table_physical_table_lti_pdi_ptn` (`logical_table_id`, `physical_database_id`, `physical_table_name`)
);