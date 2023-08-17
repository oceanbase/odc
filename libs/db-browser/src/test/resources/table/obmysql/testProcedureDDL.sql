create procedure procedure_test ( IN `id` int(45), IN `name` varchar(45)) begin
select id, name from dual;
end;
/

create procedure procedure_detail_test ( IN `p1` int(45), INOUT `p2` varchar(100)) begin
declare v2 varchar(100) default 'abc';
end;
/

create procedure procedure_without_parameters ()
begin
  select 1 from dual;
end;