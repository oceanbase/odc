CREATE OR REPLACE PACKAGE BODY SA_SYSDBA AS

  PROCEDURE ALTER_POLICY (
    policy_name       IN  VARCHAR,
    default_options   IN  VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
    ret := ols_policy_alter(policy_name, default_options);
  END;

  PROCEDURE CREATE_POLICY (
    policy_name       IN VARCHAR,
    column_name       IN VARCHAR := NULL,
    default_options   IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
    ret := ols_policy_create(policy_name, column_name, default_options);
  END;

  PROCEDURE DISABLE_POLICY (
    policy_name IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_policy_disable(policy_name);
  END;

  PROCEDURE DROP_POLICY ( 
    policy_name IN VARCHAR,
    drop_column IN BOOLEAN := FALSE
  ) IS
    ret INT;
  BEGIN
    ret := ols_policy_drop(policy_name, drop_column);
  END;

  PROCEDURE ENABLE_POLICY (
    policy_name IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_policy_enable(policy_name);
  END;


END SA_SYSDBA;
//
