CREATE OR REPLACE PACKAGE SA_USER_ADMIN AS
/*
  PROCEDURE ADD_COMPARTMENTS ( 
    policy_name    IN VARCHAR,
    user_name      IN VARCHAR,
    comps          IN VARCHAR,
    access_mode    IN VARCHAR := NULL,
    in_def         IN VARCHAR := NULL,
    in_row         IN VARCHAR := NULL
  );

  PROCEDURE ADD_GROUPS ( 
    policy_name       IN VARCHAR,
    user_name         IN VARCHAR,
    groups            IN VARCHAR,
    access_mode       IN VARCHAR := NULL,
    in_def            IN VARCHAR := NULL,
    in_row            IN VARCHAR := NULL
  );

  PROCEDURE ALTER_COMPARTMENTS (
    policy_name  IN VARCHAR,
    user_name    IN VARCHAR,
    comps        IN VARCHAR,
    access_mode  IN VARCHAR := NULL,
    in_def       IN VARCHAR := NULL,
    in_row       IN VARCHAR := NULL
  );

  PROCEDURE ALTER_GROUPS ( 
    policy_name      IN VARCHAR,
    user_name        IN VARCHAR,
    groups           IN VARCHAR,
    access_mode      IN VARCHAR := NULL,
    in_def           IN VARCHAR := NULL,
    in_row           IN VARCHAR := NULL
  );

  PROCEDURE DROP_ALL_COMPARTMENTS (
    policy_name  IN VARCHAR,
    user_name    IN VARCHAR
  );

  PROCEDURE DROP_ALL_GROUPS (
    policy_name IN VARCHAR,
    user_name   IN VARCHAR
  );

  PROCEDURE DROP_COMPARTMENTS ( 
    policy_name     IN VARCHAR,
    user_name       IN VARCHAR,
    comps           IN VARCHAR
  );

  PROCEDURE DROP_GROUPS ( 
    policy_name IN VARCHAR,
    user_name   IN VARCHAR,
    groups      IN VARCHAR
  );

  PROCEDURE DROP_USER_ACCESS (
    policy_name      IN VARCHAR,
    user_name        IN VARCHAR
  ); 

  PROCEDURE SET_COMPARTMENTS (
    policy_name   IN VARCHAR,
    user_name     IN VARCHAR,
    read_comps    IN VARCHAR,
    write_comps   IN VARCHAR := NULL,
    def_comps     IN VARCHAR := NULL,
    row_comps     IN VARCHAR := NULL
  );

  PROCEDURE SET_DEFAULT_LABELS (
    policy_name  IN VARCHAR,
    user_name    IN VARCHAR,
    def_label    IN VARCHAR
  );

  PROCEDURE SET_GROUPS (
    policy_name      IN VARCHAR,
    user_name        IN VARCHAR,
    read_groups      IN VARCHAR,
    write_groups     IN VARCHAR := NULL,
    def_group        IN VARCHAR := NULL,
    row_groups       IN VARCHAR := NULL
  );
*/
  PROCEDURE SET_LEVELS (
    policy_name      IN VARCHAR,
    user_name        IN VARCHAR,
    max_level        IN VARCHAR,
    min_level        IN VARCHAR := NULL,
    def_level        IN VARCHAR := NULL,
    row_level        IN VARCHAR := NULL
  );
/*
  PROCEDURE SET_PROG_PRIVS (
    policy_name           IN VARCHAR,
    schema_name           IN VARCHAR,
    program_unit_name     IN VARCHAR,
    privileges            IN VARCHAR
  );

  PROCEDURE SET_ROW_LABEL (
    policy_name   IN VARCHAR,
    user_name     IN VARCHAR,
    row_label     IN VARCHAR
  );

  PROCEDURE SET_USER_LABELS (
    policy_name      IN VARCHAR,
    user_name        IN VARCHAR,
    max_read_label   IN VARCHAR,
    max_write_label  IN VARCHAR := NULL,
    min_write_label  IN VARCHAR := NULL,
    def_label        IN VARCHAR := NULL,
    row_label        IN VARCHAR := NULL
  );

  PROCEDURE SET_USER_PRIVS (
    policy_name     IN VARCHAR,
    user_name       IN VARCHAR,
    privileges      IN VARCHAR
  );
*/

END SA_USER_ADMIN;
//