alter table `notification_event` add index `notification_event_status_time`(`status`, `trigger_time`);

alter table `notification_message` add index `idx_status_retry_times_max_retry_times`(`status`, `retry_times`, `max_retry_times`);