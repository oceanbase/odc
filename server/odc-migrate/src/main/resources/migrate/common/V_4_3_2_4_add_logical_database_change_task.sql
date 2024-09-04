CREATE TABLE IF NOT EXISTS logicaldatabase_database_change_execution_unit (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `execution_id` varchar(64) NOT NULL COMMENT 'execution id of the logical database change task, uuid',
  `schedule_task_id` bigint(20) NOT NULL COMMENT 'ID of the related schedule task, refer to schedule_task.id',
  `logical_database_id` bigint(20) NOT NULL COMMENT 'logical database id, reference to connect_database.id',
  `physical_database_id` bigint(20) NOT NULL COMMENT 'physical database id, reference to connect_database.id',
  `sql_content` text COMMENT 'sql content',
  `execution_result_json` text NOT NULL COMMENT 'execution result json, see SqlExecutionResultWrapper',
  CONSTRAINT `pk_logical_db_change_exec_id` PRIMARY KEY (`id`),
  CONSTRAINT `uk_database_schema_column_database_id_object_id_name` UNIQUE KEY (`execution_id`)
) COMMENT = 'logical database change task execution units';