INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.multiple-async.enabled', 'true', '多库变更功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.shadowtable-sync.enabled', 'true', '影子表同步功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.structure-comparison.enabled', 'true', '结构比对功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.sql-plan.enabled', 'true', 'SQL 计划功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.partition-plan.enabled', 'true', '分区计划功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.data-archive.enabled', 'true', '数据归档功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.data-delete.enabled', 'true', '数据清理功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.apply-database-permission.enabled', 'true', '申请库权限功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.apply-project-permission.enabled', 'true', '申请项目权限功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`)
VALUES('odc.features.task.apply-table-permission.enabled', 'true', '申请表/视图权限功能入口开关') ON DUPLICATE KEY UPDATE `id`=`id`;