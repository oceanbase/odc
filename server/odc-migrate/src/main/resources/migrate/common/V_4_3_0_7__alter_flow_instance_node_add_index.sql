alter table `flow_instance_node_task` add index `idx_flow_instance_node_task_instance_id` (`flow_instance_id`);
alter table `flow_instance_node_approval` add index `idx_flow_instance_node_approval_instance_id` (`flow_instance_id`);
alter table `flow_instance_node_gateway` add index `idx_flow_instance_node_gateway_instance_id` (`flow_instance_id`);

