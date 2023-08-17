CREATE OR REPLACE PACKAGE BODY DBMS_SQL IS

  FUNCTION OPEN_CURSOR
  RETURN INTEGER;
  PRAGMA INTERFACE(c, dbms_sql_open_cursor);

  PROCEDURE PARSE(cursor_id       IN INTEGER,
                  sql_stmt        IN VARCHAR2,
                  language_flag   IN INTEGER);
  PRAGMA INTERFACE(c, dbms_sql_parse);

  PROCEDURE PARSE(c IN INTEGER,
                  statement IN VARCHAR2s,
                  lb IN INTEGER,
                  ub IN INTEGER,
                  lfflg IN BOOLEAN,
                  language_flag IN INTEGER);
  PRAGMA INTERFACE(c, dbms_sql_parse);

  PROCEDURE PARSE(c in integer, statement in varchar2a,
                lb in integer, ub in integer,
                lfflg in boolean, language_flag in integer);
  PRAGMA INTERFACE(c, dbms_sql_parse);
  
  PROCEDURE BIND_VARIABLE(cursor_id   IN INTEGER,
                          name        IN VARCHAR2,
                          value       IN NUMBER);
  PRAGMA INTERFACE(c, dbms_sql_bind_variable);

  PROCEDURE BIND_VARIABLE(cursor_id   IN INTEGER,
                          name        IN VARCHAR2,
                          value       IN VARCHAR2);
  PRAGMA INTERFACE(c, dbms_sql_bind_variable);

  FUNCTION  EXECUTE(cursor_id   IN INTEGER)
  RETURN INTEGER;
  PRAGMA INTERFACE(c, dbms_sql_execute);

  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN NUMBER);
  PRAGMA INTERFACE(c, dbms_sql_define_column);

  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN VARCHAR2);
  PRAGMA INTERFACE(c, dbms_sql_define_column);  

  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN VARCHAR2,
                          column_size IN INTEGER);
  PRAGMA INTERFACE(c, dbms_sql_define_column);   

  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN DATE);
  PRAGMA INTERFACE(c, dbms_sql_define_column);   

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in binary_float);  
  PRAGMA INTERFACE(c, dbms_sql_define_column);                               

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in binary_double);    
  PRAGMA INTERFACE(c, dbms_sql_define_column);                           

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in blob);  
  PRAGMA INTERFACE(c, dbms_sql_define_column);                           

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in raw,
                          column_size IN INTEGER);   
  PRAGMA INTERFACE(c, dbms_sql_define_column);                                        
  
  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT NUMBER);
  PRAGMA INTERFACE(c, dbms_sql_column_value);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT VARCHAR2);
  PRAGMA INTERFACE(c, dbms_sql_column_value);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT DATE);
  PRAGMA INTERFACE(c, dbms_sql_column_value);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT binary_float);
  PRAGMA INTERFACE(c, dbms_sql_column_value);                    

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT binary_double);
  PRAGMA INTERFACE(c, dbms_sql_column_value);                        

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT blob);
  PRAGMA INTERFACE(c, dbms_sql_column_value);                        

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT raw);
  PRAGMA INTERFACE(c, dbms_sql_column_value);                        

  FUNCTION FETCH_ROWS (c IN INTEGER)
  RETURN INTEGER;
  PRAGMA INTERFACE(c, dbms_sql_fetch_rows);

  PROCEDURE CLOSE_CURSOR(cursor_id    IN INTEGER);
  PRAGMA INTERFACE(c, dbms_sql_close_cursor);

END DBMS_SQL;
//

