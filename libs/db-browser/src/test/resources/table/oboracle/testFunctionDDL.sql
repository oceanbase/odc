CREATE OR REPLACE FUNCTION FUNC_TEST(
p1 in NUMBER,
p2 in NUMBER)
return NUMBER as
v1 int;
begin
return p1+p2;
end;
/

CREATE OR REPLACE FUNCTION INVALIDE_FUNC(
p1 in NUMBER,
p2 in NUMBER)
return NUMBER as
v1 int;
begin
select 1+1 into v_result from dual11;
return v_result;
end;
/

CREATE OR REPLACE FUNCTION FUNC_DETAIL_TEST(
v_input  number)
RETURN NUMBER IS
v1 number; v2 varchar2(100);
type cur_emp is ref cursor;
BEGIN
RETURN v1;
END;