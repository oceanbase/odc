???prompt PL/SQL Developer Export User Objects for user LEGEND@ORCL
prompt Created by Administrator on 2023年6月20日
set define off
spool all.log

prompt
prompt Creating table DEPTMENT
prompt =======================
prompt
delimiter ;
create table DEPTMENT
(
  deptno NUMBER(2),
  dname  VARCHAR2(14),
  loc    VARCHAR2(13)
)
; -- single comment

prompt
prompt Creating table EMPLOYEE
prompt =======================
prompt
create table EMPLOYEE
(
  empno                                 NUMBER(4),
  ename                                 VARCHAR2(10),
  job                                   VARCHAR2(9),
  mgr                                   NUMBER(4),
  hiredate                              DATE,
  sal                                   NUMBER(7,2),
  comm                                  NUMBER(7,2),
  deptno                                NUMBER(2),
  char_column                           CHAR(10) default 'c',
  /*
    multi comments
    multi comments
    multi comments
    multi comments
  */
  nchar_column                          NCHAR(120),
  varchar_column                        VARCHAR2(120) default 'asd中文',
  integer_column                        INTEGER,
  blob_column                           BLOB,
  clob_column                           CLOB,
  timestamp_column                      TIMESTAMP(6) default TO_TIMESTAMP('2022-09-22 17:29:00','YYYY-MM-DD HH24:MI:SS.FF'),
  timestamp_with_time_zone_column       TIMESTAMP(6) WITH TIME ZONE default TO_TIMESTAMP_TZ('1983-02-01 14:28:30.000552 +09:00','YYYY-MM-DD HH24:MI:SS.FF TZH:TZM'),
  timestamp_with_local_time_zone_column TIMESTAMP(6) WITH LOCAL TIME ZONE default TO_TIMESTAMP('2035-01-14 20:36:26.000948','YYYY-MM-DD HH24:MI:SS.FF'),
  raw_column                            RAW(200),
  interval_day_to_second_column         INTERVAL DAY(1) TO SECOND(6),
  interval_year_to_month_column         INTERVAL YEAR(2) TO MONTH default '+10-02'
)
;

prompt
prompt Creating table EXPORT_TEST
prompt ==========================
prompt
create table EXPORT_TEST
(
  id   INTEGER,
  name VARCHAR2(64)
)
;

prompt
prompt Creating table TEST01
prompt =====================
prompt
create table TEST01
(
  col1 VARCHAR2(100),
  col2 VARCHAR2(100),
  col3 VARCHAR2(100),
  col4 VARCHAR2(100),
  col5 VARCHAR2(100)
)
;

prompt
prompt Creating table spec.al
prompt ======================
prompt
create table spec.al
(
  id INTEGER
)
;

prompt
prompt Creating view V1
prompt ================
prompt
create or replace force view v1 as
select "ID","NAME" from export_test;

prompt
prompt Creating type EMPLOYEE_TYPE
prompt ===========================
prompt
CREATE OR REPLACE TYPE EMPLOYEE_TYPE as OBJECT
                        (
                            "EMPNO" NUMBER(38),
                            "ENAME" VARCHAR2(100),
                            "JOB" VARCHAR2(100)
                        )
/

prompt
prompt Creating type EMPLOYEE_TYPECLOB
prompt ===============================
prompt
CREATE OR REPLACE TYPE EMPLOYEE_TYPECLOB as OBJECT (
  EMPNO NUMBER(38),
  CLOB_COLUMN CLOB
)
/

prompt
prompt Creating function MY11_FUNCTION
prompt ===============================
prompt
CREATE OR REPLACE FUNCTION my11_function(param1 IN VARCHAR2, param2 IN NUMBER)
RETURN VARCHAR2
AS LANGUAGE JAVA NAME 'my.package.MyClass.myMethod(java.lang.String, int) return java.lang.String';
/

prompt
prompt Creating function MY_FUNCTION
prompt =============================
prompt
CREATE OR REPLACE FUNCTION my_function(param1 IN VARCHAR2, param2 IN NUMBER)
RETURN VARCHAR2
AS LANGUAGE JAVA NAME 'my.package.MyClass.myMethod(java.lang.String, int) return java.lang.String';
/

prompt
prompt Creating procedure DEMOC
prompt ========================
prompt
create or replace procedure democ(src out SYS_REFCURSOR) is
    chinese VARCHAR2(100);

    BEGIN
        open src for select '中文' as name from dual;
        FETCH src into chinese;
        LOOP
            DBMS_OUTPUT.PUT_LINE(chinese);
            FETCH src into chinese;
            EXIT when src%NOTFOUND;
        END LOOP;

        if src%ISOPEN THEN
            -- dbms_output.PUT_LINE('游标是否开启：'||src%NOTFOUND);
            CLOSE src; -- 关闭游标
            dbms_output.PUT_LINE('demo的游标关闭成功！');
        end if;
    END;
/

prompt
prompt Creating procedure RECORDBLOBPL
prompt ===============================
prompt
create or replace procedure RECORDBLOBPL(re out EMPLOYEE_TYPECLOB) is
begin
  select EMPNO,CLOB_COLUMN into re from EMPLOYEE where EMPNO = 7934;
  dbms_output.put_line(re.empno);
  dbms_output.put_line(DBMS_LOB.GETLENGTH(re.clob_column));
end;
/

prompt
prompt Creating procedure RECORD_TYPE
prompt ==============================
prompt
create or replace procedure record_type(src out SYS_REFCURSOR) IS
    TYPE recordtype is record (
            var_ename varchar(100),
            var_mgr number(38, 0),
            var_hiredate date,
            var_char char(10),
            var_nchar nchar(120),
            var_integer integer,
            var_blob blob,
            var_clob clob,
            var_timestamp timestamp(6),
            var_TIMESTAMP_WITH_TIME_ZONE timestamp(6) with time zone,
            var_TIMESTAMP_WITH_LOCAL_TIME_ZONE TIMESTAMP(6) WITH LOCAL TIME ZONE,
            var_raw raw(200),
            var_INTERVAL_DAY_TO_SECOND_COLUMN INTERVAL DAY(1) TO SECOND(6),
            var_INTERVAL_YEAR_TO_MONTH_COLUMN INTERVAL YEAR(2) TO MONTH
        );
    var_record recordtype;
    begin
        open src for SELECT
            ENAME, MGR, HIREDATE, CHAR_COLUMN, NCHAR_COLUMN, INTEGER_COLUMN, BLOB_COLUMN, CLOB_COLUMN, TIMESTAMP_COLUMN, TIMESTAMP_WITH_TIME_ZONE_COLUMN, TIMESTAMP_WITH_LOCAL_TIME_ZONE_COLUMN, RAW_COLUMN, INTERVAL_DAY_TO_SECOND_COLUMN, INTERVAL_YEAR_TO_MONTH_COLUMN
                from employee;
        LOOP
            -- DBMS_OUTPUT.PUT_LINE(var_record.var_ename);
            FETCH src into var_record;
            EXIT when src%NOTFOUND;
        END LOOP;

        if src%ISOPEN THEN
            -- dbms_output.PUT_LINE('游标是否开启：'||src%NOTFOUND);
            CLOSE src; -- 关闭游标
            dbms_output.PUT_LINE('normal游标关闭成功！');
        end if;
    end;
/

prompt
prompt Creating procedure ROW_TYPE
prompt ===========================
prompt
create or replace procedure row_type(src out SYS_REFCURSOR) IS
    var_row employee%rowtype;
    V_SQL varchar2(2000);

    BEGIN
        V_SQL := 'select * from
                    (select ROWNUM rn, t.* from
                        (select ename,sal,deptno from employee ORDER by sal desc) t where ROWNUM <= :1);
                        where rn >= :2;
                        ';
        open src for V_SQL USING 10,10;
        LOOP
            fetch src into var_row;
            DBMS_OUTPUT.PUT_LINE(var_row.ename);
            EXIT when src%NOTFOUND;
        END LOOP;
        close src;
    END;
/

prompt
prompt Creating procedure SELF_TYPE
prompt ============================
prompt
create or replace procedure self_type(src out SYS_REFCURSOR) IS
    employeetype EMPLOYEE_TYPE;
    BEGIN
        OPEN src FOR SELECT empno,ename,job FROM employee;
        FETCH src INTO employeetype;
        while src%found
        loop
            dbms_output.put_line('雇员编号: '||employeetype.empno);
            dbms_output.put_line('；雇员名字: '||employeetype.ename);
            dbms_output.put_line('；雇员职务: '||employeetype.job);
            FETCH src INTO employeetype;
        END loop;
        close src;
    END;
/

prompt
prompt Creating package TEST_PACKAGE_FUNC_PROC
prompt =======================================
prompt
create or replace package Test_Package_Func_Proc
  as
   function  test_func  (in_val in number) return number;
   procedure test_proc (in_val in number);
  end;
  /

  create or replace package body Test_Package_Func_Proc
  as
  function test_func (in_val in number) return number
  is
  begin
  return in_val;
  exception
  when others then
  RAISE;
  end;
  procedure test_proc  (in_val in number)
  is
  begin
  dbms_output.put_line (in_val);
  exception
  when others then
  RAISE;
  end;
  end;
/

/*
  multi comments
  multi comments
  multi comments
  multi comments
*/

prompt
prompt Creating package TEST_PACKAGE_FUNC_PROC
prompt =======================================
prompt
select q'[It's a bird.]' from dual;

prompt Done
spool off
set define on
