CREATE OR REPLACE PACKAGE BODY SA_SESSION AS
/*
  FUNCTION COMP_READ ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION COMP_WRITE ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION GROUP_READ ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION GROUP_WRITE ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;
*/
  FUNCTION LABEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR AS
    ret VARCHAR (4000);
  BEGIN
    ret := label_to_char(ols_session_label(policy_name));
    RETURN ret;
  END;
/*
  FUNCTION MAX_LEVEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION MAX_READ_LABEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION MAX_WRITE_LABEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION MIN_LEVEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION MIN_WRITE_LABEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;

  FUNCTION PRIVS ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR;
*/
  PROCEDURE RESTORE_DEFAULT_LABELS (
    policy_name in VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_session_restore_default_label(policy_name);
  END;

  FUNCTION ROW_LABEL ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR AS
    ret VARCHAR (4000);
  BEGIN
    ret := label_to_char(ols_session_row_label(policy_name));
    RETURN ret;
  END;

  PROCEDURE SET_LABEL (
    policy_name IN VARCHAR,
    label       IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_session_set_label(policy_name, label);
  END;
/*
  FUNCTION SA_USER_NAME ( 
    policy_name IN VARCHAR
  )
  RETURN VARCHAR; 

  PROCEDURE SAVE_DEFAULT_LABELS ( 
    policy_name IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;

  PROCEDURE SET_ACCESS_PROFILE (
    policy_name IN VARCHAR,
    user_name   IN VARCHAR
  ) IS
    ret INT;
  BEGIN
  END;
*/
  PROCEDURE SET_ROW_LABEL (
    policy_name   IN VARCHAR,
    row_label     IN VARCHAR
  ) IS
    ret INT;
  BEGIN
    ret := ols_session_set_row_label(policy_name, row_label);
  END;

END SA_SESSION;
//
