---
--- v 3.2.3
---
-- migrate from odc_configuration to config_system_configuration for specific items
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.arn-role', `value`, 'oss arn 角色名称' FROM odc_configuration WHERE `key`='OSS.roleArn') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.bucket-name', `value`, 'oss 所使用的 bucket 名称' FROM odc_configuration WHERE `key`='OSS.bucket') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.role-session-name', `value`, 'oss 在使用 sts 时的 session 角色名称' FROM odc_configuration WHERE `key`='OSS.roleSessionName') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.access-key-id', `value`, '接入 oss 时所使用的ak，该字段已加密' FROM odc_configuration WHERE `key`='OSS.accessKeyId') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.access-key-secret', `value`, '接入 oss 时所使用的sk，该字段已加密' FROM odc_configuration WHERE `key`='OSS.accessKeySecret') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.inner-environment', `value`, '布尔值，记录是否是处于内部环境，如果为真则使用内部 oss 高速链路' FROM odc_configuration WHERE `key`='OSS.inner') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.degrade-hours', `value`, 'oss 存储文件存储降级最大时间间隔' FROM odc_configuration WHERE `key`='OSS.degrade') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.delete-hours', `value`, 'oss 存储文件自动删除最大时间间隔' FROM odc_configuration WHERE `key`='OSS.delete') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.encrypt-storage-content', `value`, '是否对 oss 存储内容做服务端加密' FROM odc_configuration WHERE `key`='OSS.encrypt') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.oss.kms-name', `value`, 'oss 使用密钥托管服务的 kms 服务名' FROM odc_configuration WHERE `key`='OSS.KMS') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.monitor.access-key-id', `value`, 'odc monitor 应用阿里云监控的ak，该字段已加密' FROM odc_configuration WHERE `key`='MONITOR.accessKeyId') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.monitor.access-key-secret', `value`, 'odc monitor 应用阿里云监控的sk，该字段已加密' FROM odc_configuration WHERE `key`='MONITOR.accessKeySecret') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.monitor.event-code', `value`, 'odc monitor 应用阿里云监控的自定义事件 code' FROM odc_configuration WHERE `key`='MONITOR.eventcode') ON DUPLICATE KEY UPDATE `id`=`id`;
-- insert default value
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.region-id','CHANGE_ME', 'oss 地域 id') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.public-endpoint','CHANGE_ME', 'oss 公网服务地址') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.inner-endpoint','CHANGE_ME', 'oss 内网服务地址') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.global-speedup-endpoint','CHANGE_ME', 'oss 全球加速站点') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.overseas-speedup-endpoint','CHANGE_ME', 'oss 海外（除中国大陆）加速站点') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.arn-role', 'CHANGE_ME', 'oss arn 角色名称') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.bucket-name', 'CHANGE_ME', 'oss 所使用的 bucket 名称') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.role-session-name', 'CHANGE_ME', 'oss 在使用 sts 时的 session 角色名称') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.access-key-id', 'CHANGE_ME', '接入 oss 时所使用的ak，该字段已加密') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.access-key-secret', 'CHANGE_ME', '接入 oss 时所使用的sk，该字段已加密') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.inner-environment', 'false', '布尔值，记录是否是处于内部环境，如果为真则使用内部 oss 高速链路') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.degrade-hours', '12', 'oss 存储文件存储降级最大时间间隔') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.delete-hours', '24', 'oss 存储文件自动删除最大时间间隔') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.encrypt-storage-content', 'false', '是否对 oss 存储内容做服务端加密') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.kms-name', 'CHANGE_ME', 'oss 使用密钥托管服务的 kms 服务名') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.monitor.access-key-id', 'CHANGE_ME', 'odc monitor 应用阿里云监控的ak，该字段已加密') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.monitor.access-key-secret', 'CHANGE_ME', 'odc monitor 应用阿里云监控的sk，该字段已加密') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.monitor.event-code', 'CHANGE_ME', 'odc monitor 应用阿里云监控的自定义事件 code') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.download-url-expiration-interval-millis', '7200000', 'oss 下载链接过期时效，默认2小时') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oss.secret', 'CHANGE_ME', 'oss 的AK和SK加密密钥') ON DUPLICATE KEY UPDATE `id`=`id`;
