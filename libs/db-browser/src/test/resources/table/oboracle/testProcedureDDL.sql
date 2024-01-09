create or replace procedure PROCEDURE_TEST(p1 in VARCHAR) is
begin
null;
end PROCEDURE_TEST;
/

create or replace procedure INVALID_PROCEDURE_TEST(
val in varchar2 ) is
begin
select 1, val from invalid_dual;
end INVALID_PROCEDURE_TEST;
/

create or replace procedure PROCEDURE_DETAIL_TEST(p1 in int, p2 in varchar2) IS v1 number;
begin
return;
end;