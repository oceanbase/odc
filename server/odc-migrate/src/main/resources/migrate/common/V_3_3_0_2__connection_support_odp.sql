---
--- v3.3.0
---

--
-- connection add odp sharding support
--
ALTER TABLE `connect_connection` ADD COLUMN `type` VARCHAR(64) NOT NULL default 'UNKNOWN'
  COMMENT '连接类型，可选值 OB_MYSQL/OB_ORACLE/CLOUD_OB_MYSQL/CLOUD_OB_ORACLE/ODP_SHARDING_OB_MYSQL'
  AFTER last_modifier_id;

--
-- migrate connection config
--

-- for direct/obproxy connection
update connect_connection set `type`='OB_MYSQL' where `type`='UNKNOWN' AND dialect_type='OB_MYSQL' AND `port` IS NOT NULL;
update connect_connection set `type`='OB_ORACLE' where `type`='UNKNOWN' AND dialect_type='OB_ORACLE' AND `port` IS NOT NULL;

-- for cloud connection
update connect_connection set `type`='CLOUD_OB_MYSQL' where `type`='UNKNOWN' AND dialect_type='OB_MYSQL' AND `port` IS NULL;
update connect_connection set `type`='CLOUD_OB_ORACLE' where `type`='UNKNOWN' AND dialect_type='OB_ORACLE' AND `port` IS NULL;

-- use SUBSTR+LOCATE due REGEXP_SUBSTR function not compatible from mysql to h2database
update connect_connection set `port` = SUBSTR( `host`, LOCATE(':', `host`) + 1), `host` = SUBSTR( `host`, 1, LOCATE(':', `host`) - 1)
   where `type` IN ('CLOUD_OB_MYSQL', 'OB_CLOUD_ORACLE') AND `host` like '%:%';
