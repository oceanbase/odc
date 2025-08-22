CREATE TABLE IF NOT EXISTS knowledge_base_schema_key_value (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id',
  `database_id` BIGINT(20) NOT NULL COMMENT 'reference to connect_database.id',
  `key_content` VARCHAR(128) NOT NULL COMMENT 'key',
  `value_content` MEDIUMTEXT NOT NULL COMMENT 'value',
  `tag` VARCHAR(16) NOT NULL COMMENT '标签，标记索引来源（表、字段、骨架）',
  CONSTRAINT pk_knowledge_base_schema_key_value PRIMARY KEY (id),
  KEY `idx_knowledge_base_schema_key_value_key_db_tag` (`key_content`, `database_id`, `tag`)
);