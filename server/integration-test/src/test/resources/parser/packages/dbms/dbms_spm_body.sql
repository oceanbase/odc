CREATE OR REPLACE PACKAGE BODY DBMS_SPM AS
  FUNCTION LOAD_PLANS_FROM_CURSOR_CACHE(
    sql_id               IN VARCHAR
    , plan_hash_value    IN NUMBER   := NULL
    , fixed              IN VARCHAR  := 'NO'
    , enabled            IN VARCHAR  := 'YES')
  RETURN PLS_INTEGER AS
    ret PLS_INTEGER;
    bool_fixed PLS_INTEGER;
    bool_enabled PLS_INTEGER;
    err_invalid_arg EXCEPTION;
  BEGIN
    IF (UPPER(fixed) = 'NO') THEN
      bool_fixed := 0;
    ELSIF UPPER(fixed) = 'YES' THEN
      bool_fixed := 1;
    ELSE
      RAISE err_invalid_arg;
    END IF;

    IF (UPPER(enabled) = 'NO') THEN
      bool_enabled := 0;
    ELSIF (UPPER(enabled) = 'YES') THEN
      bool_enabled := 1;
    ELSE
      RAISE err_invalid_arg;
    END IF;

    ret := spm_load_plans_from_plan_cache(sql_id, plan_hash_value, bool_fixed, bool_enabled);
    RETURN ret;
  END;

  FUNCTION ALTER_SQL_PLAN_BASELINE ( 
     sql_handle          IN VARCHAR := NULL
     , plan_name         IN VARCHAR := NULL
     , attribute_name    IN VARCHAR
     , attribute_value   IN VARCHAR
  )
  RETURN PLS_INTEGER AS
    ret PLS_INTEGER;
  BEGIN
     ret := spm_alter_baseline(sql_handle, plan_name, attribute_name, attribute_value);
     RETURN ret;
  END;

  FUNCTION DROP_SQL_PLAN_BASELINE (
     sql_handle       IN VARCHAR := NULL
     , plan_name      IN VARCHAR := NULL
  )
  RETURN PLS_INTEGER AS
    ret PLS_INTEGER;
  BEGIN
    ret := spm_drop_baseline(sql_handle, plan_name);
    RETURN ret;
  END;
END DBMS_SPM;
//
