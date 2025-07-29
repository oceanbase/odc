-- add index on flow_instance_id
CREATE INDEX if not exists `idx_schedule_changelog_flow_instance_id` ON `schedule_changelog` (`flow_instance_id`);
-- add index on schedule_id
CREATE INDEX if not exists `idx_schedule_changelog_flow_schedule_id` ON `schedule_changelog` (`schedule_id`, `create_time`);

-- add index on status
CREATE INDEX if not exists `idx_schedule_changelog_status` ON `schedule_changelog` (`status`, `create_time`);
