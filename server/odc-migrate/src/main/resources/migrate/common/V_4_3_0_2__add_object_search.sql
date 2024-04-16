-- Record all database objects such as table, view, function, procedure, package,  package_body, trigger, type, sequence, synonym and public synonym
CREATE TABLE IF NOT EXISTS `database_schema_object` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID of the database object',
  `name` varchar(128) NOT NULL COMMENT 'Name of the database object',
  `type` varchar(32) NOT NULL COMMENT 'Type of the database object, optional values: TABLE, VIEW, FUNCTION, PROCEDURE, PACKAGE, TRIGGER, TYPE, SEQUENCE, SYNONYM, PUBLIC_SYNONYM',
  `database_id` bigint(20) NOT NULL COMMENT 'ID of the related database, refer to connect_database.id',
  `organization_id` bigint(20) NOT NULL COMMENT 'ID of the related organization, refer to iam_organization.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT `pk_database_schema_object_id` PRIMARY KEY (`id`),
  INDEX `idx_database_schema_object_database_id_type_name` (`database_id`, `type`, `name`)
) COMMENT = 'Record all database objects such as table, view, function, procedure, package,  package_body, trigger, type, sequence, synonym and public synonym';

-- Record all columns
CREATE TABLE IF NOT EXISTS `database_schema_column` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID of the column',
  `name` varchar(128) NOT NULL COMMENT 'Name of the column',
  `object_id` bigint(20) NOT NULL COMMENT 'ID of the related table or view, refer to connect_database_object.id',
  `database_id` bigint(20) NOT NULL COMMENT 'ID of the related database, refer to connect_database.id',
  `organization_id` bigint(20) NOT NULL COMMENT 'ID of the related organization, refer to iam_organization.id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT `pk_database_schema_column_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_database_schema_column_database_id_object_id_name` UNIQUE KEY (`database_id`, `object_id`, `name`)
) COMMENT = 'Record all columns';

-- Add columns in `connect_database`
ALTER TABLE `connect_database` ADD COLUMN `object_sync_status` varchar ( 32 ) NOT NULL DEFAULT 'INITIALIZED' COMMENT 'Database object synchronizing status, optional values: INITIALIZED, PENDING, SYNCING, FAILED, SYNCED';
ALTER TABLE `connect_database` ADD COLUMN `object_last_sync_time` datetime DEFAULT NULL COMMENT 'Database object last synchronizing time';
