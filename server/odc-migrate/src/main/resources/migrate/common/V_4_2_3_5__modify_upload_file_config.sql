--
-- Modify upload file config
--
DELETE FROM
  `config_system_configuration`
WHERE
  `key` = 'odc.flow.async.max-upload-file-total-size}';
