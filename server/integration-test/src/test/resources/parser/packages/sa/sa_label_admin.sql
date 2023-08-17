CREATE OR REPLACE PACKAGE SA_LABEL_ADMIN AS
  PROCEDURE CREATE_LABEL (
    policy_name IN VARCHAR,
    label_tag   IN BINARY_INTEGER,
    label_value IN VARCHAR,
    data_label  IN BOOLEAN := TRUE
  );

  PROCEDURE ALTER_LABEL (
    policy_name       IN VARCHAR,
    label_tag         IN BINARY_INTEGER,
    new_label_value   IN VARCHAR := NULL,
    new_data_label    IN BOOLEAN := NULL
  );
/*
  PROCEDURE ALTER_LABEL (
    policy_name       IN VARCHAR,
    label_value       IN VARCHAR,
    new_label_value   IN VARCHAR := NULL,
    new_data_label    IN BOOLEAN := NULL
  );
*/
  PROCEDURE DROP_LABEL (
    policy_name       IN VARCHAR,
    label_tag         IN BINARY_INTEGER
  );
/*
  PROCEDURE DROP_LABEL (
    policy_name       IN VARCHAR,
    label_value       IN VARCHAR
  );
*/
END SA_LABEL_ADMIN;
//
