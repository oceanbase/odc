alter table `schedule_schedule` add column `project_id` bigint default null COMMENT '项目 ID';
alter table `schedule_schedule` add column `database_id` bigint default '-1' comment '数据库 ID' not null;
