create TABLE IF NOT EXISTS `objectstorage_object_block` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `object_id` varchar(64) NOT NULL COMMENT '关联的文件ID，FK refer to storage_object_meta.object_id',
  `block_index` BIGINT NOT NULL COMMENT '当前文件块索引编号',
  `length` BIGINT NOT NULL COMMENT '当前文件块大小，单位为 bytes',
  `content` MEDIUMBLOB DEFAULT NULL COMMENT '文件块内容（MEDIUMBLOB 存储长度上限16M）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_objectstorage_object_block_object_id_index` (`object_id`, `block_index`)
) COMMENT = '文件块';

create TABLE IF NOT EXISTS `objectstorage_object_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `object_id` varchar(64) NOT NULL COMMENT '关联的文件ID',
  `bucket_name` VARCHAR(64) NOT NULL COMMENT '存储空间名称',
  `creator_id` BIGINT NOT NULL COMMENT '创建者 id，FK refer to iam_user.id',
  `object_name` VARCHAR(1024) NOT NULL COMMENT '文件对象名',
  `extension` VARCHAR(64) DEFAULT NULL COMMENT '后缀名（保留字段，未来方便检索文件）',
  `sha1` VARCHAR(64) NULL COMMENT '文件SHA-1',
  `total_length` BIGINT NOT NULL COMMENT '文件总大小，单位为 bytes',
  `split_length` BIGINT NOT NULL COMMENT '文件块拆分大小，单位为 bytes',
  `status` varchar(50) NOT NULL DEFAULT 'INIT' COMMENT '文件存储状态： INIT, FINISHED',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_objectstorage_object_metadata_object_id` (`object_id`),
  KEY `index_objectstorage_object_metadata_bucket_name_object_id`(`bucket_name`, `object_id`)
) COMMENT = '对象存储元数据';

create TABLE IF NOT EXISTS `objectstorage_bucket` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON update CURRENT_TIMESTAMP,
  `name` VARCHAR(64) NOT NULL COMMENT '存储空间名称',
  `creator_id` BIGINT NOT NULL COMMENT '创建者 id，FK refer to iam_user.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_objectstorage_bucket_name` (`name`)
) COMMENT = '对象存储空间';