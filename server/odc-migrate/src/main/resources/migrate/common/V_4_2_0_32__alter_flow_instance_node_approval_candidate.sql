alter table `flow_instance_node_approval_candidate` add column `resource_role_identifier` varchar(32) DEFAULT NULL comment 'resource role identifiers in format of resource_id:resource_role_id';

alter table `flow_instance` modify column `flow_config_id` bigint(20) default null comment '[Deprecated from ODC 4.2.0]Process config id, references flow_config(id)';