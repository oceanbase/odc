--
-- Add task_resource table
--
CREATE TABLE IF NOT EXISTS `task_resource` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
`update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
`region` varchar(256) NOT NULL COMMENT 'region name',
`group_name` varchar(256) NOT NULL COMMENT 'group name',
`namespace` varchar(256) NOT NULL COMMENT 'namespace name',
`name` varchar(1024) NOT NULL COMMENT 'resource name',
`resource_type` varchar(128) NOT NULL COMMENT 'resource type, eg memory, k8s',
`endpoint` varchar(128) NOT NULL COMMENT 'endpoint',
`status` varchar(128) NOT NULL COMMENT 'status, candidate is CREATING,RUNNING,DESTROYING,DESTROYED,ERROR_STATE,UNKNOWN',
`resource_properties` longtext COMMENT 'resource detailed properties',
PRIMARY KEY (`id`),
CONSTRAINT `resource_unique` UNIQUE (`resource_type`, `region`, `group_name`, `namespace`, `name`)
);