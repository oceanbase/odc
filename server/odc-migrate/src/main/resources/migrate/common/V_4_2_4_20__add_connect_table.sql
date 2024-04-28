CREATE TABLE IF NOT EXISTS `connect_table` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `database_id` bigint(20) NOT NULL comment 'relation connect_database id column',
  `name` varchar(256) NOT NULL comment 'table name',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `sync_status` varchar(64) NOT NULL COMMENT 'synchronizing status, enum values: PENDING, SUCCEEDED, FAILED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_db_id_tablename` (`database_id`, `name`)
);