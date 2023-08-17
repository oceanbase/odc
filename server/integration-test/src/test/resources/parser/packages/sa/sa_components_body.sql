CREATE OR REPLACE PACKAGE BODY SA_COMPONENTS AS
/*
  PROCEDURE ALTER_COMPARTMENT (
    policy_name       IN VARCHAR,
    comp_num          IN NUMBER,
    new_short_name    IN VARCHAR,
    new_long_name     IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE ALTER_COMPARTMENT (
    policy_name       IN VARCHAR,
    short_name        IN VARCHAR := NULL,
    new_long_name     IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
  END;
     
  PROCEDURE ALTER_GROUP (
    policy_name    IN VARCHAR,
    group_num      IN NUMBER,
    new_short_name IN VARCHAR := NULL,
    new_long_name  IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE ALTER_GROUP (
    policy_name    IN VARCHAR,
    short_name     IN VARCHAR,
    new_long_name  IN VARCHAR) IS
    ret INT;
  BEGIN
  END;
     
  PROCEDURE ALTER_GROUP_PARENT (
    policy_name     IN VARCHAR,
    group_num       IN NUMBER,
    new_parent_num  IN NUMBER
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE ALTER_GROUP_PARENT (
    policy_name     IN VARCHAR,
    group_num       IN NUMBER,
    new_parent_name IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE ALTER_GROUP_PARENT (
    policy_name     IN VARCHAR,
    short_name      IN VARCHAR,
    new_parent_name IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;
*/     
  PROCEDURE ALTER_LEVEL (
    policy_name     IN VARCHAR,
    level_num       IN NUMBER,
    new_short_name  IN VARCHAR := NULL,
    new_long_name   IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
    ret := ols_level_alter(policy_name, level_num, new_short_name, new_long_name);
  END;
/*
  PROCEDURE ALTER_LEVEL (
    policy_name     IN VARCHAR,
    short_name      IN VARCHAR,
    new_long_name   IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;
     
  PROCEDURE CREATE_GROUP (
    policy_name IN VARCHAR,
    group_num   IN NUMBER,
    short_name  IN VARCHAR,
    long_name   IN VARCHAR,
    parent_name IN VARCHAR := NULL
  ) IS
    ret INT;
  BEGIN
  END;
*/
  PROCEDURE CREATE_LEVEL (
    policy_name       IN VARCHAR,
    level_num         IN NUMBER,
    short_name        IN VARCHAR,
    long_name         IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_level_create(policy_name, level_num, short_name, long_name);
  END;
/*
  PROCEDURE DROP_COMPARTMENT (
    policy_name IN VARCHAR,
    comp_num    IN INTEGER
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE DROP_COMPARTMENT (
    policy_name IN VARCHAR,
    short_name  IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE DROP_GROUP (
    policy_name IN VARCHAR,
    group_num   IN NUMBER
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE DROP_GROUP (
    policy_name IN VARCHAR,
    short_name  IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;
*/
  PROCEDURE DROP_LEVEL (
    policy_name IN VARCHAR,
    level_num   IN NUMBER
  ) IS
    ret INT;
  BEGIN
    ret := ols_level_drop(policy_name, level_num);
  END;
/*
  PROCEDURE DROP_LEVEL (
    policy_name IN VARCHAR,
    short_name  IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;
*/
END SA_COMPONENTS;
//

