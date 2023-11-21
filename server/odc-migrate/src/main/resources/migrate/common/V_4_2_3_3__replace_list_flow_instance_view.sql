create or replace view `flow_instance_approval_view` as
SELECT
	  fai.id,
    fai.flow_instance_id,
    faci.resource_role_identifier,
    fai.status as approval_status
FROM `flow_instance_node_approval` fai
INNER JOIN `flow_instance_node_approval_candidate` faci
ON fai.id=faci.approval_instance_id;