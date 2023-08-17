CREATE OR REPLACE PACKAGE BODY SA_POLICY_ADMIN AS
  
  PROCEDURE APPLY_TABLE_POLICY (
    policy_name       IN VARCHAR,
    schema_name       IN VARCHAR,
    table_name        IN VARCHAR,
    table_options     IN VARCHAR := NULL,
    label_function    IN VARCHAR := NULL,
    predicate         IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
    ret := ols_table_policy_apply(policy_name, schema_name, table_name, table_options, label_function, predicate);
  END;

  PROCEDURE DISABLE_TABLE_POLICY (
    policy_name      IN VARCHAR,
    schema_name      IN VARCHAR,
    table_name       IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_table_policy_disable(policy_name, schema_name, table_name);
  END;

  PROCEDURE ENABLE_TABLE_POLICY (
    policy_name     IN VARCHAR,
    schema_name     IN VARCHAR,
    table_name      IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_table_policy_enable(policy_name, schema_name, table_name);
  END;

  PROCEDURE REMOVE_TABLE_POLICY (
    policy_name        IN VARCHAR,
    schema_name        IN VARCHAR,
    table_name         IN VARCHAR,
    drop_column        IN BOOLEAN := FALSE
  ) IS
    ret INT;
  BEGIN
    ret := ols_table_policy_remove(policy_name, schema_name, table_name, drop_column);
  END;
/*
  PROCEDURE ALTER_SCHEMA_POLICY (
    policy_name         IN VARCHAR,
    schema_name         IN VARCHAR,
    default_options     IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE APPLY_SCHEMA_POLICY (
    policy_name        IN VARCHAR,
    schema_name        IN VARCHAR,
    default_options    IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE DISABLE_SCHEMA_POLICY (
    policy_name    IN VARCHAR,
    schema_name    IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE ENABLE_SCHEMA_POLICY (
    policy_name    IN VARCHAR,
    schema_name    IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE POLICY_SUBSCRIBE(
    policy_name     IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE POLICY_UNSUBSCRIBE(
    policy_name  IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE REMOVE_SCHEMA_POLICY (
    policy_name     IN VARCHAR,
    schema_name     IN VARCHAR,
    drop_column     IN BOOLEAN := FALSE
  ) IS
    ret INT;
  BEGIN
  END;
*/
END SA_POLICY_ADMIN;
//
