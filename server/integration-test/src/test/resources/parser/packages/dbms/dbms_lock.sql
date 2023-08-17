CREATE OR REPLACE PACKAGE dbms_lock AS

-- for the SESSION or SYSTEM
--  /*
--   * For the following functions, meanings of parameters are:
--   *
--   * 1. seconds:
--   *    Amount of time, in seconds, to suspend the session.
--   *    The smallest increment can be entered in hundredths of a second; for example, 1.95 is a legal time value.
--   */

  --
  -- This API suspends the session for a specified period of time
  --
  PROCEDURE sleep(seconds IN NUMBER);
 
END dbms_lock;
//