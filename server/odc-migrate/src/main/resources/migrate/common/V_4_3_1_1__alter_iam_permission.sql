--
-- Add columns in `iam_permission` table for table-level permission management
--
alter table `iam_permission` add column `resource_type` varchar(64) default null comment 'Refer to ResourceType, optional values: ODC_DATABASE, ODC_TABLE, ODC_COLUMN, etc.';
alter table `iam_permission` add column `resource_id` bigint(20) default null comment 'ID of the resource';
alter table `iam_permission` add key `idx_iam_permission_resource_type_id` (resource_type, resource_id);
update `iam_permission` set `resource_type` = 'ODC_DATABASE', `resource_id` = substring( `resource_identifier` from 14 ) where `resource_identifier` like 'ODC_DATABASE:%'  and `action` in ('query', 'change', 'export');
