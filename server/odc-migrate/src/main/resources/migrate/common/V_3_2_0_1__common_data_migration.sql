---
--- v3.2.0
---

-- below system organization
-- migrate odc_configuration to config_system_configuration
-- 1. here do not select from odc_organization into config_system_configuration straightly, due:
--   -  all apsara related configuration will be rewrite while deploy in private aliyun environment, same as new configurations
--   - migration required for some specific items only
-- 2. all other new items does not require migration maintained in `resources/data.sql`, which will be load by spring cloud config while bootstrap

-- migrate from odc_configuration to config_system_configuration for specific items
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.cloud.ocp.url', `value`, 'cloud ocp url' FROM
odc_configuration WHERE `key`='domain.ocp.url') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.cloud.ocp.vpc.deploy.id', `value`, 'vpc deploy id' FROM
odc_configuration WHERE `key`='deploy.vpc.id') ON DUPLICATE KEY UPDATE `id`=`id`;

-- private aliyun related
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.oauth-url', 'CHANGE_ME', '阿里云专有云AAS OAuth 认证服务的URL地址') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.oauth-key', 'CHANGE_ME', '阿里云专有云AAS OAuth服务的access key') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.oauth-secret', 'CHANGE_ME', '阿里云专有云AAS OAuth服务的access secret') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.ops-site-domain', 'CHANGE_ME', '阿里云专有云运维侧域名') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.ops-site-login-url', 'CHANGE_ME', '阿里云专有云运维侧登录URL') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.ops-site-logout-url', 'CHANGE_ME', '阿里云专有云运维侧登出URL') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.user-site-domain', 'CHANGE_ME', '阿里云专有云用户侧域名') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.user-site-login-url', 'CHANGE_ME', '阿里云专有云用户侧登录URL') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.aas.user-site-logout-url', 'CHANGE_ME', '阿里云专有云用户侧登出URL') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.oam.endpoint', 'CHANGE_ME', '阿里云专有云OAM服务的endpoint') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.oam.region-name', 'CHANGE_ME', '阿里云专有云OAM服务所在region名字') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.oam.domain', 'CHANGE_ME', '阿里云专有云OAM服务的域名') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.oam.access-key', 'CHANGE_ME', '阿里云专有云OAM服务的access-key') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.oam.access-secret', 'CHANGE_ME', '阿里云专有云OAM服务的access-secret') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.ram.endpoint', 'CHANGE_ME', '阿里云专有云RAM服务的endpiont') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('apsara.ram.region-name', 'CHANGE_ME', '阿里云专有云RAM服务所在region名字') ON DUPLICATE KEY UPDATE `id`=`id`;

-- public aliyun related
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.ocp.url', 'CHANGE_ME', 'cloud ocp url') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.ocp.vpc.deploy.id', 'CHANGE_ME', 'vpc deploy id') ON DUPLICATE KEY UPDATE `id`=`id`;
