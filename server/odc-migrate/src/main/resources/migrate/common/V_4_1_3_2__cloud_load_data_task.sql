CREATE TABLE IF NOT EXISTS `cloud_task`
(
    `id`          	    	BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务 ID',
    `creator_id`		    BIGINT 			NOT NULL COMMENT '创建用户 ID',
    `organization_id`	    BIGINT 			NOT NULL COMMENT '组织 ID',
  	`instance_id`		    VARCHAR(64)     NOT NULL COMMENT '实例 ID',
  	`tenant_id`			    VARCHAR(64)		NOT NULL COMMENT '租户 ID',
    `db_username`		    VARCHAR(32)		NOT NULL COMMENT '数据库用户名',
    `status`			    VARCHAR(32)		NOT NULL COMMENT '任务状态, WAITING、RUNNING、CANCELED、FAILED、SUCCESSFUL',
    `progress_percentage`   DECIMAL(5, 2)	COMMENT '百分比',
    `environment`	        VARCHAR(32)		COMMENT '环境信息，e.g. OBCloud',
    `parameters_json`	    TEXT		    NOT NULL COMMENT '任务参数',
    `results_json`          TEXT            COMMENT '结果信息',
    `error_message`	        TEXT			COMMENT '错误信息',
    `executor_info`         VARCHAR(512)    COMMENT '任务发起机器信息',
    `type`                  VARCHAR(64)     COMMENT '任务类型',
    `duration`              VARCHAR(16)     COMMENT '任务耗时，格式为 HH:mm:ss',
    `create_time`		    DATETIME		NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`		    DATETIME		DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY (`instance_id`, `tenant_id`, `creator_id`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT = '多云任务模型';

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('cloud.load-data.max-file-size-from-object-storage-megabyte',
 '10240', '多云从对象存储导入数据时文件上限，单位为兆字节') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('cloud.load-data.cbs-key', 'changeme',
'多云导入任务依赖逻辑备份恢复服务所需要的配置') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('cloud.load-data.local-load-threads', 1,
'多云使用本地 ob-loader-dumper sdk 导入时使用的线程');
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('cloud.load-data.max-load-data-task-count', 100,
'多云导入任务最大并行数量') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('cloud.load-data.max-wait-task-count', 200,
'多云导入任务最大等待数量') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('cloud.load-data.max-load-buffer-size', 100000,
'多云导入任务 ob-loader 内存中最大数据条数，默认 100000') ON DUPLICATE KEY UPDATE `id`=`id`;