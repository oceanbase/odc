CREATE TABLE IF NOT EXISTS `connection_partition_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `connection_id` bigint NOT NULL COMMENT '连接 ID',
  `organization_id` bigint NOT NULL COMMENT '组织 ID',
  `flow_instance_id` bigint NOT NULL COMMENT '任务 ID',
  `inspect_trigger_strategy` varchar(32) NOT NULL COMMENT '巡检周期枚举值：EVERY_DAY,FIRST_DAY_OF_MONTH,LAST_DAY_OF_MONTH,NONE',
  `is_inspect_enabled` boolean NOT NULL COMMENT '巡检功能是否开启',
  `is_config_enabled` boolean NOT NULL DEFAULT FALSE COMMENT '当前生效配置',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建者 ID',
  `modifier_id` bigint NOT NULL COMMENT '修改者 ID',
  CONSTRAINT pk_auto_partition_connection_configuration_id PRIMARY KEY (`id`),
  CONSTRAINT `uk_connection_id_flow_instance_id` UNIQUE (`connection_id`,`flow_instance_id`)
);

CREATE TABLE IF NOT EXISTS `table_partition_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `connection_id` bigint NOT NULL COMMENT '连接 ID',
  `organization_id` bigint NOT NULL COMMENT '组织 ID',
  `flow_instance_id` bigint NOT NULL COMMENT '任务 ID',
  `schema_name` varchar(128) NOT NULL COMMENT '库名',
  `table_name` varchar(128) NOT NULL COMMENT '表名',
  `is_config_enabled` boolean NOT NULL DEFAULT FALSE COMMENT '当前生效配置',
  `is_auto_partition` boolean NOT NULL COMMENT '自动分区是否开启',

  `partition_interval` bigint COMMENT '分区间隔',
  `partition_interval_unit` varchar(32) COMMENT '分区间隔单位枚举值：DAY,MONTH,YEAR',
  `pre_create_partition_count` bigint COMMENT '预创建分区数量',
  `expire_period` bigint COMMENT '分区失效周期',
  `expire_period_unit` varchar(32)  COMMENT '分区失效周期单位枚举值：DAY,MONTH,YEAR',
  `partition_naming_prefix` varchar(128) COMMENT '分区命名规则前缀',
  `partition_naming_suffix_expression` varchar(128)  COMMENT '分区命名规则后缀',

  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建者 ID',
  `modifier_id` bigint NOT NULL COMMENT '修改者 ID',
  CONSTRAINT pk_auto_partition_table_plan_id PRIMARY KEY (`id`),
  CONSTRAINT `uk_connectionId_flowInstanceId_schemaName_tableName` UNIQUE (`connection_id`,`flow_instance_id`,`schema_name`,`table_name`)
);

ALTER TABLE `flow_instance` ADD COLUMN `parent_instance_id` bigint DEFAULT NULL COMMENT '父流程实例 ID';
ALTER TABLE `flow_instance_node_approval` ADD COLUMN `wait_for_confirm` boolean COMMENT '审批节点-确认策略状态';