--
-- Data security management related DDL
--

CREATE TABLE IF NOT EXISTS `data_security_sensitive_column` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for the sensitive column',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT '1' COMMENT 'Mark whether the sensitive column is enabled',
  `database_id` BIGINT(20) NOT NULL COMMENT 'Record the ID of the database to which the sensitive column belongs',
  `table_name` VARCHAR(128) NOT NULL COMMENT 'Record the name of the table to which the sensitive column belongs',
  `column_name` VARCHAR(128) NOT NULL COMMENT 'Record the name of the column to which the sensitive column belongs',
  `sensitive_level` VARCHAR(32) NOT NULL DEFAULT 'HIGH' COMMENT 'Record the sensitive level of the sensitive column',
  `masking_algorithm_id` BIGINT(20) NOT NULL COMMENT 'Record the masking algorithm used by the sensitive column',
  `creator_id` BIGINT(20) NOT NULL COMMENT 'Record creator ID',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Record organization ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT `pk_data_security_sensitive_column_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_data_security_sensitive_column_database_id_table_column` UNIQUE KEY (`database_id`, `table_name`, `column_name`)
) COMMENT 'Record sensitive column info';

CREATE TABLE IF NOT EXISTS `data_security_sensitive_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for the sensitive rule',
  `name` VARCHAR(128) NOT NULL COMMENT 'Name of the sensitive rule',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT '1' COMMENT 'Mark whether the sensitive rule is enabled',
  `is_builtin` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'Mark whether the sensitive rule is builtin',
  `type` VARCHAR(32) NOT NULL COMMENT 'Record the type of the sensitive rule',
  `database_regex_expression` VARCHAR(256) DEFAULT NULL COMMENT 'Record the database name match regex expression, valid when type="REGEX"',
  `table_regex_expression` VARCHAR(256) DEFAULT NULL COMMENT 'Record the table name match regex expression, valid when type="REGEX"',
  `column_regex_expression` VARCHAR(256) DEFAULT NULL COMMENT 'Record the column name match regex expression, valid when type="REGEX"',
  `column_comment_regex_expression` VARCHAR(256) DEFAULT NULL COMMENT 'Record the column comment match regex expression, valid when type="REGEX"',
  `groovy_script` VARCHAR(2048) DEFAULT NULL COMMENT 'Record the groovy script, valid when type="GROOVY"',
  `path_includes` VARCHAR(512) DEFAULT NULL COMMENT 'Record the path includes expression, valid when type="PATH"',
  `path_excludes` VARCHAR(512) DEFAULT NULL COMMENT 'Record the path excludes expression, valid when type="PATH"',
  `sensitive_level` VARCHAR(32) NOT NULL DEFAULT 'HIGH' COMMENT 'Record the sensitive level of the sensitive rule',
  `masking_algorithm_id` BIGINT(20) NOT NULL  COMMENT 'Record the masking algorithm used by the sensitive rule',
  `description` VARCHAR(1024) DEFAULT NULL COMMENT 'Description of the sensitive rule',
  `project_id` BIGINT(20) NOT NULL COMMENT 'Record the project ID to which the sensitive rule belongs',
  `creator_id` BIGINT(20) NOT NULL COMMENT 'Record creator ID',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Record organization ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT `pk_data_security_sensitive_rule_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_data_security_sensitive_rule_name_project_id` UNIQUE KEY (`name`, `project_id`)
) AUTO_INCREMENT = 10000 COMMENT 'Record sensitive rule info';

CREATE TABLE IF NOT EXISTS `data_security_masking_algorithm` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for the masking algorithm',
  `name` VARCHAR(128) NOT NULL COMMENT 'name of the masking algorithm',
  `is_enabled` TINYINT(1) NOT NULL DEFAULT '1' COMMENT 'Mark whether the masking algorithm is enabled',
  `is_builtin` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'Mark whether the masking algorithm is builtin',
  `type` VARCHAR(32) NOT NULL COMMENT 'Record the type of the masking algorithm',
  `segments_type` VARCHAR(32) DEFAULT NULL COMMENT 'Record the segment type of the masking algorithm, valid when type="MASK"|"SUBSTITUTION"',
  `substitution` VARCHAR(128) DEFAULT NULL COMMENT 'Record the replacement characters of the masking algorithm, valid when type="MASK"|"SUBSTITUTION"',
  `charsets` VARCHAR(128) DEFAULT NULL COMMENT 'Record the charsets of the masking algorithm, valid when type="PSEUDO"',
  `hash_type` VARCHAR(32) DEFAULT NULL COMMENT 'Record the hash type of the masking algorithm, valid when type="HASH"',
  `is_decimal` TINYINT(1) DEFAULT NULL COMMENT 'Record the decimal of the masking algorithm, valid when type="ROUNDING"',
  `rounding_precision` TINYINT(8) DEFAULT NULL COMMENT 'Record the precision of the masking algorithm, valid when type="ROUNDING"',
  `sample_content` VARCHAR(1024) NOT NULL COMMENT 'Record the sample content',
  `masked_content` VARCHAR(1024) DEFAULT NULL COMMENT 'Record the masked value of sample content',
  `creator_id` BIGINT(20) NOT NULL COMMENT 'Record creator ID',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Record organization ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT `pk_data_security_masking_algorithm_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_data_security_masking_algorithm_name_organization_id` UNIQUE KEY (`name`, `organization_id`)
) AUTO_INCREMENT = 10000 COMMENT 'Record masking algorithm info';

CREATE TABLE IF NOT EXISTS `data_security_masking_algorithm_segment` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for the masking algorithm segment',
  `masking_algorithm_id` BIGINT(20) NOT NULL COMMENT 'Record the id of the masking algorithm to which the segment belongs',
  `is_mask` TINYINT(1) DEFAULT NULL COMMENT 'Mark whether the segment needs to be masked',
  `type` VARCHAR(32) NOT NULL COMMENT 'Record the type of the segment',
  `replaced_characters` VARCHAR(64) DEFAULT NULL COMMENT 'Record the replacement characters, valid when is_mask=1',
  `delimiter` VARCHAR(16) DEFAULT NULL COMMENT 'Record the delimiter of the segment, valid when type="DELIMITER"',
  `digit_number` INT DEFAULT NULL COMMENT 'Record the digit number of the segment, valid when type="DIGIT"',
  `digit_percentage` INT DEFAULT NULL COMMENT 'Record the digit percentage of the segment, valid when type="DIGIT_PERCENTAGE"',
  `ordinal` INT DEFAULT NULL COMMENT 'Record the ordinal of the segment',
  `creator_id` BIGINT(20) NOT NULL COMMENT 'Record creator ID',
  `organization_id` BIGINT(20) NOT NULL COMMENT 'Record organization ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  CONSTRAINT `pk_data_security_masking_algorithm_segment_id` PRIMARY KEY (`id`)
) COMMENT 'Record masking algorithm segment info';
