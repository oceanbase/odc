-- modify column flow_instance_id can be null
alter table `partitionplan`
modify column `flow_instance_id` bigint(20) null
comment 'Related flow instance id, reference flow_instance(id)';