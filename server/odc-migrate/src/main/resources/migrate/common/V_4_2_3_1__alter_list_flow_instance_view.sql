--
-- Alter view `list_flow_instance_view` to support new task types
--
drop view if exists list_flow_instance_view;
create or replace view list_flow_instance_view as
select
  flow_instance.*,
  task_info.task_type
from
  (
    select
      nt.flow_instance_id,
      nt.task_type
    from
      flow_instance_node_task nt
    group by
      nt.flow_instance_id, nt.task_type
  ) as task_info
  inner join flow_instance on flow_instance.id = task_info.flow_instance_id;
