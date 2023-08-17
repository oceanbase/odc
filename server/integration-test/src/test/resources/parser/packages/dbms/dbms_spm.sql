CREATE OR REPLACE PACKAGE DBMS_SPM AS
  FUNCTION LOAD_PLANS_FROM_CURSOR_CACHE (
     sql_id               IN VARCHAR
     ,plan_hash_value     IN NUMBER   := NULL
     ,fixed               IN VARCHAR  := 'NO'
     ,enabled             IN VARCHAR  := 'YES'
   )
   RETURN PLS_INTEGER;

  FUNCTION ALTER_SQL_PLAN_BASELINE ( 
     sql_handle          IN VARCHAR := NULL
     , plan_name         IN VARCHAR := NULL
     , attribute_name    IN VARCHAR
     , attribute_value   IN VARCHAR
  )
  RETURN PLS_INTEGER;

  FUNCTION DROP_SQL_PLAN_BASELINE (
     sql_handle       IN VARCHAR := NULL
     , plan_name      IN VARCHAR := NULL
  )
  RETURN PLS_INTEGER;
END DBMS_SPM;
//
