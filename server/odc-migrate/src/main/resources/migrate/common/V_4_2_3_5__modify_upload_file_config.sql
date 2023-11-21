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

DELETE FROM
  `config_system_configuration`
WHERE
  `key` = 'odc.flow.async.max-upload-file-total-size}';

INSERT INTO
  config_system_configuration (`key`, `value`, `description`)
VALUES(
    'odc.flow.async.max-upload-file-total-size',
    '#{32*1024*1024}',
    '异步任务最大上传文件总大小，单位为字节，默认 32 MB'
  ) ON DUPLICATE KEY
UPDATE
  `value` = '#{32*1024*1024}';
