---
--- v3.4.0
---

--
-- flow_config_node_approval add is_auto_approve column
--
ALTER TABLE `flow_config_node_approval` ADD COLUMN `is_auto_approve` tinyint NOT NULL default 0
  COMMENT '是否自动审批，1 是，0 否';

--
-- flow_instance_node_approval add is_auto_approve column
--
ALTER TABLE `flow_instance_node_approval` ADD COLUMN `is_auto_approve` tinyint NOT NULL default 0
  COMMENT '是否自动审批，1 是，0 否';

--
-- flow_instance_node_task add execution_time column
--
ALTER TABLE `flow_instance_node_task` ADD COLUMN `execution_time` datetime DEFAULT NULL
  COMMENT '定时执行时间'
