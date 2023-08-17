create or replace view `list_flow_instance_view` as
select
	flow_instance.*,
 	task_info.task_type
from (
  select
  	nt.flow_instance_id,
  	nt.task_type
	from `flow_instance_node_task` nt
	where nt.task_type in ('EXPORT', 'IMPORT', 'MOCKDATA', 'ASYNC', 'SHADOWTABLE_SYNC', 'PARTITION_PLAN', 'ONLINE_SCHEMA_CHANGE', 'ALTER_SCHEDULE', 'EXPORT_RESULT_SET')
    group by nt.flow_instance_id
)
as task_info
inner join `flow_instance`
on flow_instance.id = task_info.flow_instance_id;

create or replace view `flow_instance_approval_view` as
SELECT
	  fai.id,
    fai.flow_instance_id,
    faci.resource_role_identifier,
    fai.status as approval_status
FROM `flow_instance_node_approval` fai
INNER JOIN `flow_instance_node_approval_candidate` faci
ON fai.id=faci.approval_instance_id
where fai.status in ('EXECUTING', 'WAIT_FOR_CONFIRM');