CREATE TABLE IF NOT EXISTS `job_job` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_class` varchar(256) NOT NULL COMMENT '任务执行的 Class 类名',
  `job_type` varchar(32) NOT NULL COMMENT '任务类型，可选值有: ASYNC,IMPORT,EXPORT,MOCKDATA',
  `status` varchar(16) NOT NULL COMMENT '任务运行状态，可选值有：PREPARING,RUNNING,RETRYING,FAILED,CANCELING,CANCELED,DONE',
  `execution_times` int NOT NULL COMMENT '已执行次数',
  `executor_identifier` varchar(256) DEFAULT NULL COMMENT 'executor identifier',
  `run_mode` varchar(16) DEFAULT NULL COMMENT 'task run mode, THREAD or K8S',
  `job_parameters_json` mediumtext DEFAULT NULL COMMENT '任务参数，不同任务由不同字段组成，为json格式',
  `result_json` mediumtext DEFAULT NULL COMMENT '任务执行结果',
  `progress_percentage` decimal(6,3) DEFAULT NULL COMMENT '任务完成百分比',
  `executor_endpoint` varchar(128) DEFAULT NULL COMMENT '执行此任务的执行器信息',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述信息',
  `creator_id` bigint DEFAULT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint  DEFAULT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  CONSTRAINT pk_job_schedule PRIMARY KEY (`id`)
) COMMENT = '任务表';


CREATE TABLE IF NOT EXISTS `job_attribute` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` int NOT NULL COMMENT 'job id',
  `attribute_key` varchar(128) DEFAULT NULL COMMENT 'attribute key',
  `attribute_value` varchar(512) DEFAULT NULL COMMENT 'attribute value',
  `creator_id` bigint DEFAULT NULL COMMENT '创建用户 ID, references iam_user(id)',
  `organization_id` bigint  DEFAULT NULL COMMENT '所属组织 ID, references iam_organization(id)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  CONSTRAINT pk_job_schedule PRIMARY KEY (`id`),
  index `idx_job_attribute_job_id` (`job_id`),
  CONSTRAINT UNIQUE `uk_job_attribute_job_id_attribute_key` (`job_id`, `attribute_key`)
) COMMENT = '任务属性表';
