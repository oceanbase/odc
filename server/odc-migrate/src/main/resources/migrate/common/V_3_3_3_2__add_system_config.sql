INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.obclient.enabled', 'true', '是否支持命令行窗口，默认支持')
ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.personal-audit.enabled', 'true',
'是否支持查看个人操作审计，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.console-audit.enabled', 'true',
'是否支持在控制台查看操作审计，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.console-user.enabled', 'true',
'是否支持在控制台查看用户，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.console-role.enabled', 'true',
'是否支持在控制台查看角色，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.task.async.enabled', 'true',
'是否支持异步任务，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.task.import.enabled', 'true',
'是否支持导入任务，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.task.export.enabled', 'true',
'是否支持导出任务，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.task.mockdata.enabled', 'true',
'是否支持模拟数据任务，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.show-new-features.enabled', 'true',
'前端是否展示新功能介绍，默认展示') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.spm.enabled', 'false', '前端是否开启埋点，默认不开启') ON
DUPLICATE KEY UPDATE `id`=`id`;

