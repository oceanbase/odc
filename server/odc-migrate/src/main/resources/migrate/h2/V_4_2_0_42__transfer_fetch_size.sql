-- set cursor fetch size of ob-dumper to 100
update config_system_configuration set `value`='100' where `key`='odc.task.datatransfer.cursor-fetch-size';
