CREATE OR REPLACE FUNCTION FUNC_ACCESSOR(
    p1 in NUMBER,
    p2 in NUMBER)
return NUMBER as
v1 int;
begin
return p1+p2;
end;
/

CREATE OR REPLACE FUNCTION FUNC_INVALIDE_ACCESSOR(
p1 in NUMBER,
p2 in NUMBER)
return NUMBER as
v1 int;
begin
select 1+1 into v_result from dual11;
return v_result;
end;
/

CREATE OR REPLACE FUNCTION FUNC_DETAIL_ACCESSOR (
    p_in IN NUMBER,
    p_in_out IN OUT VARCHAR2,
    p_out OUT DATE
)
RETURN BOOLEAN
IS
    v_temp VARCHAR2(100);
	type cur_emp is ref cursor;
BEGIN
    v_temp := 'Hello ' || p_in_out;
    p_in_out := v_temp;
    p_out := SYSDATE;
    RETURN TRUE;
END;
/

CREATE OR REPLACE FUNCTION FUNC_NO_PARAM_ACCESSOR
RETURN VARCHAR2
IS
    result VARCHAR2(100);
BEGIN
    result := 'Hello';
    RETURN result;
END;