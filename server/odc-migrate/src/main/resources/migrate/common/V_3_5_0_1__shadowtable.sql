create table if not exists `shadowtable_table_comparing`(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `shadowtable_comparing_task_id` bigint NOT NULL COMMENT '影子表结构分析任务 ID, refer to shadowtable_table_comparing_task.id',
  `original_table_name` varchar(256) NOT NULL COMMENT '待同步的源表表名',
  `dest_table_name` varchar(256) NOT NULL COMMENT '待同步的目标表名',
  `comparing_result` varchar(32) NOT NULL COMMENT '结构分析结果，可选值：CREATE, UPDATE, NO_ACTION, WAITING, COMPARING, SKIP',
  `original_table_ddl` TEXT DEFAULT NULL COMMENT '源表 DDL',
  `dest_table_ddl` TEXT DEFAULT NULL COMMENT '目标表 DDL',
  `comparing_ddl` TEXT DEFAULT NULL COMMENT '源表到目标表的变更 DDL',
  `is_skipped` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否跳过该表，默认不跳过',
  CONSTRAINT `pk_shadowtable_table_comaring_id` PRIMARY KEY(`id`),
  CONSTRAINT uk_shadowtable_table_comaring_task_id_original_table_name UNIQUE KEY (`shadowtable_comparing_task_id`, `original_table_name`),
  KEY `idx_shadowtable_comparing_comparing_task_id_comparing_result`(`shadowtable_comparing_task_id`, `comparing_result`)
);

create table if not exists `shadowtable_table_comparing_task`(
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `last_modifier_id` bigint NOT NULL COMMENT '最后一次修改的用户 ID, references iam_user(id)',
  `flow_instance_id` bigint DEFAULT NULL COMMENT 'Process instance id, references flow_instance(id)',
  `connection_id` bigint NOT NULL COMMENT 'Connection ID, FK to connect_connection(id)',
  `schema_name` varchar(256) NOT NULL COMMENT '所属 schema',
  CONSTRAINT `pk_shadowtable_table_comparing_task_id` PRIMARY KEY(`id`),
  KEY `idx_shadowtable_table_comparing_task_flow_instance_id`(`flow_instance_id`),
  KEY `idx_shadowtable_table_comparing_task_flow_instance_id_creator_id`(`id`, `creator_id`)
);
