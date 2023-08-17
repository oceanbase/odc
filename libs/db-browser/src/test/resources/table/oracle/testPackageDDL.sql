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