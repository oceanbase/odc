alter table `task_task` add column `risk_level_id` bigint default null comment 'reference to regulation_risklevel.id';
alter table `task_task` add column `database_id` bigint default null comment 'reference to connect_database.id'
