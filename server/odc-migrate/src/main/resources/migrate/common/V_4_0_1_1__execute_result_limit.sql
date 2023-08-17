INSERT INTO config_system_configuration(`key`, `value`, `description`) VALUES('odc.session.sql-execute.default-result-set-rows',
   '1000', '查询结果集默认行数，默认 1000 行') ON DUPLICATE KEY UPDATE `id`=`id`;

UPDATE config_system_configuration set `value`=100000 where `key`='odc.session.sql-execute.max-result-set-rows';