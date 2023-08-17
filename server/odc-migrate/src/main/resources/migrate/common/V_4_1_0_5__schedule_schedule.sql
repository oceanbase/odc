CREATE TABLE IF NOT EXISTS `schedule_schedule` (
`id` bigint(20) NOT NULL AUTO_INCREMENT,
`connection_id` bigint(20) NOT NULL COMMENT '连接 ID',
`organization_id` bigint(20) NOT NULL COMMENT '组织机构 ID',
`database_name` varchar(255) NOT NULL COMMENT '数据库名称',
`job_type` varchar(32) NOT NULL COMMENT '计划任务类型',
`allow_concurrent` boolean  NOT NULL COMMENT '是否允许并发执行',
`misfire_strategy` varchar(64) NOT NULL COMMENT '错误任务执行时间处理机制',
`status` varchar(32) NOT NULL COMMENT '计划状态',
`job_parameters_json` mediumtext DEFAULT NULL COMMENT '计划任务参数 JSON 字符串',
`trigger_config_json` mediumtext DEFAULT NULL COMMENT '触发器配置参数 JSON 字符串',
`description` mediumtext NULL COMMENT '计划描述',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON
UPDATE
CURRENT_TIMESTAMP COMMENT '更新时间',
`creator_id` bigint(20) NOT NULL COMMENT '创建用户 ID',
`modifier_id` bigint(20) NOT NULL COMMENT '更新用户 ID',
PRIMARY KEY (`id`)
);