-- add column schedule_changelog_id to relate to schedule_changelog.id
alter table `schedule_schedule` add column `latest_schedule_changelog_id` bigint(20) default null COMMENT 'latest ID of the change log';
-- add index on list all schedules
CREATE INDEX if not exists `idx_schedule_schedule_list_index` ON `schedule_schedule` (`organization_id`, `create_time`);

CREATE INDEX if not exists `idx_schedule_schedule_approve_join_index` ON `schedule_schedule` (`organization_id`, `latest_schedule_changelog_id`, `create_time`);

-- full scan table join
UPDATE schedule_schedule
  SET latest_schedule_changelog_id = (
      SELECT a.id
      FROM schedule_changelog a
      WHERE a.schedule_id = schedule_schedule.id
      AND a.create_time = (
          SELECT MAX(create_time)
          FROM schedule_changelog
          WHERE schedule_id = schedule_schedule.id
      )
  )
  WHERE EXISTS (
      SELECT 1
      FROM schedule_changelog
      WHERE schedule_id = schedule_schedule.id
  );