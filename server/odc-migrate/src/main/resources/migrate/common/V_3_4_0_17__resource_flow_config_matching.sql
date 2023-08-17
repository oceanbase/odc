---
--- v3.4.0
---

--
-- flow_config_node_approval add is_auto_approve column
--
ALTER TABLE `flow_config` ADD COLUMN `next_id` bigint(20) DEFAULT NULL
  COMMENT '下一个流程配置的 id，表示优先级，如果为 null 则代表最低优先级';

CREATE TABLE IF NOT EXISTS `flow_config_resource` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for relation',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
  `flow_config_id` bigint(20) NOT NULL COMMENT 'Associated flow config id',
  `resource_id` bigint(20) DEFAULT NULL COMMENT 'Associated resource config id',
  `resource_type` varchar(64) NOT NULL COMMENT 'Associated resource type',
  `organization_id` bigint(20) NOT NULL COMMENT 'Organization id',
  `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
  CONSTRAINT pk_flow_config_resource PRIMARY KEY (`id`),
  CONSTRAINT uk_flow_config_resource_rid_rtype_fconfig_id UNIQUE KEY (resource_id, resource_type, flow_config_id)
);