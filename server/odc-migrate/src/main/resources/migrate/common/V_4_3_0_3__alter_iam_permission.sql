alter table `iam_permission` add column `resource_type` varchar(64) default null comment 'ResourceType: ODC_DATABASE,ODC_TABLE,ODC_COLUMN ';
alter table `iam_permission` add column `resource_id` bigint(20) default null comment 'resource id';
alter table `iam_permission` add key `idx_resource_type_and_id` (resource_type,resource_id);
update `iam_permission` set `resource_type` = 'ODC_DATABASE',`resource_id` = SUBSTRING( `resource_identifier` FROM 14 ) where `resource_identifier` LIKE 'ODC_DATABASE:%'  and `action` in ('query', 'change', 'export');