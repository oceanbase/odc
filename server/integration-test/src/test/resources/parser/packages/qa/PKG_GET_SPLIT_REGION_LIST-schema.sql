DELIMITER $$
CREATE OR REPLACE PACKAGE BODY "PKG_GET_SPLIT_REGION_LIST" is

  -- Function and procedure implementations
  FUNCTION get(v_param_name in VARCHAR2, v_region in VARCHAR2)
    return REGION_TYPE is
    V_REGION_LIST REGION_TYPE;

    V_REGION_STR VARCHAR2(1024);
    V_GET_FLAG   NUMBER(4) := 0;
    V_TMP_REGION VARCHAR2(12);

    V_POS_BEGIN NUMBER(4) := 0;
    V_POS_END   NUMBER(4) := 0;
    V_INDEX     NUMBER(4) := 0;
  begin
    --split region and part
    for cl4 in (SELECT *
                  FROM GLOBAL_CONFIG
                 WHERE LOWER(PARAM_NAME) like
                       v_param_name || '_' || v_region || '_part%') loop
      V_REGION_STR := V_REGION_STR || ',' || cl4.param_value;
      V_GET_FLAG   := 1;
	  dbms_output.put_line('1');
    end loop;
    --split region
    IF V_GET_FLAG = 0 THEN
      for cl1 in (SELECT *
                    FROM GLOBAL_CONFIG
                   WHERE LOWER(PARAM_NAME) = v_param_name || '_' || v_region) loop
        V_REGION_STR := V_REGION_STR || ',' || cl1.param_value;
        V_GET_FLAG   := 1;
		 dbms_output.put_line('2');
      end loop;
    end if;
    --split part
    IF V_GET_FLAG = 0 THEN
      for cl1 in (SELECT *
                    FROM GLOBAL_CONFIG
                   WHERE LOWER(PARAM_NAME) like v_param_name || '_part%') loop
        V_REGION_STR := V_REGION_STR || ',' || cl1.param_value;
        V_GET_FLAG   := 1;
		 dbms_output.put_line('3');
      end loop;
    end if;
    --no split
    IF V_GET_FLAG = 0 THEN
      for cl1 in (SELECT *
                    FROM GLOBAL_CONFIG
                   WHERE LOWER(PARAM_NAME) = v_param_name) loop
        V_REGION_STR := V_REGION_STR || ',' || cl1.param_value;
        V_GET_FLAG   := 1;
		 dbms_output.put_line('4');
      end loop;
    end if;

    if V_REGION_STR is null then
	 dbms_output.put_line('5');
      return V_REGION_LIST;
    end if;
    <<Cl_SPLIT>>
    V_POS_END := INSTR(V_REGION_STR, ',', V_POS_BEGIN + 1);
    IF (V_POS_END > 0) THEN
      V_TMP_REGION := SUBSTR(V_REGION_STR,
                             V_POS_BEGIN + 1,
                             V_POS_END - V_POS_BEGIN - 1);
      IF (V_TMP_REGION IS NOT NULL) THEN
        V_REGION_LIST(V_INDEX) := V_TMP_REGION;
        V_INDEX := V_INDEX + 1;
 dbms_output.put_line('6');
      END IF;
      V_POS_BEGIN := V_POS_END;
      GOTO Cl_SPLIT;
    ELSE
      V_REGION_LIST(V_INDEX) := SUBSTR(V_REGION_STR, V_POS_BEGIN + 1);
	   dbms_output.put_line('7');
    END IF;
    return V_REGION_LIST;
  end;
begin
  -- Initialization
  null;
   dbms_output.put_line('8');
end pkg_get_split_region_list;
$$