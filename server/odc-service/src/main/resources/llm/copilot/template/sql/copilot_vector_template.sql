CREATE TABLE IF NOT EXISTS copilot_vector_${DATABASE_ID} (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id',
  `database_id` BIGINT(20) NOT NULL COMMENT 'reference to connect_database.id',
  `table_name` VARCHAR(128) NOT NULL COMMENT '所属表名',
  `embedding` VECTOR(1024) NOT NULL COMMENT '向量',
  `content` VARCHAR(4096) NOT NULL COMMENT '原文本内容',
  `content_sha1` VARCHAR(4096) NOT NULL COMMENT '原文本内容 sha1 值',
  `tag` VARCHAR(16) NOT NULL COMMENT '标签，标记索引来源（表、字段、骨架）',
  PRIMARY KEY (id),
  KEY `idx_tag_${DATABASE_ID}` (`tag`),
  VECTOR KEY `idx_vec_${DATABASE_ID}` (`embedding`) WITH (distance=l2, type=hnsw, lib=vsag)
);