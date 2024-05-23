alter table connect_database modify column connection_id bigint(20) DEFAULT NULL COMMENT 'refernce to connect_connection.id';
alter table connect_database add column `type` varchar(32)  NOT NULL DEFAULT 'PHYSICAL' COMMENT 'optional value: PHYSICAL, LOGICAL';
alter table connect_database add column `alias` varchar(256) DEFAULT NULL COMMENT 'alias name for database';
alter table `connect_database` modify `last_sync_time` datetime default null comment 'last synchronizing time';
alter table `connect_database` add column `dialect_type` varchar(64) DEFAULT NULL COMMENT 'database dialect type';

create table if not exists `connect_database_mapping`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is LOGICAL',
    `physical_database_id` bigint(20) NOT NULL COMMENT 'reference to connect_database.id whose type is PHYSICAL',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connect_logical_db_physical_db_ldi_pdi` (`logical_database_id`, `physical_database_id`)
);

create table if not exists `database_table_mapping`(
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `logical_table_id` bigint(20) NOT NULL COMMENT 'reference to database_schema_object.id',
    `physical_database_id` bigint(20) NOT NULL COMMENT 'reference to database_schema_object.database_id',
    `physical_database_name` varchar(256) NOT NULL COMMENT 'reference to connect_database.name',
    `physical_table_name` varchar(128) NOT NULL COMMENT 'reference to database_schema_object.name',
    `expression` varchar(1024) NOT NULL COMMENT 'logical table expression, e.g., db_[0-3].tb_[0-3]',
    `is_consistent` tinyint(1) NOT NULL DEFAULT 1 COMMENT '0: inconsistent, 1: consistent',
    `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_connect_logical_table_physical_table_lti_pdi_ptn` (`logical_table_id`, `physical_database_id`, `physical_table_name`)
);