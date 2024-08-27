--
-- Add task_resource table
--
CREATE TABLE IF NOT EXISTS `task_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  `region` varchar(1024) NOT NULL COMMENT 'region name',
  `group_name` varchar(1024) NOT NULL COMMENT 'group name',
  `namespace` varchar(1024) NOT NULL COMMENT 'namespace name',
  `name` varchar(1024) NOT NULL COMMENT 'resource name',
  `resource_mode` varchar(128) NOT NULL COMMENT 'resource mode, candidate is MEMORY,REMOTE_K8S',
  `endpoint` varchar(128) NOT NULL COMMENT 'endpoint',
  `status` varchar(128) NOT NULL COMMENT 'status, candidate is CREATING,RUNNING,DESTROYING,DESTROYED,ERROR_STATE,UNKNOWN',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_index` (`region`, `group_name`, `namespace`, name`)
)