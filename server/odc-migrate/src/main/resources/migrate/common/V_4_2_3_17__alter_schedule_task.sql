alter table `schedule_task` add column `job_id` bigint DEFAULT NULL COMMENT 'job id, references job_job(id)';
alter table `schedule_task` add index `uk_schedule_task_job_id` (`job_id`);