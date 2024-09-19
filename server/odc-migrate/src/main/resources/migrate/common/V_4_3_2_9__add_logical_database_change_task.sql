CREATE TABLE IF NOT EXISTS `logicaldatabase_database_change_execution_unit` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `execution_id` varchar(64) NOT NULL COMMENT 'execution id of the logical database change task, uuid',
  `execution_order` bigint(20) NOT NULL COMMENT 'execution order',
  `schedule_task_id` bigint(20) NOT NULL COMMENT 'ID of the related schedule task, refer to schedule_task.id',
  `logical_database_id` bigint(20) NOT NULL COMMENT 'logical database id, reference to connect_database.id',
  `physical_database_id` bigint(20) NOT NULL COMMENT 'physical database id, reference to connect_database.id',
  `sql_content` mediumtext COMMENT 'sql content',
  `execution_result_json` mediumtext NOT NULL COMMENT 'execution result json, see SqlExecutionResultWrapper',
  `status` varchar(32) NOT NULL COMMENT 'status of the execution, see ExecutionStatus',
  CONSTRAINT `pk_logical_db_change_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_logical_db_change_execution_id` UNIQUE KEY (`execution_id`),
  CONSTRAINT `uk_logical_db_change_sti_pdi_order` UNIQUE KEY (`schedule_task_id`, `physical_database_id`, `execution_order`)
) COMMENT = 'logical database change task execution units';

alter table schedule_schedule modify column connection_id bigint(20) COMMENT 'reference to connect_connection.id';