-- initial empty for avoid duplicated records
-- TODO: add unique index for odc_version_diff_config
truncate table odc_version_diff_config;

-- v2.0.0
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_MYSQL',
'int:NUMERIC, numeric:NUMERIC, decimal:NUMERIC, bit:NUMERIC, tinyint:NUMERIC, bool:NUMERIC, boolean:NUMERIC, smallint:NUMERIC, mediumint:NUMERIC, bigint:NUMERIC, float:NUMERIC, double:NUMERIC, varchar():TEXT, char():TEXT, tinytext:OBJECT, mediumtext:OBJECT, text:OBJECT, longtext:OBJECT, tinyblob:OBJECT, blob:OBJECT, mediumblob:OBJECT, longblob:OBJECT, binary:TEXT, varbinary():TEXT, timestamp:TIMESTAMP, date:DATE, time:TIME, datetime:DATETIME, year:YEAR', '1.4.70')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE',
'INTEGER:NUMERIC, NUMBER:NUMERIC, CHAR:TEXT, VARCHAR:TEXT, VARCHAR2:TEXT, BLOB:OBJECT, CLOB:OBJECT, DATE:DATE, TIMESTAMP:TIMESTAMP,
 TIMESTAMP WITH TIME ZONE:TIMESTAMP, TIMESTAMP WITH LOCAL TIME ZONE:TIMESTAMP', '2.1.0') ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE', 'RAW:TEXT,
INTERVAL YEAR TO MONTH:TEXT, INTERVAL DAY TO SECOND:TEXT', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE', 'NCHAR:TEXT,
NVARCHAR2:TEXT', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;

insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_partition_modify', 'OB_MYSQL', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_view', 'OB_ORACLE', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_procedure', 'OB_MYSQL', 'true', '2.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_procedure', 'OB_ORACLE', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_function', 'OB_MYSQL', 'true', '2.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_function', 'OB_ORACLE', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_partition_modify', 'OB_ORACLE', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- v2.1.0
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_sequence', 'OB_ORACLE', 'true', '2.1.1')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- v2.2.0
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_constraint_modify', 'OB_ORACLE', 'true', '2.2.50')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_constraint_modify', 'OB_MYSQL', 'true', '2.2.50')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_package', 'OB_ORACLE', 'true', '2.2.1')  ON DUPLICATE KEY update `config_key`=`config_key`;

update odc_version_diff_config set config_value = 'int:NUMERIC, numeric:NUMERIC, decimal:NUMERIC, bit:NUMERIC, tinyint:NUMERIC, smallint:NUMERIC, mediumint:NUMERIC, bigint:NUMERIC, float:NUMERIC, double:NUMERIC, varchar():TEXT, char():TEXT, tinytext:OBJECT, mediumtext:OBJECT, text:OBJECT, longtext:OBJECT, tinyblob:OBJECT, blob:OBJECT, mediumblob:OBJECT, longblob:OBJECT, binary:TEXT, varbinary():TEXT, timestamp:TIMESTAMP, date:DATE, time:TIME, datetime:DATETIME, year:YEAR'
where config_key = 'column_data_type' and db_mode = 'OB_MYSQL';

-- v2.2.1
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_pl_debug', 'OB_ORACLE', 'true', '2.2.73')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- v2.3.0
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_view', 'OB_MYSQL', 'true', '1.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_partition_modify', 'OB_MYSQL', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_sequence', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_package', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_pl_debug', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_rowid', 'OB_ORACLE', 'true', '2.2.70')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_rowid', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- v2.4.0
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_type', 'OB_ORACLE', 'true', '2.2.76')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger', 'OB_ORACLE', 'true', '2.2.50')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_compile', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_compile', 'OB_ORACLE', 'false', '2.2.70')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_ddl', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_ddl', 'OB_ORACLE', 'true', '2.2.60')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_references', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_references', 'OB_ORACLE', 'true', '2.2.70')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_alterstatus', 'OB_MYSQL', 'false', '3.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_trigger_alterstatus', 'OB_ORACLE', 'true', '2.2.70')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_synonym', 'OB_MYSQL', 'true', '2.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_synonym', 'OB_ORACLE', 'true', '2.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- v2.4.1
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_kill_query', 'OB_ORACLE', 'true', '2.2.70')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- v3.2.2
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_show_foreign_key', 'OB_ORACLE', 'true', '1.0.0') ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_show_foreign_key', 'OB_MYSQL', 'true', '3.1.2') ON DUPLICATE KEY update `config_key`=`config_key`;

-- v3.2.3
-- OB_MYSQL does not support synonym now
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_type', 'OB_MYSQL', 'false', '1.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
update odc_version_diff_config set config_value='false' where config_key='support_synonym' and db_mode='OB_MYSQL';

-- OB_ORACLE not support show foreign key until 312
update odc_version_diff_config set min_version='3.1.2' where config_key='support_show_foreign_key' and db_mode='OB_ORACLE';

-- v3.2.3 patch1
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_data_export', 'OB_MYSQL', 'true', '1.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_data_export', 'OB_ORACLE', 'true', '2.2.0')  ON DUPLICATE KEY update `config_key`=`config_key`;


-- v3.3.0
-- add ODP sharding config
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('column_data_type','ODP_SHARDING_OB_MYSQL','int:NUMERIC, numeric:NUMERIC, decimal:NUMERIC, bit:NUMERIC, tinyint:NUMERIC, smallint:NUMERIC, mediumint:NUMERIC, bigint:NUMERIC, float:NUMERIC, double:NUMERIC, varchar():TEXT, char():TEXT, tinytext:OBJECT, mediumtext:OBJECT, text:OBJECT, longtext:OBJECT, tinyblob:OBJECT, blob:OBJECT, mediumblob:OBJECT, longblob:OBJECT, binary:TEXT, varbinary():TEXT, timestamp:TIMESTAMP, date:DATE, time:TIME, datetime:DATETIME, year:YEAR','1.0.0',CURRENT_TIMESTAMP)  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_modify','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_procedure','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_function','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint_modify','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_view','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sequence','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_package','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_pl_debug','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_rowid','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_compile','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_ddl','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_references','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_alterstatus','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_synonym','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_show_foreign_key','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_type','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

--
-- new feature switcher for adapt ODP sharding mode
--

-- mock data
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_mock_data','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_mock_data','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_mock_data','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- recycle bin
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_recycle_bin','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_recycle_bin','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_recycle_bin','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- data import/export
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_import','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_import','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_import','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_export','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_export','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_export','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- session management
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_session','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_session','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_session','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_query','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_query','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_query','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- sql trace and explain
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_trace','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_trace','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_trace','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_explain','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_explain','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_explain','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- constraint
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- partition
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition','OB_MYSQL','true','1.4.70',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition','OB_ORACLE','true','2.2.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- add while merge from 3.2.3_release into 3.3.x_dev
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('support_data_export', 'ODP_SHARDING_OB_MYSQL', 'true', '1.0.0')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- 3.4.0
update odc_version_diff_config set config_value='int:NUMERIC, numeric:NUMERIC, decimal:NUMERIC, bit:NUMERIC, tinyint:NUMERIC,
smallint:NUMERIC, mediumint:NUMERIC, bigint:NUMERIC, bool:NUMERIC, boolean:NUMERIC, float:NUMERIC, double:NUMERIC, varchar():TEXT, char():TEXT, tinytext:OBJECT,
mediumtext:OBJECT, text:OBJECT, longtext:OBJECT, tinyblob:OBJECT, blob:OBJECT, mediumblob:OBJECT, longblob:OBJECT, binary:TEXT, varbinary():TEXT, timestamp:TIMESTAMP, date:DATE, time:TIME, datetime:DATETIME, year:YEAR, set:SET, enum:ENUM' where
config_key='column_data_type' and db_mode='OB_MYSQL';

insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE',
'FLOAT:NUMERIC, BINARY_FLOAT:NUMERIC, BINARY_DOUBLE:NUMERIC, ROWID:OBJECT, UROWID:OBJECT', '2.2.0')  ON DUPLICATE
KEY update `config_key`=`config_key`;


-- 4.0.0
-- shadowtable
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_shadowtable','OB_MYSQL','true','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_shadowtable','OB_ORACLE','false','1.1.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_shadowtable','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_plan','OB_MYSQL','true','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_plan','OB_ORACLE','false','1.1.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_plan','ODP_SHARDING_OB_MYSQL','false','1.0.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- support mysql
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_view','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_procedure','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_function','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_mock_data','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_explain','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_trace','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_plan','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint_modify','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_modify','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_shadowtable','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_show_foreign_key','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_data_export','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_import','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_export','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_session','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_query','MYSQL','true','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sequence','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_package','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_rowid','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_recycle_bin','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_compile','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_ddl','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_references','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_alterstatus','MYSQL','false','5.7.0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'MYSQL', 'bit:NUMERIC, tinyint:NUMERIC, bool:NUMERIC, boolean:NUMERIC, smallint:NUMERIC, mediumint:NUMERIC, int:NUMERIC, bigint:NUMERIC, decimal:NUMERIC, float:NUMERIC, double:NUMERIC, date:DATE, time:TIME, datetime:DATETIME, timestamp:TIMESTAMP, year:YEAR, char:TEXT, varchar:TEXT, binary:TEXT,  varbinary:TEXT, blob:OBJECT, tinyblob:OBJECT, mediumblob:OBJECT, longblob:OBJECT, text:OBJECT, tinytext:OBJECT, mediumtext:OBJECT, longtext:OBJECT, json:OBJECT, enum:ENUM, set:SET', '5.7.0') ON DUPLICATE KEY update `config_key`=`config_key`;

insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_session','DORIS','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_query','DORIS','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

-- support json datatype
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_MYSQL',
'json:OBJECT', '3.2.4')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE',
'JSON:OBJECT', '4.1')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- support gis datatype
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_MYSQL',
'geometry:OBJECT, point:OBJECT, linestring:OBJECT, polygon:OBJECT, multipoint:OBJECT, multilinestring:OBJECT, multipolygon:OBJECT, geometrycollection:OBJECT', '3.2.4')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'MYSQL',
'geometry:OBJECT, point:OBJECT, linestring:OBJECT, polygon:OBJECT, multipoint:OBJECT, multilinestring:OBJECT, multipolygon:OBJECT, geometrycollection:OBJECT', '5.7.0')  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE',
'SDO_GEOMETRY:OBJECT', '4.2.2')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- supports sys_refcursor
insert into odc_version_diff_config(config_key, db_mode, config_value, min_version) values('column_data_type', 'OB_ORACLE', 'SYS_REFCURSOR:OBJECT', '2.2.76')  ON DUPLICATE KEY update `config_key`=`config_key`;

-- support oracle datasource
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_view','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_function','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_procedure','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_package','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_type','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sequence','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_synonym','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_mock_data','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_explain','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_sql_trace','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_plan','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_constraint_modify','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_partition_modify','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_shadowtable','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_show_foreign_key','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_data_export','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_import','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_db_export','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_session','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_kill_query','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_rowid','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_recycle_bin','ORACLE','false','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_compile','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_ddl','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_references','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_trigger_alterstatus','ORACLE','true','0',CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_pl_debug', 'ORACLE', 'false', '0', CURRENT_TIMESTAMP)  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('column_data_type', 'ORACLE',
'INTEGER:NUMERIC, NUMBER:NUMERIC, CHAR:TEXT, VARCHAR:TEXT, VARCHAR2:TEXT, BLOB:OBJECT, CLOB:OBJECT, DATE:DATE, TIMESTAMP:TIMESTAMP,
 TIMESTAMP WITH TIME ZONE:TIMESTAMP, TIMESTAMP WITH LOCAL TIME ZONE:TIMESTAMP, RAW:TEXT, INTERVAL YEAR TO MONTH:TEXT, INTERVAL DAY TO SECOND:TEXT, NCHAR:TEXT, NVARCHAR2:TEXT,
 FLOAT:NUMERIC, BINARY_FLOAT:NUMERIC, BINARY_DOUBLE:NUMERIC, ROWID:OBJECT, UROWID:OBJECT, JSON:OBJECT', '0', CURRENT_TIMESTAMP) ON DUPLICATE KEY update `config_key`=`config_key`;

insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_column_group', 'OB_MYSQL', 'true', '4.3.0', CURRENT_TIMESTAMP)  ON DUPLICATE KEY update `config_key`=`config_key`;
insert into `odc_version_diff_config`(`config_key`,`db_mode`,`config_value`,`min_version`,`gmt_create`) values('support_column_group', 'OB_ORACLE', 'true', '4.3.0', CURRENT_TIMESTAMP)  ON DUPLICATE KEY update `config_key`=`config_key`;
