--
-- Modify upload file config
--
UPDATE
  `config_system_configuration`
SET
  `value` = '100',
  `description` = '异步任务最大上传文件数量，默认 100 个'
WHERE
  `key` = 'odc.flow.async.max-upload-file-count';
