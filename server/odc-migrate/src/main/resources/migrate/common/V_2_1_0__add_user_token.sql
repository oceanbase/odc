--
--  v2.1.0
--
CREATE TABLE IF NOT EXISTS `odc_user_token` (
 `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
 `user_id` BIGINT(20) DEFAULT NULL,
 `token` VARCHAR(256) DEFAULT NULL,
 `status` INT(11) DEFAULT '1',
 `desc` VARCHAR(512) DEFAULT NULL,
 `gmt_create` TIMESTAMP NULL DEFAULT NULL,
 `gmt_modify` TIMESTAMP NULL DEFAULT NULL,
 PRIMARY KEY (`id`),
 UNIQUE KEY `unique_user_id` (`user_id`),
 UNIQUE KEY `unique_token` (`token`)
);
