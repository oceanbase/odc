--
-- Add column `project_id` to `connect_connection` table
--
alter table `connect_connection` add column `project_id` bigint(20) DEFAULT NULL COMMENT 'reference to collaboration_project.id, null means this connection does not belong to any project';
