--
-- for 3.1.0
--
CREATE TABLE IF NOT EXISTS `odc_user_configuration` (
  `user_id` bigint(20) NOT NULL COMMENT 'User id, references odc_user_info(id)',
  `key` varchar(256) NOT NULL COMMENT 'User config key, enum value, defined by application',
  `value` varchar(1024) DEFAULT NULL COMMENT 'User config value',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `description` varchar(1024) DEFAULT NULL COMMENT 'Notice info',
  PRIMARY KEY (`user_id`, `key`)
) ;
