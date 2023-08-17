CREATE OR REPLACE PACKAGE BODY SA_LABEL_ADMIN AS
  PROCEDURE CREATE_LABEL (
    policy_name IN VARCHAR,
    label_tag   IN BINARY_INTEGER,
    label_value IN VARCHAR,
    data_label  IN BOOLEAN := TRUE
  ) IS
    ret INT;
  BEGIN
    ret := ols_label_create(policy_name, label_tag, label_value, data_label);
  END;

  PROCEDURE ALTER_LABEL (
    policy_name       IN VARCHAR,
    label_tag         IN BINARY_INTEGER,
    new_label_value   IN VARCHAR := NULL,
    new_data_label    IN BOOLEAN := NULL
  ) IS
    ret INT;
  BEGIN
    ret := ols_label_alter(policy_name, label_tag, new_label_value, new_data_label);
  END;
/*
  PROCEDURE ALTER_LABEL (
    policy_name       IN VARCHAR,
    label_value       IN VARCHAR,
    new_label_value   IN VARCHAR := NULL,
    new_data_label    IN BOOLEAN  := NULL
  ) IS
    ret INT;
  BEGIN
  END;
*/
  PROCEDURE DROP_LABEL (
    policy_name       IN VARCHAR,
    label_tag         IN BINARY_INTEGER
  ) IS
    ret INT;
  BEGIN
    ret := ols_label_drop(policy_name, label_tag);
  END;
/*
  PROCEDURE DROP_LABEL (
    policy_name       IN VARCHAR,
    label_value       IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;
*/
END SA_LABEL_ADMIN;
//
