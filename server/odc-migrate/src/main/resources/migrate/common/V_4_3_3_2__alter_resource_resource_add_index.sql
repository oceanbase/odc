--
-- Add constraint (resource_type, status) to `resource_resource` table
--
alter table `resource_resource` add index `type_status`(`resource_type`, `status`);