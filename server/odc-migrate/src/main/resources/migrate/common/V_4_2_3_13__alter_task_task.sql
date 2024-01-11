alter table `task_task` add column `job_id` bigint DEFAULT NULL COMMENT 'job id, references job_job(id)';
alter table `task_task` add unique `uk_task_task_job_id` (`job_id`);