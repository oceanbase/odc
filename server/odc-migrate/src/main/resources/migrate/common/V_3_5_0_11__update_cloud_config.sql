UPDATE
  `config_system_configuration`
SET
  `value` = 1048576,
  `description` = '公有云模式下接口最大下行数据量，单位为字节，默认大小 1024KB，超过此限制则通过OSS文件进行返回值中转'
where
  `key` = 'odc.cloud.max-response-size';