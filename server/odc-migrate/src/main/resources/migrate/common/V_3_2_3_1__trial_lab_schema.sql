create table if not exists `connect_connection_access` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `connection_id` bigint NOT NULL,
    `user_id` bigint NOT NULL COMMENT 'User who access the connect session',
    `last_access_time` DATETIME NOT NULL COMMENT 'Last access time of the connect session',
    CONSTRAINT pk_connect_connection_access_id PRIMARY KEY (`id`),
    CONSTRAINT uk_connect_connection_access UNIQUE KEY (`connection_id`, `user_id`)
);