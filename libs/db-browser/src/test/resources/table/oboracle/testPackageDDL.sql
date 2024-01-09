create or replace package t_package is
    v1 number;
    type cur_emp is ref cursor;
    procedure append_proc(p1 in out varchar2, p2 number);
    function append_fun(p2 out varchar2) return varchar2;
end;
/

create or replace package body t_package is
    v_t varchar2(30);
    function private_fun(p1 IN OUT NOCOPY varchar2) return varchar2 is
    begin
    return v_t;
    end;
    procedure append_proc(p2 number) is
    begin
    return v_t;
    end;
end;
/

create or replace package INVALID_PKG is
    v1 number;
    type cur_emp is ref cursor;
    procedure append_proc(p1 in out varchar2, p2 number);
    function append_fun(p2 out varchar2) return varchar2;
end;
/

create or replace package body INVALID_PKG is
    v_t varchar2(30);
    function private_fun(p1 IN OUT NOCOPY varchar2) return varchar2 is
    begin
    return v_t;
    end;
    procedure append_proc(p2 number) is
    begin
    return v_t;
    end;
end;
/

CREATE OR REPLACE PACKAGE PAC_ACCESSOR
IS
    v1 number;
    type cur_emp is ref cursor;
		PROCEDURE PROC_PARAMS_ACCESSOR (p_in IN NUMBER DEFAULT 100, p_in_out IN OUT VARCHAR2, p_out OUT DATE, p_result OUT BOOLEAN);
		FUNCTION FUNC_DETAIL_ACCESSOR (p_in IN NUMBER, p_in_out IN OUT VARCHAR2, p_out OUT DATE) RETURN NUMBER;
		FUNCTION FUNC_IN_PRO RETURN NUMBER;
END PAC_ACCESSOR;
/

CREATE OR REPLACE PACKAGE BODY PAC_ACCESSOR
AS
PROCEDURE PROC_PARAMS_ACCESSOR (p_in IN NUMBER DEFAULT 100, p_in_out IN OUT VARCHAR2, p_out OUT DATE, p_result OUT BOOLEAN)
IS
  total NUMBER;
BEGIN
	DBMS_OUTPUT.PUT_LINE('db-browser test');
END;

FUNCTION FUNC_DETAIL_ACCESSOR (p_in IN NUMBER, p_in_out IN OUT VARCHAR2, p_out OUT DATE)
RETURN NUMBER
IS
  total NUMBER;
BEGIN
    RETURN total;
END;

FUNCTION FUNC_IN_PRO
RETURN NUMBER
IS
  total NUMBER;
BEGIN
	RETURN total;
END;
END PAC_ACCESSOR;
