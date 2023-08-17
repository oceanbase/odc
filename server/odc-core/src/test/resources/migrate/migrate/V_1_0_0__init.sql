
CREATE TABLE IF NOT EXISTS `t_1_for_migrate_test` (
 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
 `name` VARCHAR(64) DEFAULT NULL COMMENT 'name',
 `description` VARCHAR(256) DEFAULT NULL COMMENT 'description',
 PRIMARY KEY (`id`)
 ) ;

CREATE TABLE IF NOT EXISTS `t_2_for_migrate_test` (
 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
 `name` VARCHAR(64) DEFAULT NULL COMMENT 'name',
 `description` VARCHAR(256) DEFAULT NULL COMMENT 'description',
 PRIMARY KEY (`id`)
 ) ;
