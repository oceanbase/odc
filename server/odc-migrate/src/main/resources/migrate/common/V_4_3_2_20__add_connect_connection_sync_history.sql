create table if not exists connect_connection_sync_history(
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `organization_id` bigint(20) NOT NULL COMMENT 'reference to iam_organization.id',
  `connection_id` bigint(20) NOT NULL COMMENT 'reference to connect_connection.id',
  `last_sync_result` varchar(32) NOT NULL COMMENT 'last sync result of the sync, see ConnectionSyncResult',
  `last_sync_time` datetime NOT NULL COMMENT 'last sync time of the sync',
  `last_sync_error_reason` varchar(128) DEFAULT NULL COMMENT 'last error reason of the sync; null if sync succeeded',
  `last_sync_error_message` mediumtext DEFAULT NULL COMMENT 'last error message of the sync; null if sync succeeded',
  CONSTRAINT `pk_connect_connection_sync_history_id` PRIMARY KEY (`id`),
  constraint `uk_connection_sync_record_connection_id` unique key(`connection_id`)
);