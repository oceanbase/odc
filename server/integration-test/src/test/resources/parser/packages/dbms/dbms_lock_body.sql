CREATE OR replace PACKAGE BODY dbms_lock AS
  PROCEDURE SLEEP(SECONDS IN NUMBER);
  pragma interface (C, SLEEP_LOCK);
END dbms_lock;
//