--
-- Add resource_allocate_info table to maintain task resource allocate info
--
CREATE TABLE IF NOT EXISTS `task_resource_allocate_info` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `task_id` bigint(20) NOT NULL COMMENT 'task id allocate to this allocate info, equals to job_job.id',
    `resource_allocate_state` varchar(20) NOT NULL COMMENT 'resource allocate state, update by resource allocator, including PREPARING, AVAILABLE, FAILED, FINISHED',
    `resource_usage_state` varchar(20) NOT NULL COMMENT 'resource usage state update by resource user, including PREPARING, USING, FINISHED',
    `supervisor_endpoint` varchar(512) DEFAULT NULL COMMENT 'supervisor endpoint, in format host:port',
    `supervisor_endpoint_id` bigint(20) DEFAULT NULL  COMMENT 'supervisor endpoint id related to supervisor_endpoint.id',
    `resource_region` varchar(128) NOT NULL COMMENT 'resource region to filter endpoint',
    `resource_group` varchar(128) NOT NULL COMMENT 'resource group to filter endpoint',
    `resource_applier_name`  varchar(128) NOT NULL COMMENT 'resource applier name, eg PROCESS/K8S',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_resource_allocate_info_task_id` (`task_id`),
    INDEX `idx_task_resource_allocate_info_usage_allocate_state` (`resource_usage_state`, `resource_allocate_state`),
    INDEX `idx_task_resource_allocate_info_allocate_usage_state` (`resource_allocate_state`, `resource_usage_state`)
);