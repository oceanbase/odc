CREATE OR REPLACE FUNCTION ${const:com.oceanbase.odc.service.pldebug.util.OBOracleCallFunctionCallBackTest.TEST_CASE_1} (
  P1 INT,
  P2 INT,
  N1 INT,
  N2 VARCHAR2,
  N3 VARCHAR2) RETURN INT AS V_RESULT INT;
BEGIN
  RETURN P1+P2;
END;
$$

CREATE OR REPLACE FUNCTION ${const:com.oceanbase.odc.service.pldebug.util.OBOracleCallFunctionCallBackTest.TEST_CASE_2} (
  P1 INT,
  P2 INT,
  TOTAL OUT INT) RETURN INT AS V_RESULT INT;
BEGIN
  TOTAL:=P1+P2;
  RETURN TOTAL;
END;
$$