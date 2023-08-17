CREATE OR REPLACE PACKAGE dbms_warning AS
-- DBMS_WARNING packages, exposes API's to set and get warning settings
-- for the SESSION or SYSTEM
--  /*
--   * For the following functions, meanings of parameters are:
--   *
--   * 1. warning_category - one of:
--   *    - 'ALL'
--   *    - 'INFORMATIONAL'
--   *    - 'SEVERE'
--   *    - 'PERFORMANCE'
--   *
--   * 2. warning_value - one of:
--   *    - 'ENABLE'
--   *    - 'DISABLE'
--   *    - 'ERROR'
--   *
--   * 3. scope - one of:
--   *    - 'SYSTEM'
--   *    - 'SESSION'
--   *
--   * 4. warning_number - any valid warning number
--   */
  --
  -- This API changes the warning_category to warning value without affecting
  -- other independent categories.
  --
  PROCEDURE add_warning_setting_cat(warning_category IN VARCHAR2,
                                    warning_value    IN VARCHAR2,
                                    scope            IN VARCHAR2);
  --
  -- This API changes the warning_number to warning value without affecting
  -- other existing settings.
  --
  PROCEDURE add_warning_setting_num(warning_number IN PLS_INTEGER,
                                    warning_value  IN VARCHAR2,
                                    scope          IN VARCHAR2);
  --
  -- This API returns the session warning_value for a given warning_category
  -- If any of parameter values are incorrect or, if the function was
  -- unsuccessful 'INVALID' is returned, the session warning_value is
  -- returned on successful completion.
  --
  FUNCTION get_warning_setting_cat(warning_category IN VARCHAR2) RETURN VARCHAR2;

  --
  -- This API returns the session warning_value for a given warning_number
  -- If any of parameter values are incorrect or, if the function was
  -- unsuccessful 'INVALID' is returned, the session warning_value is
  -- returned on successful completion.
  --
  FUNCTION get_warning_setting_num(warning_number IN PLS_INTEGER) RETURN VARCHAR2;

  --
  -- This API returns the entire warning setting string for the current
  -- session
  --
  FUNCTION get_warning_setting_string RETURN VARCHAR2;

  --
  -- This API sets the entire warning string, replacing the old values.
  -- It can set the value for the SESSION or for SYSTEM depending on the
  -- value of the scope parameter.
  --
  PROCEDURE set_warning_setting_string(VALUE IN VARCHAR2, scope IN VARCHAR2);

  --
  -- This API returns the warning category name for the given warning number
  --
  FUNCTION get_category(warning_number IN  PLS_INTEGER) RETURN VARCHAR2;
END dbms_warning;
//