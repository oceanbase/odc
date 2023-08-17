INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.data.export.enabled', 'true', '是否开启数据导出，默认开启') ON DUPLICATE KEY UPDATE `id`=`id`;

insert into config_system_configuration(`key`, `value`, `description`) VALUES('support_kill_session', 'true', '是否支持 kill
session，默认支持') ON DUPLICATE KEY update `id`=`id`;

insert into config_system_configuration(`key`, `value`, `description`) VALUES('support_kill_query', 'true', '是否支持 kill
query，默认支持') ON DUPLICATE KEY update `id`=`id`;

insert into config_system_configuration(`key`, `value`, `description`) VALUES('support_pl_debug', 'true', '是否支持 pl debug，默认支持') ON
 DUPLICATE KEY update `id`=`id`;


INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.audit.enabled', 'false', '是否开启专有云操作审计, 默认不开启') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.audit.sls.endpoint', 'CHANGE_ME', '专有云操作审计SLS服务的endpoint') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.audit.sls.access-key', 'CHANGE_ME', '专有云操作审计SLS服务的accessKey') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.audit.sls.access-secret', 'CHANGE_ME', '专有云操作审计SLS服务的accessSecret') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.audit.sls.user-site-topic', 'CHANGE_ME', '专有云操作审计ASCM侧SLS topic') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.audit.sls.ops-site-topic', 'CHANGE_ME', '专有云操作审计ASO侧SLS topic') ON DUPLICATE KEY UPDATE `id`=`id`;

insert into config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.object-expiration-days', '14', '公有云 OSS
对象过期时间，默认 14 天') ON DUPLICATE KEY update `id`=`id`;