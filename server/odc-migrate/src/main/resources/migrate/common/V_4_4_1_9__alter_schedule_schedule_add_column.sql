-- add column is_inner to schedule_schedule
alter table `schedule_schedule`
add column `is_inner` tinyint(1) not null default 0
comment 'mark whether the entity is displayed externally';
