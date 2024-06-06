---
--- v3.3.0
---
create table if not exists `flow_instance` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a process instance',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `name` varchar(1024) NOT NULL COMMENT 'Name for this flow instance',
    `flow_config_id` bigint(20) not null comment 'Process config id, references flow_config(id)',
    `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
    `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
    `process_definition_id` varchar(128) DEFAULT NULL COMMENT 'External framework auxiliary field, flowable process definition id',
    `process_instance_id`varchar(128) DEFAULT NULL COMMENT 'External framework auxiliary field, flowable process instance id',
    `status` varchar(32) NOT NULL COMMENT 'Status for a instance, optional values: CREATED,EXECUTING,KILLED,COMPLETED,EXPIRED,FAILED',
    `flow_config_snapshot_xml` mediumtext DEFAULT NULL COMMENT 'Process snapshots to record instant information about the process',
    `description` varchar(1024) DEFAULT NULL COMMENT 'Description of a flow instance',
    constraint pk_flow_instance primary key (`id`)
);

create table if not exists `flow_instance_node_approval` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT comment 'Id for a usertask instance',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
    `user_task_id` varchar(128) DEFAULT NULL COMMENT 'External framework auxiliary field, flowable user task id',
    `status` varchar(32) NOT NULL COMMENT 'Status for a instance, optional values: CREATED,EXECUTING,KILLED,COMPLETED,EXPIRED,FAILED',
    `operator_id` bigint(20) DEFAULT NULL COMMENT 'Operator, references iam_user(id)',
    `comment` varchar(1024) DEFAULT NULL COMMENT 'Handling comments',
    `approval_expire_interval_seconds` int(11) NOT NULL COMMENT 'Approval node timeout time, if it is less than or equal to 0, it means no timeout',
    `is_approved` tinyint NOT NULL COMMENT 'Is it approved',
    `is_start_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the starting endpoint of the process',
    `is_end_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the end endpoint of the process',
    `flow_instance_id` bigint(20) NOT NULL comment 'Process instance id, references flow_instance(id)',
    constraint pk_flow_instance_node_approval primary key (`id`)
);

create table if not exists `flow_instance_node_approval_candidate` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a relation',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `approval_instance_id` bigint(20) NOT NULL COMMENT 'Refer to flow_instance_node_approval(id)',
    `user_id` bigint(20) DEFAULT NULL COMMENT 'The list of candidate operators, which can be specific user, refer to iam_user(id)',
    `role_id` bigint(20) DEFAULT NULL COMMENT 'The list of candidate operators, which can be specific roles, refer to iam_role(id)',
    constraint pk_flow_instance_node_approval_candidate primary key (`id`),
    constraint uk_flow_instance_node_approval_candidate unique key (`approval_instance_id`, `user_id`, `role_id`)
);

create table if not exists `flow_config_node_approval`(
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Id for a usertask config',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
    `creator_id` bigint(20) NOT NULL COMMENT 'Creator id, references iam_user(id)',
    `approval_expire_interval_seconds` int(11) DEFAULT 0 COMMENT 'Approval node timeout time, if it is less than or equal to 0, it means no timeout',
    `is_start_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the starting endpoint of the process',
    `is_end_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the end endpoint of the process',
    `flow_config_id` bigint(20) NOT NULL COMMENT 'Process instance id, references flow_config(id)',
    constraint pk_flow_config_node_approval PRIMARY KEY (`id`)
);

create table if not exists `flow_config_node_approval_candidate` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a relation',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `approval_config_id` bigint(20) NOT NULL COMMENT 'Refer to flow_config_node_approval(id)',
    `user_id` bigint(20) DEFAULT NULL COMMENT 'The list of candidate operators, which can be specific user, refer to iam_user(id)',
    `role_id` bigint(20) DEFAULT NULL COMMENT 'The list of candidate operators, which can be specific roles, refer to iam_role(id)',
    constraint pk_flow_config_node_approval_candidate primary key (`id`),
    constraint uk_flow_config_node_approval_candidate unique key (`approval_config_id`, `user_id`, `role_id`)
);

create table if not exists `flow_instance_node_gateway` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a gateway instance',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `organization_id` bigint(20) NOT NULL COMMENT 'Organization id, references iam_organization(id)',
    `status` varchar(32) NOT NULL COMMENT 'Status for a instance, optional values: CREATED,EXECUTING,KILLED,COMPLETED,EXPIRED,FAILED',
    `is_start_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the starting endpoint of the process',
    `is_end_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the end endpoint of the process',
    `flow_instance_id` bigint(20) NOT NULL COMMENT 'Process instance id, references flow_instance(id)',
    constraint pk_flow_instance_node_gateway primary key (`id`)
);

create table if not exists `flow_instance_node` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a node instance',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `instance_id` bigint(20) NOT NULL COMMENT 'Instance id, references flow_instance_xxx(id)',
    `instance_type` varchar(64) NOT NULL COMMENT 'Node type, optional values: APPROVAL_TASK,GATEWAY,SERVICE_TASK',
    `activity_id` varchar(64) NOT NULL COMMENT 'Related to flowable integration, used for topology mapping',
    `name` varchar(64) NOT NULL COMMENT 'Related to flowable integration, used for topology mapping',
    `flowable_element_type` varchar(32) DEFAULT NULL COMMENT 'The flowable type corresponding to activityid and name, optional values: USER_TASK,SERVICE_TASK,EXCLUSIVE_GATEWAY,SIGNAL_EVENT,TIMER_EVENT,TIMER_BOUNDARY_EVENT,ERROR_BOUNDARY_EVENT',
    `flow_instance_id` bigint(20) NOT NULL COMMENT 'Process instance id, references flow_instance(id)',
    constraint pk_flow_instance_node primary key(`id`),
    constraint uk_flow_instance_node_name_flowable_element_type unique key (`name`, `flowable_element_type`),
    constraint uk_flow_instance_node_activity_id_flowable_element_type unique key (`activity_id`, `flowable_element_type`),
    constraint uk_flow_instance_node_activity_id_flow_instance_id unique key (`flow_instance_id`, `activity_id`),
    constraint uk_flow_instance_node_name_flow_instance_id unique key (`flow_instance_id`, `name`)
);

create table if not exists `flow_instance_sequence` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a sequence instance',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `source_node_instance_id` bigint(20) NOT NULL COMMENT 'Configure the id on the row dimension, references flow_instance_node(id)',
    `target_node_instance_id` bigint(20) NOT NULL COMMENT 'Configure the id on the col dimension, references flow_instance_node(id)',
    `flow_instance_id` bigint(20) NOT NULL COMMENT 'Flow config id, references flow_instance(id)',
    constraint pk_flow_instance_sequence primary key (`id`),
    constraint uk_flow_instance_sequence_source_target_instance_id unique key(`source_node_instance_id`, `target_node_instance_id`)
);

create table if not exists `flow_config_node` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a node config',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `config_id` bigint(20) NOT NULL COMMENT 'Config id, references flow_config_xxx(id)',
    `config_type` varchar(64) NOT NULL COMMENT 'Node type, optional values: APPROVAL_TASK,GATEWAY,SERVICE_TASK',
    `flow_config_id` bigint(20) NOT NULL COMMENT 'Process instance id, references flow_config(id)',
    constraint pk_flow_config_node primary key(`id`),
    constraint uk_flow_config_node_config_type_config_id unique key (`config_type`, `config_id`)
);

create table if not exists `flow_config_sequence` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Id for a sequence config',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record insertion time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Record modification time',
    `source_node_config_id` bigint(20) NOT NULL COMMENT 'Configure the id on the row dimension, references flow_config_node(id)',
    `target_node_config_id` bigint(20) NOT NULL COMMENT 'Configure the id on the col dimension, references flow_config_node(id)',
    `condition_expression` varchar(32) DEFAULT NULL COMMENT 'Flow condition expression, eg. ${level == 1}',
    `risk_level` int DEFAULT 1 COMMENT 'Record current sequence belongs to which risk level',
    `risk_level_config_id` bigint(20) DEFAULT NULL COMMENT 'References flow_risk_level_config(id)',
    `flow_config_id` bigint(20) NOT NULL COMMENT 'Flow config id, references flow_config(id)',
    constraint pk_flow_config_sequence primary key (`id`),
    constraint uk_flow_config_sequence_source_target_config_id unique key(`source_node_config_id`, `target_node_config_id`)
);

CREATE TABLE IF NOT EXISTS `task_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型，可选值有: ASYNC,IMPORT,EXPORT,MOCKDATA',
  `connection_id` bigint DEFAULT NULL COMMENT '所属连接 ID, references connect_connection(id)',
  `database_name` varchar(512) DEFAULT NULL COMMENT '所属库',
  `description` varchar(1024) DEFAULT NULL,
  `parameters_json` mediumtext DEFAULT NULL COMMENT '任务参数，不同任务由不同字段组成，为json格式',
  `execution_expire_interval_seconds` int NOT NULL COMMENT '执行有效期',
  `executor` varchar(128) NOT NULL COMMENT '执行此任务的ODC Server信息',
  `status` varchar(16) DEFAULT NULL COMMENT '任务运行状态，可选值有：PREPARING,RUNNING,FAILED,CANCELED,DONE',
  `progress_percentage` decimal(6,3) DEFAULT NULL COMMENT '任务完成百分比',
  `result_json` mediumtext DEFAULT NULL COMMENT '任务结果, json格式',
  CONSTRAINT pk_task_instance_id PRIMARY KEY (`id`)
) COMMENT = '任务实例表';

CREATE TABLE IF NOT EXISTS `flow_instance_node_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `status` varchar(32) NOT NULL COMMENT 'Status for a instance, optional values: CREATED,EXECUTING,KILLED,COMPLETED,EXPIRED,FAILED',
  `flow_instance_id` bigint NOT NULL COMMENT '关联流程实例 ID, references flow_instance(id)',
  `task_task_id` bigint DEFAULT NULL COMMENT '关联任务实例 ID, references task_task(id)',
  `task_type` varchar(32) NOT NULL COMMENT 'The task type that marks the task instance node, optional values: ASYNC,IMPORT,EXPORT,MOCKDATA',
  `wait_execution_expire_interval_seconds` int NOT NULL COMMENT 'Execute waiting for expiration',
  `task_execution_strategy` varchar(16) NOT NULL COMMENT 'Task execution execution mode, optional values: AUTO, MANUAL, TIMER',
  `is_start_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the starting endpoint of the process',
  `is_end_endpoint` tinyint NOT NULL COMMENT 'Describes whether this endpoint is the end endpoint of the process',
  CONSTRAINT pk_flow_instance_node_task_id PRIMARY KEY (`id`)
) COMMENT = '任务实例表';

CREATE TABLE IF NOT EXISTS `flow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `user_update_time` datetime DEFAULT NULL COMMENT '用户修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `name` varchar(512) NOT NULL COMMENT '变更流程名称',
  `general_type` varchar(64) NOT NULL COMMENT '变更流程父类型，可选值有: ASYNC,IMPORT,EXPORT,MOCKDATA',
  `sub_type` varchar(256) DEFAULT NULL COMMENT '变更流程子类型，可选值有: insert,update,select,delete,create,drop,alter,other',
  `is_enabled` tinyint(1) NOT NULL COMMENT '是否启用',
  `is_builtin` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否为内置流程配置',
  `approval_expire_interval_seconds` int NOT NULL COMMENT '审批有效期',
  `wait_execution_expire_interval_seconds` int NOT NULL COMMENT '等待执行有效期',
  `execution_expire_interval_seconds` int NOT NULL COMMENT '执行有效期',
  `description` varchar(1024) DEFAULT NULL COMMENT '备注',
  CONSTRAINT pk_flow_config_id PRIMARY KEY (`id`),
  CONSTRAINT uk_flow_config_organization_id_name UNIQUE KEY (`organization_id`, `name`)
) COMMENT = '变更流程配置表';

CREATE TABLE IF NOT EXISTS `flow_config_node_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `flow_config_id` bigint NOT NULL COMMENT '关联流程配置 ID, references flow_config(id)',
  `task_type` varchar(32) NOT NULL COMMENT '任务类型，可选值有: ASYNC,IMPORT,EXPORT,MOCKDATA',
  `is_start_endpoint` tinyint(1) NOT NULL COMMENT '描述此节点是否为流程开始节点',
  `is_end_endpoint` tinyint(1) NOT NULL COMMENT '描述此节点是否为流程结束节点',
  CONSTRAINT pk_flow_config_node_task_id PRIMARY KEY (`id`)
) COMMENT = '流程与任务配置映射表';

CREATE TABLE IF NOT EXISTS `flow_config_node_gateway` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `flow_config_id` bigint NOT NULL COMMENT '关联流程配置 ID, references flow_config(id)',
  `is_start_endpoint` tinyint(1) NOT NULL COMMENT '描述此节点是否为流程开始节点',
  `is_end_endpoint` tinyint(1) NOT NULL COMMENT '描述此节点是否为流程结束节点',
  CONSTRAINT pk_flow_config_node_gateway_id PRIMARY KEY (`id`)
) COMMENT = '网关节点配置表';

CREATE TABLE IF NOT EXISTS `flow_config_risk_level` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `flow_config_id` bigint NOT NULL COMMENT '关联流程配置 ID, references flow_config(id)',
  `sub_type` varchar(256) DEFAULT NULL COMMENT '风险等级对应变更子类型，可选值有: insert,update,select,delete,create,drop,alter,other',
  `is_contains_risk_data` tinyint(1) NOT NULL COMMENT '是否包含风险数据',
  `min_affected_rows` int DEFAULT NULL COMMENT '变更影响数量最小值',
  `max_affected_rows` int DEFAULT NULL COMMENT '变更影响数量最大值',
  CONSTRAINT pk_flow_config_risk_level_id PRIMARY KEY (`id`)
) COMMENT = '风险等级配置表';

CREATE TABLE IF NOT EXISTS `flow_config_sub_task_type` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `flow_config_id` bigint NOT NULL COMMENT '关联流程配置 ID, references flow_config(id)',
  `risk_level_id` bigint DEFAULT NULL COMMENT '关联流程风险配置 ID, references flow_config_risk_level(id)',
  `sub_type` varchar(32) NOT NULL COMMENT '风险等级对应变更子类型，可选值有: insert,update,select,delete,create,drop,alter,other',
  CONSTRAINT pk_flow_config_sub_task_type_id PRIMARY KEY (`id`),
  CONSTRAINT uk_flow_config_sub_task_type UNIQUE KEY (`organization_id`, `flow_config_id`, `risk_level_id`, `sub_type`)
) COMMENT = '流程配置和任务子类型关联表';

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.task.default-execution-expiration-interval-hours', '48', '流程任务执行默认超时时间，单位为小时，默认为48小时') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.task.default-wait-execution-expiration-interval-hours', '48', '流程任务手动执行/触发回滚默认超时时间，单位为小时，默认为48小时') ON DUPLICATE KEY UPDATE `id`=`id`;