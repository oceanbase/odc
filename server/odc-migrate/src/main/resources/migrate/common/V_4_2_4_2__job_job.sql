CREATE TABLE IF NOT EXISTS `job_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_class` varchar(512) NOT NULL COMMENT '任务执行的 Class 类名',
  `job_type` varchar(32) DEFAULT NULL COMMENT '任务类型，可选值有: ASYNC,IMPORT,EXPORT,MOCKDATA',
  `status` varchar(16) NOT NULL COMMENT '任务运行状态，可选值有：PREPARING,RUNNING,FAILED,CANCELED,DONE',
  `schedule_times` int  DEFAULT NULL COMMENT '已调度次数',
  `job_name` varchar(512) DEFAULT NULL COMMENT 'job name',
  `trigger_config_json` mediumtext DEFAULT NULL COMMENT '触发器配置参数json格式',
  `job_data_json` mediumtext DEFAULT NULL COMMENT '任务参数，不同任务由不同字段组成，为json格式',
  `result_json` mediumtext DEFAULT NULL COMMENT '任务执行结果',
  `progress_percentage` decimal(6,3) DEFAULT NULL COMMENT '任务完成百分比',
  `executor` varchar(128) DEFAULT NULL COMMENT '执行此任务的执行器信息',
  `creator_id` bigint DEFAULT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint  DEFAULT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  CONSTRAINT pk_job_schedule PRIMARY KEY (`id`)
) COMMENT = '任务记录表';