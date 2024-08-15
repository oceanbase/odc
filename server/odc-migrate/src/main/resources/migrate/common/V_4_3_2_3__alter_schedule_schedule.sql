--
-- Add column `name` to `schedule_schedule` table
--
alter table `schedule_schedule` add column `name` varchar(255) DEFAULT NULL COMMENT '任务名称';