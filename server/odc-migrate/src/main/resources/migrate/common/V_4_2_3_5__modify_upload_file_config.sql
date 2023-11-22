--
-- Modify upload file config
--
UPDATE
  `config_system_configuration`
SET
  `value` = '100',
  `description` = 'Maximum number of uploaded files for database change task, default 100'
WHERE
  `key` = 'odc.flow.async.max-upload-file-count';
