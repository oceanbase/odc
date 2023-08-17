-- Add index for table `flow_instance`
create index if not exists flow_instance_idx_organization_id_creator_id on flow_instance(organization_id,creator_id);
