update `config_system_configuration` set `value` = 'true' where `key` = 'odc.task-framework.enabled';
update `config_system_configuration` set `value` = '3' where `key` = 'odc.task-framework.executor-waiting-to-run-threshold-seconds';
update config_system_configuration set `value`='0/10 * * * * ?' where `key` = 'odc.task-framework.check-running-job-cron-expression';
update `config_system_configuration` set `value` = 'true' where `key` = 'odc.notification.enabled';