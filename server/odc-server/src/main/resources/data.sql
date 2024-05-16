--
-- data.sql
-- for initialize all required system configuration for config framework,
-- ODC use spring cloud config as config framework
-- @since v3.2.0
--

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('sqlexecute.defaultDelimiter', ';', 'odc', 'default', 'master', 'Delimiter for sql-execute') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('sqlexecute.mysqlAutoCommitMode', 'ON', 'odc', 'default', 'master', 'Auto commit flag for OB-Mysql mode') ON DUPLICATE KEY UPDATE
 `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('sqlexecute.oracleAutoCommitMode', 'ON', 'odc', 'default', 'master', 'Auto commit flag for OB-Oracle mode') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('sqlexecute.defaultQueryLimit', '1000', 'odc', 'default', 'master', 'Query limit for sql-execute') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.chinamobile.4a.httpReadTimeoutSeconds', '10', 'odc', 'default', 'master', '4A API http read timeout') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.chinamobile.4a.httpConnectTimeoutSeconds', '2', 'odc', 'default', 'master', '4A API http connect timeout') ON DUPLICATE KEY
UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.config.userConfig.cacheRefreshTimeSeconds', '60', 'odc', 'default', 'master', '个人配置缓存刷新时间') ON DUPLICATE KEY
UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.task.file.expireTimeHours', '72', 'odc', 'default', 'master', '任务数据文件过期时间') ON DUPLICATE KEY
UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.task.mock-data.max-row-count',
 '1000000', '模拟数据单表生成记录条数最大值') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.task.async.sql-content-max-length',
 '10485760', '数据库变更任务 SQL 内容允许最大长度，单位：字节。取值不超过 16 MB，默认为 10 MB') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.common.task.expiredTimeInMinutes', '1440', '通用任务过期时间') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.common.intercept.task.timeoutInSeconds', '3600000', '通用任务执行超时时间') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.system.config.heartbeat.enabled', 'true', '心跳检测开关') ON
 DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.buc.enabled',
 'false', '是否开启 BUC 认证') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.buc.supportGroupQRCodeUrl',
 'TO_BE_REPLACED', '用户支持群地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.client-id',
 'TO_BE_REPLACED', 'BUC 应用 client-id') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.client-secret',
 'TO_BE_REPLACED', 'BUC 应用 client-secret') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.redirect-uri',
 'TO_BE_REPLACED', 'BUC grant code 回调地址，由 spring 框架自动解析，请勿随意更改') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.authorization-grant-type',
 'TO_BE_REPLACED', 'OAuth2 授权模式，当前实现为授权码模式') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.scope',
 'TO_BE_REPLACED', '应用授权作用域') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.buc.authorization-uri',
 'TO_BE_REPLACED', 'BUC 授权服务器提供的获取 grant-code 的地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.buc.token-uri',
 'TO_BE_REPLACED', 'BUC 授权服务器提供的获取 access-token 的地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.buc.user-info-uri',
 'TO_BE_REPLACED', 'BUC 授权服务器提供的获取 user-info 的地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.buc.adminEmpIds',
 'TO_BE_REPLACED', 'BUC 默认管理员工号') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.buc.logoutUrl',
 'TO_BE_REPLACED', '集团登录地址，用于 ODC 退出登录后前端跳转') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.buc.loginRedirectUrl',
 'TO_BE_REPLACED', '登录失效后再次访问页面时的跳转 url，用于重新登录 ODC') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.provider',
 'buc', 'BUC 授权服务提供方配置，兼容配置，请勿修改') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.buc.clientAuthenticationMethod',
 'post', 'BUC 授权服务认证请求方式，支持[post|none|basic]，默认为post') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.buc.userInfoAuthenticationMethod',
 'form', 'BUC 授权服务器提供的获取 user-info 请求方式，支持[header|query|form]，BUC默认为form') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.provider',
 'cas', 'OAuth2 授权服务提供方配置，默认为cas，在自行配置授权服务提供方时修改。') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.client-id',
 'TO_BE_REPLACED', 'OAuth2 应用 client-id') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.client-secret',
 'TO_BE_REPLACED', 'OAuth2 应用 client-secret') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.redirect-uri',
 'TO_BE_REPLACED', 'OAuth2 grant code 回调地址，由 spring 框架自动解析，请勿随意更改') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.authorization-grant-type',
 'TO_BE_REPLACED', 'OAuth2 授权模式，当前实现为授权码模式') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.scope',
 'TO_BE_REPLACED', 'OAuth2 应用授权作用域') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.registration.odc.clientAuthenticationMethod',
 'post', 'OAuth2 授权服务认证请求方式，支持[post|none|basic]，默认为post') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.cas.authorization-uri',
 'TO_BE_REPLACED', 'OAuth2 授权服务器提供的获取 grant-code 的地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.cas.token-uri',
 'TO_BE_REPLACED', 'OAuth2 授权服务器提供的获取 access-token 的地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.cas.user-info-uri',
 'TO_BE_REPLACED', 'OAuth2 授权服务器提供的获取 user-info 的地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('spring.security.oauth2.client.provider.cas.userInfoAuthenticationMethod',
 'TO_BE_REPLACED', 'OAuth2 授权服务器提供的获取 user-info 请求方式，支持[header|query|form]') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.userProfileViewType',
 'FLAT', 'OAuth2 授权服务器提供的获取 userProfile的数据结构，支持[NESTED,FLAT]') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.nestedAttributeField',
'attributes', 'OAuth2 授权服务器提供的获取 userProfile的数据结构 为NESTED 模式的时候， 获取userinfo的字段') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.adminAccountNames',
 'admin1,admin2', 'OAuth2 默认管理员账户，[userAccountNameField]配置字段的取值集合。') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.logoutUrl',
 'TO_BE_REPLACED', '集团登录地址，用于 ODC 退出登录后前端跳转') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.loginRedirectUrl',
 'TO_BE_REPLACED', '登录失效后再次访问页面时的跳转 url，用于重新登录 ODC') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.redirectUrlHashEnabled',
'true', '是否开启重定向支持hash#符号，如果不支持，oauth2重定向的时候会截断url#号的内容，默认支持') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.userAccountNameField',
 'TO_BE_REPLACED', '授权中心返回用户信息字段【用户账户名】') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.userNickNameField',
 'nickname,name,account', '授权中心返回用户信息字段【用户名称】，允许配置多个字段，优先级按顺序由高到低') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.organizationNameField',
 'TO_BE_REPLACED', '授权中心返回用户信息字段【组织机构名称】，如果odc.oauth2.organizationName非空，则优先使用组织名称配置项，否则根据当前配置项从OAuth2账号信息中提取') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.oauth2.organizationName',
'TO_BE_REPLACED', '指定用户组织机构名称，如果非空，组织名称引用当前配置项的值，否则组织名称基于 odc.oauth2.organizationNameField 从 OAuth2 账号信息中提取') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.pldebug.thread-pool.size',
  '0', 'PL 调试功能分配的 ODC 线程数，0 表示使用系统推荐值') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.pldebug.session.timeout-seconds',
  '600', 'PL 调试功能超时秒数') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.pldebug.sync.enabled',
  'true', 'PL 调试功能是否在开启调试阶段使用sync机制') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.resultset.export.thread-count',
 '10', '结果集导出线程池大小') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.system.security.sensitive-property-encrypted',
 'true', '是否启用敏感数据传输加密，默认为 true，设置为 false 时则使用明文传输') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.obsdk.oracle.pl-parser-type',
 'oracle', 'ODC 使用的 PL Parser 类型，可选值为 ob_oracle / oracle，默认值 oracle') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.type', 'local',
'登录鉴权模式') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.auto-login.enabled', 'false',
'是否开启前端自动登录') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.enabled', 'false',
'是否是开源实验室模式') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.tutorial.enabled', 'false',
'体验站场景下，是否开启 Tutorial 功能') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.session-limit.enabled', 'false',
'体验站场景下，是否开启用户创建 Session 限流') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.apply-permission.hidden', 'false',
'是否隐藏权限申请功能') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.admin-id',
'REPLACE_ME', '开源实验室默认管理员账号') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.ob.connection.key', 'REPLACE_ME',
'官网体验站 OB 体验连接列表，json 格式') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.app-id', 'REPLACE_ME',
'开源实验室在支付宝开放开发平台上注册的应用 id') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.server-url', 'REPLACE_ME',
'支付宝开放开发平台 OpenAPI 网关地址') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.private-key', 'REPLACE_ME',
'开源实验室在支付宝开放开发平台上注册的应用私钥') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.alipay-public-key',
'REPLACE_ME', '开源实验室在支付宝开放开发平台上注册的应用公钥') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.sign-type',
'REPLACE_ME', '开源实验室调用 OpenAPI 接口的加签方式，如 RSA2') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.format',
'REPLACE_ME', '开源实验室调用 OpenAPI 接口的字段格式，如 JSON') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.charset', 'REPLACE_ME',
'开源实验室调用 OpenAPI 接口的字符集，如 utf-8, GBK') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.ob-official-domain', 'REPLACE_ME',
'开源官网域名') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.ob-official-login-url',
'REPLACE_ME', '开源官网登录 URL') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.alipay.openapi.ob-official-logout-url',
'REPLACE_ME', '开源官网登出 URL') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.resource.revoke-interval-seconds',
'28800', 'lab 资源回收时间，默认8小时') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.system.info.support-email',
'', '用户支持反馈邮箱，为空表示使用默认值，设置后会覆盖默认值') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.system.info.support-url',
'', '用户支持 URL 地址，为空表示使用默认值，设置后会覆盖默认值') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.system.info.home-page-text',
'', '主页文案，为空表示使用默认值，设置后会覆盖默认值') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.connect.session.history-update-interval-millis',
'600000', '连接 session 历史刷新时间间隔') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.schedule.fix-delay-millis',
'7200000', 'lab 模块定时任务间隔时间') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.schedule.user-expired-time-millis',
'180000', 'lab 模块清理不活跃用户创建session权限时间') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.schedule.update-session-permission-delay-millis',
'1000', 'lab 查询空闲资源定时任务周期') ON DUPLICATE KEY UPDATE `id`=`id`;


-- rate limit related
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.enabled',
  'false', '限流配置，是否开启限流，默认为 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.url-white-list',
  '/api/v1/info,/sqls/getResult', '限流配置，限流 API 白名单, 默认为 /api/v1/info,/sqls/getResult') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.api.capacity',
  '20', '限流配置，API 调用，令牌桶的容量，默认为 20') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.api.refill-tokens',
  '10', '限流配置，API 调用，令牌每次填充数量，默认为 10') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.api.refill-duration-seconds',
  '5', '限流配置，API 调用，令牌填充间隔，单位 秒，默认为 5') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.sql.capacity',
  '10000', '限流配置，SQL 执行数量，令牌桶的容量，默认为 10000') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.sql.refill-tokens',
  '1000', '限流配置，SQL 执行数量，令牌每次填充数量，默认为 1000') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.rate-limit.sql.refill-duration-seconds',
  '10', '限流配置，SQL 执行数量，令牌填充间隔，单位 秒，默认为 10') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.static-resource.cache-timeout-seconds',
  '60', '静态资源缓存时间，单位 秒，默认为 60') ON DUPLICATE KEY UPDATE `id`=`id`;


INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.max-response-size',
  '1048576', '公有云模式下接口最大下行数据量，单位为字节，默认大小 1024KB，超过此限制则通过OSS文件进行返回值中转') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.audit.enabled',
  'true', '是否开启操作审计，默认打开') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('sqlexecute.defaultObjectDraggingOption', 'object_name', 'odc', 'default', 'master', 'Default object dragging option can
only accept the value object_name|select_stmt|insert_stmt|update_stmt|delete_stmt') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('connect.sessionMode', 'MultiSession', 'odc', 'default', 'master', 'Default session mode can only accept the
value SingleSession|MultiSession') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.objectstorage.upload-timeout-seconds', '60', 'odc', 'default', 'master', '单个文件分块上传超时时间，单位为秒，默认 60 秒') ON DUPLICATE KEY UPDATE
`id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.objectstorage.local.dir', '#{systemProperties[''user.home''].concat(T(java.io.File).separator).concat(''data'').concat(T(java
.io.File).separator).concat(''files'')}', 'odc', 'default', 'master','本地文件存储根目录') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.objectstorage.max-concurrent-count', '16', 'odc', 'default', 'master', '文件存储模块最大并发线程数，默认 16') ON DUPLICATE KEY UPDATE
`id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.objectstorage.default-block-split-length', '1048576', 'odc', 'default', 'master', '文件存储模块默认分块大小，单位为 byte，默认 1024*1024 bytes
(1M)') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.objectstorage.try-lock-timeout-milliseconds', '10000', 'odc', 'default', 'master', '文件存储模块锁超时时间，单位为毫秒，默认 10000 毫秒') ON
DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.script.max-edit-length', '#{20*1024*1024}', 'odc', 'default', 'master', '文件存储模块锁超时时间，单位为毫秒，默认 10000 毫秒') ON
DUPLICATE KEY UPDATE `id`=`id`;

-- 修改 odc.script.max-edit-length 配置项的 description 字段
UPDATE `config_system_configuration` SET `description`='脚本管理功能允许编辑的最大脚本长度，超出该长度的脚本不允许编辑，单位为字节，默认值 20 MB' WHERE `key`='odc.script.max-edit-length';

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.script.max-upload-length', '#{250*1024*1024}', 'odc', 'default', 'master', '文件存储模块锁超时时间，单位为毫秒，默认 10000 毫秒') ON
DUPLICATE KEY UPDATE `id`=`id`;

-- 修改 odc.script.max-upload-length 配置项的 description 字段
UPDATE `config_system_configuration` SET `description`='脚本管理功能允许上传的最大脚本长度，超出该长度的脚本不允许上传，单位为字节，默认值 250 MB' WHERE `key`='odc.script.max-upload-length';

INSERT INTO `config_system_configuration` (`key`, `value`, `description`)
VALUES ('odc.rpc.connect-timeout-seconds', '10', 'rpc 调用连接超时时间，单位为秒，默认是 10 秒') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `description`)
VALUES ('odc.rpc.read-timeout-seconds', '60', 'rpc 调用超时时间，单位为秒，默认是 60 秒') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.flow.async.max-upload-file-count', '500', 'odc', 'default', 'master', '异步任务最大上传文件数量，默认 500 个') ON
DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.flow.async.max-upload-file-total-size', '#{256*1024*1024}', 'odc', 'default', 'master', '异步任务最大上传文件总大小，单位为字节，默认 256 MB') ON
DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.task.file-expire-hours', '336', '流程任务附带文件最多保留小时数，默认 336 小时，即 2 星期') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.connect.host-white-list',
 '', 'ODC 数据库连接主机白名单，只允许用户连接在白名单内的主机，防止 SSRF 安全漏洞。默认为空，表示允许所有访问所有主机') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO `config_system_configuration` (`key`, `value`, `description`) VALUES ('odc.connect.min-query-timeout-seconds',
   '60', 'ODC 数据库连接最小查询超时时间，单位 秒，默认值 60') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO `config_system_configuration` (`key`, `value`, `description`) VALUES ('odc.connect.private.temp-connection-only',
   'false', '是否开启个人连接只支持创建临时链接，true 表示开启，默认值 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO `config_system_configuration` (`key`, `value`, `description`) VALUES ('odc.connect.temp-connection-operations',
   '*', '临时连接支持的操作列表，逗号分隔多个操作，支持的操作包括  create/delete/update/read ，为空或者 * 表示所有操作，默认值为 *') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO `config_system_configuration` (`key`, `value`, `description`) VALUES ('odc.connect.persistent-connection-operations',
   '*', '持久连接支持的操作列表，逗号分隔多个操作，支持的操作包括  create/delete/update/read ，为空或者 * 表示所有操作，默认值为 *') ON DUPLICATE KEY UPDATE `id`=`id`;

--
-- v3.3.2
--

-- 4A HTTP
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.http.connect-timeout-seconds',
 '5', '4A  HTTP API 连接超时，默认值 5') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.http.read-timeout-seconds',
 '20', '4A  HTTP API 读超时，默认值 20') ON DUPLICATE KEY UPDATE `id`=`id`;

-- 4A 审批拦截
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.enabled',
 'false', '是否启用 4a 拦截，默认值 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.batch-check-enabled',
 'false', '是否支持 批量 check API，默认值 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.approve-timeout-seconds',
 '3600', '4A 拦截审批超时时长，默认为 3600 （1 小时），如果超过审批超时时长没有获取到审批结果，则自动取消变更任务') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.approve-account-name',
 'CHANGE_ME', '4A 拦截审批审批账号名称，ODC 会根据这个账号名称查找待审批的数据库变更任务，和 4A 金库服务审批任务集成') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.api-list',
 '/api/v2/connect/sessions/*/sqls/asyncExecute,/api/v1/schema/tableModify/*,/api/v1/schema/plModify/*',
 '需要拦截的 API 清单，多个值使用逗号分隔，当 odc.chinamobile.4a.intercept.enabled = true 时有效') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.check-url',
 '', 'SQL 检查 API URL，即是否需要触发金库接口') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.batch-check-url',
  '', 'SQL 批量检查 API URL，即是否需要触发金库接口，和 check-url 相比，发送的 sqlStatement 参数改为 sqlStatements 数组') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.status-url',
 '', '审批状态查询 API URL，即查询金库审批是否通过') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.intercept.cancel-url',
  '', '取消金库审批 URL，预期用于取消尚未完成审批的工单，当前未使用') ON DUPLICATE KEY UPDATE `id`=`id`;

-- 4A Mock API
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.mock.enabled',
  'false', '是否开启 4A 审批 MOCK，默认为 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.mock.approve-accept-sql-pattern',
  '.*', '4A 审批 MOCK 审批通过 SQL 匹配规则，语法为正则表达式，odc.chinamobile.4a.mock.api.enabled=true 时生效，默认为 .*， 表示全都匹配')
  ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.mock.approve-deny-sql-pattern',
  '', '4A 审批 MOCK 审批拒绝 SQL 匹配规则，语法为正则表达式，odc.chinamobile.4a.mock.api.enabled=true 时生效，默认为空， 表示全都不匹配')
  ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.mock.block-sql-pattern',
  '', '4A 审批 MOCK 阻拦 SQL 匹配规则，语法为正则表达式，odc.chinamobile.4a.mock.api.enabled=true 时生效，默认为空， 表示全都不匹配')
  ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.chinamobile.4a.mock.pass-sql-pattern',
  '', '4A 审批 MOCK 白名单 SQL 匹配规则，语法为正则表达式，odc.chinamobile.4a.mock.api.enabled=true 时生效，默认为空， 表示全都不匹配')
  ON DUPLICATE KEY UPDATE `id`=`id`;


-- 堡垒机集成
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.encryption.enabled',
 'false', '堡垒机集成参数是否加密，默认值 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.encryption.algorithm',
 'RAW', '堡垒机集成参数加密算法，可选值 RAW、AES256_BASE64、CMCC4A，默认值 RAW 表示不加密') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.encryption.secret',
 '', '堡垒机集成参数加密秘钥，当 algorithm 值非 RAW 时有效, 跳转时 data 参数值需使用此秘钥加密后传递，ODC 会使用此密钥对参数值解密') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.http.connect-timeout-seconds',
 '5', '堡垒机账号集成 HTTP API 连接超时，默认值 5') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.http.read-timeout-seconds',
 '20', '堡垒机账号集成 HTTP API 读超时，默认值 20') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.auto-login-enabled',
   'false', '堡垒机账号集成 是否支持自动登录，默认值 false') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.mock-username',
   '', '堡垒机账号集成 账号自动登录模拟账号名称，值非空则会忽略后续基于账号查询 API 的验证过程，以配置的 mock-username 作为当前登录用户名称，用于测试阶段集成验证，默认值为空，')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.request-method',
   'POST', '堡垒机账号集成 账号查询 API 请求 方法，用于堡垒机集成场景 ODC 调用外部服务确认账号信息是否有效，可选值 GET/POST/PUT/PATCH，默认值 POST')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.request-url',
   '', '堡垒机账号集成 账号查询 API 请求 URL，用于堡垒机集成场景 ODC 调用外部服务确认账号信息是否有效。')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.request-headers',
   '', '堡垒机账号集成 账号查询 API 请求 headers，值可引用模板变量，支持的模板变量包括：${account_verify_token}，为空表示不包含 headers')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.request-body',
   '', '堡垒机账号集成 账号查询 API 请求 body，值可引用模板变量，支持的模板变量包括：${account_verify_token}，为空表示不包含 request body')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.request-encrypted',
   'true', '堡垒机账号集成 账号查询 API request body 是否加密，默认值 true 表示加密')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.response-encrypted',
   'true', '堡垒机账号集成 账号查询 API response body 是否加密，默认值 true 表示加密')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.response-body-valid-expression',
   'true', '堡垒机账号集成 账号查询 API 调用成功判断 response body 判定表达式，默认值 true 表示不校验 response body')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.response-body-username-extract-expression',
   '[''data''][''username'']', '堡垒机账号集成 账号查询 API response body 账户名称提取表达式，默认值 [''data''][''username'']')
   ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.bastion.account.query.response-body-nickname-extract-expression',
   '[''data''][''nickName'']', '堡垒机账号集成 账号查询 API response body 账户昵称提取表达式，默认值 [''data''][''nickName'']')
   ON DUPLICATE KEY UPDATE `id`=`id`;

-- 连接管理
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.connect.temp.expire-after-inactive-interval-seconds',
   '86400', '临时连接不活跃之后的保留周期，单位：秒，默认值 86400') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.connect.temp.expire-check-interval-millis',
   '600000', '临时连接配置清理检查周期，单位：毫秒，默认值 600000 表示 10 分钟') ON DUPLICATE KEY UPDATE `id`=`id`;
update config_system_configuration set `value`='120000',`description`=
'连接 session 历史刷新时间间隔，单位：毫秒，默认值为 120000 表示 2 分钟，需要保证小于 session 在内存中可能存留的最短时间（3 分钟）'
 where `key`='odc.connect.session.history-update-interval-millis';

-- WEB 安全
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.security.csrf.enabled',
  'true', '是否开启 CSRF 防护，默认值 true')
  ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.security.cors.enabled',
  'false', '是否开启 CORS 支持，默认不支持，前端通过 CDN 部署且后端 API 部署的 domain 和前端访问 domain 不一致时需开启本项配置')
  ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.security.cors.allowedOrigins',
  '*', 'CORS 允许的 origins domain 列表，多个值使用逗号分隔，默认值 * 表示允许所有 Origins，当 odc.web.security.cors.enabled=true 时配置有效')
  ON DUPLICATE KEY UPDATE `id`=`id`;

--
-- v4.0.0
--
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.authentication.captcha.enabled', 'false',
'是否开启验证码机制，默认关闭') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.default-time-zone', 'Asia/Shanghai',
'oracle 模式下 timestamp with local time zone 类型的默认时区设置') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.user.default-roles', 'private_connection,apply_connection',
'创建新用户默认关联的角色，多个值用逗号分隔') ON DUPLICATE KEY UPDATE `id`=`id`;

-- OBCloud
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.obcloud.login-url',
  'CHANGE_ME', 'OB Cloud 环境登录 URL')
  ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.obcloud.logout-url',
  'CHANGE_ME', 'OB Cloud 环境登出 URL')
  ON DUPLICATE KEY UPDATE `id`=`id`;

-- SQL 执行
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.oracle.remove-comment-prefix',
 'false', 'SQL 执行 Oracle 模式是否移除注释前缀，默认为 false。如果设置为 true，SQL 拆句会去除注释前缀，可以用于绕过部分 OB 版本 (如 3.1.x) PL 语句包含注释前缀执行报错的问题') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.result-set.max-cached-lines',
 '10000', 'SQL 执行结果集缓存最大条目数，该配置主要影响大字段查看功能，若结果集规模太大可能超过此配置造成无法查看相关二进制数据') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.result-set.max-cached-size',
 '1073741824', 'SQL 执行结果集缓存最大字节数，该配置主要影响大字段查看功能，若结果集规模太大可能超过此配置造成无法查看相关二进制数据') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.max-result-set-rows',
   '-1', '结果集查询条数限制，默认为 -1，表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.max-single-session-count',
   '-1', '独立 session 模式下，允许建立的最大连接数，默认为 -1，表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.user-max-count',
   '-1', '允许创建 Session 的最大用户数，默认 -1，表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.backend-query-timeout-micros',
  '60000000', 'ODC 会话后台连接的查询超时时间，单位为微秒，默认 60 秒') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.add-internal-rowid',
  'true', 'SQL 执行时是否对用户输入的 SQL 进行改写增加 rowid 字段，默认为 true') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.timeout-mins',
  '480', 'ODC 连接会话超时时间，单位为分钟，默认 480 分钟') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.max-sql-length',
   '-1', '单次执行的最大 SQL 语句长度，默认为 0, <=0 表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.max-sql-statement-count',
   '-1', '单次执行的最大 SQL 语句数量，默认为 0, <=0 表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.database.max-table-count',
   '-1', '当前 database 最大表数量，默认为 0， <=0 表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.database.max-table-partition-count',
   '-1', '当前 database 最大表分区数量，默认为 0， <=0 表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.database.max-data-size',
   '-1', '当前 database 最大数据空间占用，单位字节，默认为 0， <=0 表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.database.max-memstore-size',
   '-1', '当前 database 最大 MemStore 空间占用，单位字节，默认值 -1， <=0 表示不限制') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.full-link-trace.enabled',
   'true', '是否开启全链路诊断的功能，默认为开启') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.full-link-trace-timeout-seconds',
   '60', '查询全链路追踪的超时时间，单位为秒，默认 60 秒') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.security.file.upload.safe-suffix-list',
   '*', '允许上传的文件名扩展名，默认 *，表示允许所有文件扩展名') ON DUPLICATE KEY UPDATE `id`=`id`;

--
-- cloud object-storage
--
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.provider',
 'NONE', '云存储服务提供厂商，可选值 NONE/ALIBABA_CLOUD/AWS ，默认为 NONE 表示不使用云储存') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.region',
 'CHANGE_ME', '云存储服务 RegionId') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.endpoint',
 'CHANGE_ME', '云存储服务 Endpoint') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) (SELECT 'odc.cloud.object-storage.public-endpoint', `value`, '云存储服务公网 Endpoint' FROM config_system_configuration WHERE `key`='odc.cloud.object-storage.endpoint')
 ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.internal-endpoint',
 'CHANGE_ME', '云存储服务内网 Endpoint') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.access-key-id',
 'CHANGE_ME', '云存储服务 accessKeyId') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.access-key-secret',
 'CHANGE_ME', '云存储服务 accessKeySecret') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.bucket-name',
 'CHANGE_ME', '云存储服务 bucketName') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.role-arn',
 'CHANGE_ME', '云存储服务 roleArn，用于生成 STS 临时 token') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.role-name',
 'ODCCloudStoragePutOnly', '云存储服务 roleName，用于在云环境查询 roleArn，默认值 ODCCloudStoragePutOnly') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.cloud.object-storage.role-session-name',
 'ODCCloudStorageTempUpload', '云存储服务 roleSessionName，用于生成 STS 临时 token，默认值 ODCCloudStorageTempUpload') ON DUPLICATE KEY UPDATE `id`=`id`;

-- file interaction mode
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.file.interaction-mode',
'LOCAL', '文件交互模式，可选值 LOCAL/CLOUD_STORAGE，默认值 LOCAL') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('sqlexecute.sqlCheckMode', 'AUTO', 'odc', 'default', 'master', 'Default sql check mode can only accept the
value AUTO|MANUAL') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.automatic-auth-rule.enabled', 'true', 'odc', 'default', 'master', '是否开启自动授权规则功能，默认为 true，表示开启；多云环境默认关闭') ON DUPLICATE KEY UPDATE `id`=`id`;

--
-- v4.1.3
--
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.enabled', 'true', 'odc', 'default', 'master', '是否开启消息通知，默认 false，表示不开启') ON DUPLICATE KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.event-dequeue-batch-size', '5', 'odc', 'default', 'master', '批量处理事件数量，默认 5') ON DUPLICATE KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.notification-dequeue-batch-size', '5', 'odc', 'default', 'master', '批量处理消息通知数量，默认 5') ON DUPLICATE KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.dequeue-event-fixed-delay-millis', '10000', 'odc', 'default', 'master', '处理事件的定时任务周期，默认 10000 MS') ON DUPLICATE
 KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.dequeue-created-notification-fixed-delay-millis', '20000', 'odc', 'default', 'master', '处理 CREATED 消息的定时任务周期，默认 20000 MS') ON DUPLICATE KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.dequeue-failed-notification-fixed-delay-millis', '60000', 'odc', 'default', 'master', '处理 SENT_FAILED 消息的定时任务周期，默认 60000 MS') ON DUPLICATE KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.dequeue-sending-notification-fixed-delay-millis', '120000', 'odc', 'default', 'master', '处理 SENDING 消息的最长时间，超过此时间未响应则重新发送，默认 120000 MS') ON DUPLICATE KEY update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.max-resend-times', '3', 'odc', 'default', 'master', '重新处理 SENT_FAILED 消息的最大重试次数，默认 3') ON DUPLICATE KEY
update `id`=`id`;
insert into `config_system_configuration` (`key`, `value`, `application`, `profile`, `label`, `description`)
VALUES ('odc.notification.host-black-list', '', 'odc', 'default', 'master', 'The Hosts in this black list are NOT allowed to prevent SSRF security vulnerabilities. The black list is empty by default, allowing access to all Hosts.') ON DUPLICATE KEY update `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.resource.mysql-init-script-template',
'create user if not exists {{dbUsername}}@''%'' identified by {{password}};  create database if not exists {{dbName}};  grant all privileges on {{dbName}}.* to {{dbUsername}}@''%'';  grant select on oceanbase.gv$tenant to {{dbUsername}}@''%'';  grant select on oceanbase.gv$unit to {{dbUsername}}@''%'';  grant select on oceanbase.gv$table to {{dbUsername}}@''%'';  grant select on oceanbase.gv$sysstat to {{dbUsername}}@''%'';  grant select on oceanbase.gv$memory to {{dbUsername}}@''%'';  grant select on oceanbase.gv$memstore to {{dbUsername}}@''%'';  grant select on oceanbase.gv$sql_audit to {{dbUsername}}@''%'';  grant select on oceanbase.gv$plan_cache_plan_stat to {{dbUsername}}@''%'';  grant select on oceanbase.gv$plan_cache_plan_explain to {{dbUsername}}@''%'';'
, '实验室体验资源创建脚本模板，MySQL 模式，包含 create database/create user/grant privilege 过程，支持的变量包括 {{dbName}}, {{dbUsername}}, {{password}}'
) ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.resource.mysql-revoke-script-template',
'create user if not exists {{dbUsername}}@''%'' identified by {{password}};   drop user {{dbUsername}};   drop database if exists {{dbUsername}};'
, '实验室体验资源回收脚本模板，MySQL 模式，包含 drop database/drop user 过程，支持的变量包括 {{dbName}}, {{dbUsername}}, {{password}}'
) ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.resource.oracle-init-script-template',
'-- TO_BE_REPLACED'
, '实验室体验资源创建脚本模板，Oracle 模式，包含 create user/grant privilege 过程，支持的变量包括 {{dbName}}, {{dbUsername}}, {{password}}') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.lab.resource.oracle-revoke-script-template',
'-- TO_BE_REPLACED'
, '实验室体验资源回收脚本模板，Oracle 模式，包含 drop user 过程，支持的变量包括 {{dbName}}, {{dbUsername}}, {{password}}') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.security.denied-http-requests','{}', '禁用的 API 列表，json 格式，默认为空。一个例子：[{"antMatcherPath":"/api/v2/iam/users","method":"POST"},{"antMatcherPath":"/api/v2/iam/users/batchCreate","method":"POST"}]') ON DUPLICATE KEY UPDATE `id`=`id`;

--
-- v4.2.0
--
INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
	( 'odc.site.url', 'http://localhost:8989', '外部访问 ODC 网站的地址，以 http/https 开始，包含VIP地址/域名/端口的网址，且结尾不含斜杠（/）' )
	ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
	( 'odc.integration.sql-interceptor.connect-timeout-seconds', '5', 'SQL 拦截集成 HTTP 请求连接超时时间，单位：秒' )
	ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
	( 'odc.integration.sql-interceptor.socket-timeout-seconds', '30', 'SQL 拦截集成 HTTP 请求 Socket 超时时间，单位：秒' )
	ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.integration.approval.connect-timeout-seconds', '5', '审批集成 HTTP 请求连接超时时间，单位：秒' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.integration.approval.socket-timeout-seconds', '30', '审批集成 HTTP 请求 Socket 超时时间，单位：秒' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.task.async.result-preview-max-size-bytes', '5242880', '数据库变更任务查询结果预览最大文件大小，单位：字节' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.task.async.rollback.total-max-change-lines', '1000000', '生成备份回滚方案支持的数据库变更任务的最大变更行数' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.task.async.rollback.max-rollback-content-size-bytes', '268435456', '生成备份回滚方案支持的数据库变更任务生成的最大文件大小，单位：字节。默认为 256 MB' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.task.async.rollback.max-timeout-millisecond', '900000', '数据库变更任务中生成备份回滚方案节点的超时时间，单位：毫秒。默认 15 分钟' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.rollback.each-sql-max-change-lines', '100000', '生成备份回滚方案支持的单条变更 sql 的最大变更行数' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.rollback.query-data-batch-size', '1000', '生成备份回滚方案批量查询数据的数量' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.cloud.enabled-instance-ids',
  '', 'instances that enable OSC')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.enable-full-verify',
  'false', 'enable oms migrate link full verify or not, default is false')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.oms.url',
  'CHANGE_ME', 'oms url')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.oms.authorization',
  'CHANGE_ME', 'oms authorization, base64(username:password)')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.oms.region',
  'CHANGE_ME', 'oms region')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.oms.check-project-step-failed-timeout-seconds',
  '120', 'check oms project step failed, if failed timeout 120 seconds, osc task will failed')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.osc.oms.check-project-progress-cron-expression',
  '0/10 * * * * ?', 'check oms project progress cron expression')
  ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.features.task.osc.enabled', 'false',
'是否开启无锁结构变更任务，默认不开启') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.max-concurrent-task-count', '30', '计划任务最大并发数' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.connect.database.sync.interval-millis', '180000', '同步数据源下所有数据库到 metadb 的间隔时间，默认 3 分钟，单位毫秒' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.default-single-task-row-limit', '20000', 'DLM 单个任务默认每秒行限制' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.max-single-task-row-limit', '50000', 'DLM 单个任务最大每秒行限制' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.default-single-task-data-size-limit', '1024', 'DLM 单个任务默认每秒数据量限制，单位：KB' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.max-single-task-data-size-limit', '10240', 'DLM 单个任务最大每秒数据量限制，单位：KB' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.default-single-thread-batch-size', '200', 'DLM 单条 SQL 处理数据行数' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.single-task-read-write-ratio', '0.5', 'DLM 单个任务读写线程比值，默认 0.5 即读写线程个数为 1:2' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.single-task-thread-pool-size', '12', 'DLM 单个任务可用线程数' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.thread-pool-size', '100', '单个 POD 中 DLM 任务线程池大小' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.support-breakpoint-recovery', 'true', 'DLM 任务是否开启断点恢复' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.task-connection-query-timeout-seconds', '180', 'DLM 任务 SQL 超时时间' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.sharding-strategy', 'FIXED_LENGTH', 'DLM 分片策略，默认值 FIXED_LENGTH 表示均匀分片，适合小规格实例。使用 MATCH 策略时将出现少量慢 SQL，整体性能会有较大提升，适合大规格实例。' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.dlm.default-scan-batch-size', '10000', 'DLM 分片大小，默认值 10000 表示分片 SQL 每次会扫描 10000 个主键' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.datatransfer.use-server-prep-stmts', 'true', '导入导出是否开启 ps 协议，默认为开启' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.datatransfer.cursor-fetch-size', '20', '导出时游标的 fetch size，默认为 20，最大值为 1000' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.data-security.masking.enabled', 'true', '是否开启数据脱敏，默认为开启' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.partition-plan.schedule-cron', '0 0 * * * ?', '默认调度周期：每天 0 点' ) ON DUPLICATE KEY UPDATE `id` = `id`;

--
-- v4.2.1
--
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.integration.url-white-list',
 '', 'Only whitelisted URLs are allowed when configuring the integration API to prevent SSRF security vulnerabilities. The whitelist is empty by default, allowing access to all URLs.') ON DUPLICATE KEY UPDATE `id`=`id`;

---
--- v4.2.3
---
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.iam.auth.alipay.max-failed-login-attempt-times', '5', '登录失败情况下最大重试次数，小于等于 0 意味着次数无限制，改变此参数后需要重启生效' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.iam.auth.alipay.failed-login-lock-timeout-seconds', '600', '账户被锁定时长，默认 600 秒，如果该值小于等于 0 意味着不锁定，改变此参数后需要重启生效' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.integration.bastion.enabled', 'false', '堡垒机集成是否启用，默认值 false，改变此参数后需要重启生效') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.server.obclient.command-black-list',
 'connect,conn,edit,nopager,notee,pager,print,prompt,rehash,system,tee,resetconnection', 'Prohibited commands that can be executed by obclient' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.task.pre-check.max-sql-content-bytes',
 '5242880', '预检查时所允许检查的 SQL 内容的最大长度，超过该上限将终止预检查并将检查结果置为最高风险等级。单位：字节，默认值：5242880（即 5MB），修改后重启生效') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.enabled', 'true', 'enable task-framework or not' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.run-mode', 'PROCESS', 'ODC task run mode contain: PROCESS/K8S, default is PROCESS' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.odc-url', '', 'odc server url is used to report task result by TaskExecutor, use odc server ip when odc url is null') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.start-preparing-job-cron-expression', '0/1 * * * * ?', 'start preparing job cron expression, modify value restart to take affect' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.check-running-job-cron-expression', '0/10 * * * * ?', 'check running job cron expression, modify value restart to take affect' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.do-canceling-job-cron-expression', '0/1 * * * * ?', 'check canceling job cron expression, modify value restart to take affect' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.destroy-executor-job-cron-expression', '0/1 * * * * ?', 'check terminate job to destroy executor cron expression, modify value restart to take affect' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.job-heart-timeout-seconds', '300', 'job heart timeout seconds, job will failed or retrying' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.executor-waiting-to-run-threshold-seconds', '3', 'time threshold of executor waiting to run, for controller schedule rate' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.executor-waiting-to-run-threshold-count', '10', 'amount threshold of executor waiting to run, for controller schedule rate' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.job-process-min-memory-size-in-mb', '1024', 'ob process min memory size in mb' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.job-process-max-memory-size-in-mb', '1024', 'job process max memory size in mb' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.system-reserve-min-free-memory-size-in-mb', '1024', 'system reserve min free memory size in mb' ) ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.kube-config', '', 'kube config base64 encoded is used k8s connect default' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.kube-url', '', 'kube url be used to connect k8s when kube config is null' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.region', '', 'k8s region id' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.namespace', 'default', 'k8s namespace name' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.request-cpu', '2', 'k8s pod request cpu' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.request-mem', '4096', 'k8s pod request memory, unit is MB' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.limit-cpu', '2', 'k8s pod limit cpu' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.limit-mem', '4096', 'k8s pod limit memory,unit is MB' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.max-node-count', '', 'k8s max node count' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.node-cpu', '', 'cpu count of single k8s node' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.node-mem-in-mb', '', ' memory size of single k8s node, unit is MB' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.enable-mount', 'false', 'k8s pod enable mount' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.mount-path', '', 'k8s pod mount path on host' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.mount-disk-size', '64', 'k8s pod mount disk size, unit is GB' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.pod-image-name', '', 'k8s pod image name' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task-framework.k8s-properties.pod-pending-timeout-seconds', '3600', 'pod will be destroyed if pending exceed this time, default time is 1h' ) ON DUPLICATE KEY UPDATE `id` = `id`;
INSERT INTO config_system_configuration ( `key`, `value`, `description` ) VALUES( 'odc.task.pre-check.execution-timeout-millis', '3600000', 'Pre-check task execution timeout, in milliseconds, default value: 3600000 i.e. 1 hour') ON DUPLICATE KEY UPDATE `id`=`id`;

---
--- v4.2.4
---
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.permission.expired-retention-time-seconds',
 '7776000', 'How long expired permissions are retained, in seconds, defaults to 90 days') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.security.basic-authentication.enabled',
 'false', 'enable basic authentication or not, false by default') ON DUPLICATE KEY UPDATE `id`=`id`;


INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.web.stateful-route.host.type',
                                                                              'ipAddress', 'host type used for forwarding, use ipAddress or hostName') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration ( `key`, `value`, `description` )
VALUES
  ( 'odc.task.async.index-change-max-timeout-millis', '432000000', 'If the change content of the database change task involves time-consuming index change operations, the timeout period for the automatically modified database change task, unit: milliseconds. Default value is 5 days.' )
  ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.jwt.expiration-seconds',
 '900', 'How long the jwt remain valid, in seconds, defaults to 900') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.jwt.buffer-seconds',
 '180', 'How long does the jwt need to be renewed before it expires, in seconds, defaults to 180') ON DUPLICATE KEY UPDATE `id`=`id`;

INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.iam.auth.method',
 'jsession', 'The authentication mode used for login, there are two alternatives: jwt and jsession, the default is jsession') ON DUPLICATE KEY UPDATE `id`=`id`;

---
--- v4.3.0
---
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.cron-expression',
 '0 0 2 * * ?', 'cron expression for synchronizing full database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.executor-thread-count',
 '8', 'thread count for synchronizing database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.block-exclusions-when-sync-db-to-project',
 'true', 'whether to block exclusions when syncing the database to the project') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.block-exclusions-when-sync-db-schemas',
 'true', 'whether to block exclusions when syncing the database schemas') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.exclude-schemas.ob-mysql',
 'mysql, information_schema, test, oceanbase', 'schema exclusions when synchronizing OceanBase MySQL mode database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.exclude-schemas.ob-oracle',
 'SYS, LBACSYS, ORAAUDITOR', 'schema exclusions when synchronizing OceanBase Oracle mode database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.exclude-schemas.oracle',
 'SYS, SYSTEM, SYSMAN, SCOTT, HR, OE, SH, PM, IX, BI, ORDSYS, ORDSAMPLE, ORDDATA, MDSYS, OLAPSYS, XDB', 'schema exclusions when synchronizing Oracle database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.exclude-schemas.mysql',
 'mysql, information_schema, test', 'schema exclusions when synchronizing MySQL database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.database.schema.sync.exclude-schemas.doris',
 'mysql, information_schema, test', 'schema exclusions when synchronizing MySQL database schema') ON DUPLICATE KEY UPDATE `id`=`id`;
