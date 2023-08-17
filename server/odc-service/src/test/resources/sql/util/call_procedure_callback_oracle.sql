CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.pldebug.util.CallProcedureCallBackTest.TEST_CASE_1} (
  N1 IN VARCHAR2,
  P1 IN INT,
  N2 IN INT,
  N3 IN VARCHAR2,
  P2 IN INT,
  P3 IN OUT INT) IS
BEGIN
  P3 := P1 + P2;
END;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.pldebug.util.CallProcedureCallBackTest.TEST_CASE_3} (
  P1 IN INT,
  P2 IN INT,
  P3 IN OUT INT) IS
BEGIN
  P3 := P1 + P2;
END;
$$

CREATE OR REPLACE PROCEDURE ${const:com.oceanbase.odc.service.pldebug.util.CallProcedureCallBackTest.TEST_CASE_4} (
  LINE OUT VARCHAR2,
  STATUS OUT INT) IS
BEGIN
  DBMS_OUTPUT.GET_LINE(LINE, STATUS);
END ${const:com.oceanbase.odc.service.pldebug.util.CallProcedureCallBackTest.TEST_CASE_4};
