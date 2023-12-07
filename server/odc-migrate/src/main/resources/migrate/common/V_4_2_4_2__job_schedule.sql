CREATE TABLE IF NOT EXISTS `job_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `source_id` bigint DEFAULT NULL COMMENT '任务来源记录 ID, references task_task(id), schedule_task(id)',
  `source_type` varchar(32) NOT NULL COMMENT '任务分组，可选值有: TASK_TASK,SCHEDULE_TASK',
  `source_sub_type` varchar(512) DEFAULT NULL COMMENT '任务类型,可选值有:ASYNC,IMPORT,EXPORT,MOCKDATA,DATA_ARCHIVE',
  `status` varchar(16) DEFAULT NULL COMMENT '任务运行状态，可选值有：PREPARING,RUNNING,FAILED,CANCELED,DONE',
  `schedule_times` int  DEFAULT NULL COMMENT '已调度次数',
  `misfire_strategy` varchar(64) NOT NULL COMMENT '错误任务执行时间处理机制',
  `trigger_config_json` mediumtext DEFAULT NULL COMMENT '触发器配置参数 JSON 字符串',
  `job_data_json` mediumtext DEFAULT NULL COMMENT '任务参数，不同任务由不同字段组成，为json格式',
  `creator_id` bigint NOT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint NOT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  CONSTRAINT pk_job_schedule PRIMARY KEY (`id`),
  CONSTRAINT uk_job_schedule UNIQUE key (`source_id`,`source_type`)
) COMMENT = '任务调度记录表';