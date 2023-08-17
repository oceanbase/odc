CREATE OR REPLACE PACKAGE SA_POLICY_ADMIN AS
  
  PROCEDURE APPLY_TABLE_POLICY (
    policy_name       IN VARCHAR,
    schema_name       IN VARCHAR,
    table_name        IN VARCHAR,
    table_options     IN VARCHAR := NULL,
    label_function    IN VARCHAR := NULL,
    predicate         IN VARCHAR := NULL
  );

  PROCEDURE DISABLE_TABLE_POLICY (
    policy_name      IN VARCHAR,
    schema_name      IN VARCHAR,
    table_name       IN VARCHAR
  );

  PROCEDURE ENABLE_TABLE_POLICY (
    policy_name     IN VARCHAR,
    schema_name     IN VARCHAR,
    table_name      IN VARCHAR
  );

  PROCEDURE REMOVE_TABLE_POLICY (
    policy_name        IN VARCHAR,
    schema_name        IN VARCHAR,
    table_name         IN VARCHAR,
    drop_column        IN BOOLEAN := FALSE
  );
/*
  PROCEDURE ALTER_SCHEMA_POLICY (
    policy_name         IN VARCHAR,
    schema_name         IN VARCHAR,
    default_options     IN VARCHAR
  );

  PROCEDURE APPLY_SCHEMA_POLICY (
    policy_name        IN VARCHAR,
    schema_name        IN VARCHAR,
    default_options    IN VARCHAR := NULL
  );

  PROCEDURE DISABLE_SCHEMA_POLICY (
    policy_name    IN VARCHAR,
    schema_name    IN VARCHAR
  );

  PROCEDURE ENABLE_SCHEMA_POLICY (
    policy_name    IN VARCHAR,
    schema_name    IN VARCHAR
  );

  PROCEDURE POLICY_SUBSCRIBE(
    policy_name     IN VARCHAR
  );

  PROCEDURE POLICY_UNSUBSCRIBE(
    policy_name  IN VARCHAR
  );

  PROCEDURE REMOVE_SCHEMA_POLICY (
    policy_name     IN VARCHAR,
    schema_name     IN VARCHAR,
    drop_column     IN BOOLEAN := FALSE
  );
*/
END SA_POLICY_ADMIN;
//
