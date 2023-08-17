--
-- flow_config_node_approval_candidate add external_approval_id column
--
ALTER TABLE
  `flow_config_node_approval_candidate`
ADD
  COLUMN `external_approval_id` bigint(20) DEFAULT NULL COMMENT 'External approval integration ID';

--
-- flow_instance_node_approval add external_approval_id column
--
ALTER TABLE
  `flow_instance_node_approval`
ADD
  COLUMN `external_approval_id` bigint(20) DEFAULT NULL COMMENT 'External approval integration ID';
