-- disable data masking in client mode
update config_system_configuration set `value`='false' where `key`='odc.data-security.masking.enabled';
