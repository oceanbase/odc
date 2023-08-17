-- schema history table create ddl template
CREATE TABLE IF NOT EXISTS `${schema_history}` (
 `install_rank` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'install rank',
 `version` VARCHAR(64) DEFAULT NULL COMMENT 'version',
 `description` VARCHAR(64) DEFAULT NULL COMMENT 'description',
 `type` VARCHAR(64) DEFAULT NULL COMMENT 'type, optional values: SQL/JDBC',
 `script` VARCHAR(64) DEFAULT NULL COMMENT 'script name, sql file name or java class name',
 `checksum` VARCHAR(64) DEFAULT NULL COMMENT 'sha1 checksum',
 `installed_by` VARCHAR(64) NULL DEFAULT NULL COMMENT 'installed by user',
 `installed_on` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'installed timestamp',
 `execution_millis` BIGINT NOT NULL DEFAULT 0 COMMENT 'execution duration milliseconds',
 `success` BOOLEAN  NOT NULL DEFAULT 0 COMMENT 'is success: 1 success, 0 failed',
 PRIMARY KEY (`install_rank`)
 ) ;

alter table ${schema_history} modify column `script` varchar(128) DEFAULT NULL COMMENT 'script name, sql file name or java class name';
update ${schema_history} set script = replace(script, 'com.alipay.odc.metadb.migrate', 'com.oceanbase.odc.migrate.jdbc.common') WHERE script like 'com.alipay.odc.metadb.migrate%' and type='JDBC';
update ${schema_history} set script = replace(script, 'com.alipay.odc.metadb.alipay', 'com.oceanbase.odc.migrate.jdbc.web') WHERE script like 'com.alipay.odc.metadb.alipay%' and type='JDBC';
update ${schema_history} set script = replace(script, 'com.alipay.odc.metadb.clientmode', 'com.oceanbase.odc.migrate.jdbc.desktop') WHERE script like 'com.alipay.odc.metadb.clientmode%' and type='JDBC';
