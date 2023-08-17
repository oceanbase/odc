alter table `flow_instance` add column `project_id` bigint(20) default null comment 'Project id, references collaboration_project(id)';
