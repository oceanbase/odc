CREATE OR REPLACE PACKAGE SA_COMPONENTS AS
/*
  PROCEDURE ALTER_COMPARTMENT (
    policy_name       IN VARCHAR,
    comp_num          IN NUMBER,
    new_short_name    IN VARCHAR,
    new_long_name     IN VARCHAR
  );

  PROCEDURE ALTER_COMPARTMENT (
    policy_name       IN VARCHAR,
    short_name        IN VARCHAR := NULL,
    new_long_name     IN VARCHAR := NULL
  );
     
  PROCEDURE ALTER_GROUP (
    policy_name    IN VARCHAR,
    group_num      IN NUMBER,
    new_short_name IN VARCHAR := NULL,
    new_long_name  IN VARCHAR := NULL
  );

  PROCEDURE ALTER_GROUP (
    policy_name    IN VARCHAR,
    short_name     IN VARCHAR,
    new_long_name  IN VARCHAR);
     
  PROCEDURE ALTER_GROUP_PARENT (
    policy_name     IN VARCHAR,
    group_num       IN NUMBER,
    new_parent_num  IN NUMBER
  );

  PROCEDURE ALTER_GROUP_PARENT (
    policy_name     IN VARCHAR,
    group_num       IN NUMBER,
    new_parent_name IN VARCHAR
  );

  PROCEDURE ALTER_GROUP_PARENT (
    policy_name     IN VARCHAR,
    short_name      IN VARCHAR,
    new_parent_name IN VARCHAR
  );
 */    
  PROCEDURE ALTER_LEVEL (
    policy_name     IN VARCHAR,
    level_num       IN NUMBER,
    new_short_name  IN VARCHAR := NULL,
    new_long_name   IN VARCHAR := NULL
  );
/*
  PROCEDURE ALTER_LEVEL (
    policy_name     IN VARCHAR,
    short_name      IN VARCHAR,
    new_long_name   IN VARCHAR
  );
     
  PROCEDURE CREATE_GROUP (
    policy_name IN VARCHAR,
    group_num   IN NUMBER,
    short_name  IN VARCHAR,
    long_name   IN VARCHAR,
    parent_name IN VARCHAR := NULL
  );
*/
  PROCEDURE CREATE_LEVEL (
    policy_name       IN VARCHAR,
    level_num         IN NUMBER,
    short_name        IN VARCHAR,
    long_name         IN VARCHAR
  );
/*
  PROCEDURE DROP_COMPARTMENT (
    policy_name IN VARCHAR,
    comp_num    IN INTEGER
  );

  PROCEDURE DROP_COMPARTMENT (
    policy_name IN VARCHAR,
    short_name  IN VARCHAR
  );

  PROCEDURE DROP_GROUP (
    policy_name IN VARCHAR,
    group_num   IN NUMBER
  );

  PROCEDURE DROP_GROUP (
    policy_name IN VARCHAR,
    short_name  IN VARCHAR
  );
*/
  PROCEDURE DROP_LEVEL (
    policy_name IN VARCHAR,
    level_num   IN NUMBER
  );
/*
  PROCEDURE DROP_LEVEL (
    policy_name IN VARCHAR,
    short_name  IN VARCHAR
  );
*/
END SA_COMPONENTS;
//

