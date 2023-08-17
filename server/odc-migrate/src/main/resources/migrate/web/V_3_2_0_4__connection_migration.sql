---
--- v3.2.0
---

-- connection migration from odc_session_manager,odc_session_extended into connect_connection
-- `t3.id IS NULl` condition for avoid duplicate migration while execute this script multiple times
-- ON DUPLICATE KEY UPDATE `id`=`id` for avoid insert failure while user contains duplicated session_name exists
INSERT INTO connect_connection (
  id, creator_id, owner_id, `name`, dialect_type, config_url,
  `host`, `port`, cluster_name, tenant_name, username, `password`,
  default_schema, create_time, update_time, properties_json,
  cipher, salt, is_password_saved, query_timeout_seconds,
  sys_tenant_username, sys_tenant_password,
  visible_scope, organization_id
)
SELECT
  t1.id AS id, t1.user_id AS creator_id, t1.user_id AS owner_id, t1.session_name AS `name`, t1.db_mode AS dialect_type, t1.config_url AS config_url,
  t1.`host` AS `host`, t1.`port` AS `port`, t1.cluster AS cluster_name, t1.tenant AS tenant_name, t1.db_user AS username, t1.`password` AS `password`,
  t1.default_DBName AS default_schema, IFNULL(t1.gmt_create, CURRENT_TIMESTAMP) AS create_time, t1.gmt_modify AS update_time, t1.extend_info AS properties_json,
  t1.cipher AS cipher, t1.salt AS salt, t1.is_password_saved AS is_password_saved, IFNULL(t2.session_timeout, 10) AS query_timeout_seconds,
  t2.sys_user AS sys_tenant_username, t2.sys_user_password AS sys_tenant_password,
  'PRIVATE', 1 as organization_id
FROM odc_session_manager t1
  LEFT JOIN odc_session_extended t2 ON t1.id=t2.sid
  LEFT JOIN connect_connection t3 ON t1.id=t3.id
WHERE t1.user_id IS NOT NULL AND t1.`host` IS NOT NULL AND t1.session_name IS NOT NULL AND t1.db_mode IS NOT NULL AND t1.db_user IS NOT NULL
 AND t3.id IS NULL
 ON DUPLICATE KEY UPDATE `id`=`id`;