create or replace procedure ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_1} (
  p1 IN INT,
  p2 IN INT,
  p3 OUT INT
) is
begin
  p3 := p1 + p2;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_2} (
  n1 IN VARCHAR2,
  p1 IN INT,
  n2 IN INT,
  n3 IN VARCHAR2,
  p2 IN INT,
  p3 IN OUT INT) IS
BEGIN
  p3 := p1 + p2;
END;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_3} (
  p1 IN INT,
  p2 IN INT,
  p3 IN OUT INT) IS
BEGIN
  p3 := p1 + p2;
END;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_4} (
  LINE OUT VARCHAR2,
  STATUS OUT INT) IS
BEGIN
  DBMS_OUTPUT.GET_LINE(LINE, STATUS);
END ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_4};
$$

create or replace PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_5} (
  p1 in varchar2,
  p2 out integer,
  p3 in OUT DATE
) as
  var varchar2(64) := 'hello,world';
begin
  dbms_output.put_line(p1 || ' -> ' || p3);
  p2 := 420;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_VARCHAR2_1} (
  p1 in VARCHAR2,
  p2 out VARCHAR2,
  p3 in out VARCHAR2
)
as
	v1 varchar2(64) := 'hello,world';
begin
	p2 := 'YIMING';
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_VARCHAR_1} (
  p1 in VARCHAR,
  p2 out VARCHAR,
  p3 in out VARCHAR
)
as
	v1 varchar(64) := 'hello,world';
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 'YIMING';
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_CHAR_1} (
  p1 in CHAR,
  p2 out CHAR,
  p3 in out CHAR
)
as
	v1 CHAR(20) := 'hello,world';
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 'YIMING';
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_NUMBER_1} (
  p1 in NUMBER,
  p2 out NUMBER,
  p3 in out NUMBER
)
as
	v1 NUMBER(10,5) := 347.520;
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 100.001;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_INT_1} (
  p1 in INT,
  p2 out INT,
  p3 in out INT
)
as
	v1 INT := 347;
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 101;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_BINARY_INTEGER_1} (
  p1 in BINARY_INTEGER,
  p2 out BINARY_INTEGER,
  p3 in out BINARY_INTEGER
)
as
	v1 BINARY_INTEGER := 347;
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 101;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_INTEGER_1} (
  p1 in INTEGER,
  p2 out INTEGER,
  p3 in out INTEGER
)
as
	v1 INTEGER := 347;
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 101;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_DATE_1} (
  p1 in DATE,
  p2 out DATE,
  p3 in out DATE
)
as
	v1 DATE := sysdate;
begin
  dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := TO_DATE('2020-12-12', 'yyyy-mm-dd');
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_BOOLEAN_1} (
  p1 in BOOLEAN,
  p2 out BOOLEAN,
  p3 in out BOOLEAN
)
as
begin
  IF p1 THEN
	  dbms_output.put_line('TRUE');
	ELSE
	  dbms_output.put_line('FALSE');
	END IF;
	p2 := FALSE;
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_CLOB_1} (
  p1 in CLOB,
  p2 out CLOB,
  p3 in out CLOB
)
as
	v1 CLOB := 'odc';
begin
	dbms_output.put_line(p1);
	dbms_output.put_line(p3);
	p2 := 'YIMING';
end;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_BLOB_1} (
  p1 in BLOB,
  p2 out VARCHAR2
)
as
  v1 BLOB := UTL_RAW.CAST_TO_RAW('odc');
BEGIN
  DBMS_OUTPUT.PUT_LINE(UTL_RAW.CAST_TO_VARCHAR2(p1));
  p2 := UTL_RAW.CAST_TO_VARCHAR2(p1);
END;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_OBJECT_1} (
  p1 in cutom_type,
  p2 out cutom_type,
  p3 in out cutom_type
)
as
	v1 cutom_type := cutom_type(101, 'ODC');
begin
	p2 := cutom_type(347, 'YIMING');
end;
$$
CREATE OR REPLACE TYPE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_OBJECT_CUTOM_TYPE} IS OBJECT (p1 integer, p2 varchar2(64));
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_ARRAY_1} (
  p1 in int_array,
  p2 out int_array,
  p3 in out int_array
)
as
	v1 int_array := int_array(1,2,3,4,5,6);
begin
	p2 := int_array(100,101,102,103);
end;
$$
CREATE TYPE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_ARRAY_INT_ARRAY} AS VARRAY(10) OF INT;
$$

create or replace PROCEDURE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_CASE_SYS_REFCURSOR_1} (
  p1 OUT SYS_REFCURSOR
)
is
begin
  open p1 FOR select * from A;
  dbms_output.put_line('Hello World!');
end;
$$
CREATE TABLE ${const:com.oceanbase.odc.service.db.util.OBOracleCallProcedureBlockCallBackTest.TEST_TABLE_A} (
  "A" VARCHAR2(120),
  "B" VARCHAR2(120)
) COMPRESS FOR ARCHIVE REPLICA_NUM = 1 BLOCK_SIZE = 16384 USE_BLOOM_FILTER = FALSE TABLET_SIZE = 134217728 PCTFREE = 0;
$$
