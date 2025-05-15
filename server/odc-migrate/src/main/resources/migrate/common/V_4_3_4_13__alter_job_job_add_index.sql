--
-- Add constraint (status, executor_destroyed_time) to `job_job` table
--
alter table `job_job` add index `idx_job_job_status_destroy_time`(`status`, `executor_destroyed_time`);