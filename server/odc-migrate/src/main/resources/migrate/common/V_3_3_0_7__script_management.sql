CREATE TABLE IF NOT EXISTS `script_meta` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `object_id` VARCHAR(64) NOT NULL COMMENT '关联的对象ID，FK refer to objectstorage_object_metadata.object_id',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `object_name` VARCHAR(256) DEFAULT NULL COMMENT '脚本名称，FK refer to objectstorage_object_metadata.object_name',
  `bucket_name` VARCHAR(64) DEFAULT NULL COMMENT '脚本名称，FK refer to objectstorage_object_metadata.bucket_name',
  `content_abstract` VARCHAR(256) DEFAULT NULL COMMENT '脚本摘要，截取前256个字符',
  `content_length` bigint NOT NULL COMMENT '文件长度，单位为字节，FK refer to objectstorage_object_metadata.total_length',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_script_meta_object_id` (`object_id`),
  KEY `index_script_meta_creator_id_bucket_name`(`creator_id`, `bucket_name`)
) COMMENT = '脚本元数据';