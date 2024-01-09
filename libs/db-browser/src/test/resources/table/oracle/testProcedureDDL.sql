CREATE OR REPLACE PROCEDURE PROC_PARAMS_ACCESSOR (
    p_in IN NUMBER DEFAULT 100,
    p_in_out IN OUT VARCHAR2,
    p_out OUT DATE,
    p_result OUT BOOLEAN
)
IS
    v_temp VARCHAR2(100);
BEGIN
    v_temp := 'Hello ' || p_in_out;
    p_in_out := v_temp;
    p_out := SYSDATE;
    p_result := TRUE;
END;
/


CREATE OR REPLACE PROCEDURE PROC_INVALID_ACCESSOR
IS
BEGIN
    SELECT * FROM non_existent_table;
END;
