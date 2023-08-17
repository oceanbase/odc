CREATE OR REPLACE PACKAGE DBMS_SQL AUTHID CURRENT_USER IS

--  CONSTANTS
  v6 constant integer := 0;
  native constant integer := 1;
  v7 constant integer := 2;
  foreign_syntax constant integer := 4294967295;

--  TYPES
  TYPE varchar2a is table of varchar2(32767) index by binary_integer;
  TYPE varchar2s is table of varchar2(256) index by binary_integer;
  TYPE desc_rec is record (
	                    col_type	    binary_integer := 0,
	                    col_max_len	    binary_integer := 0,
                      col_name	    varchar2(32)   := '',
                      col_name_len	    binary_integer := 0,
                      col_schema_name     varchar2(32)   := '',
                      col_schema_name_len binary_integer := 0,
                      col_precision	    binary_integer := 0,
                      col_scale	    binary_integer := 0,
                      col_charsetid	    binary_integer := 0,
                      col_charsetform     binary_integer := 0,
                      col_null_ok	    boolean	   := TRUE);
  TYPE desc_tab is table of desc_rec index by binary_integer;
  TYPE desc_rec2 is record (
                      col_type	    binary_integer := 0,
                      col_max_len	    binary_integer := 0,
                      col_name	    varchar2(32767) := '',
                      col_name_len	    binary_integer := 0,
                      col_schema_name     varchar2(32)   := '',
                      col_schema_name_len binary_integer := 0,
                      col_precision	    binary_integer := 0,
                      col_scale	    binary_integer := 0,
                      col_charsetid	    binary_integer := 0,
                      col_charsetform     binary_integer := 0,
                      col_null_ok	    boolean	   := TRUE);
  TYPE desc_tab2 is table of desc_rec2 index by binary_integer;

  TYPE desc_rec3 is record (
                      col_type	    binary_integer := 0,
                      col_max_len	    binary_integer := 0,
                      col_name	    varchar2(32767) := '',
                      col_name_len	    binary_integer := 0,
                      col_schema_name     varchar2(32)   := '',
                      col_schema_name_len binary_integer := 0,
                      col_precision	    binary_integer := 0,
                      col_scale	    binary_integer := 0,
                      col_charsetid	    binary_integer := 0,
                      col_charsetform     binary_integer := 0,
                      col_null_ok	    boolean	   := TRUE,
                      col_type_name	    varchar2(32)   := '',
                      col_type_name_len   binary_integer := 0);
  TYPE desc_tab3 is table of desc_rec3 index by binary_integer;

  --TYPE desc_rec4 is record (
  --                    col_type	    binary_integer := 0,
  --                    col_max_len	    binary_integer := 0,
  --                    col_name	    varchar2(32767) := '',
  --                    col_name_len	    binary_integer := 0,
  --                    col_schema_name     dbms_id	   := '',
  --                    col_schema_name_len binary_integer := 0,
  --                    col_precision	    binary_integer := 0,
  --                    col_scale	    binary_integer := 0,
  --                    col_charsetid	    binary_integer := 0,
  --                    col_charsetform     binary_integer := 0,
  --                    col_null_ok	    boolean	   := TRUE,
  --                    col_type_name	    dbms_id	   := '',
  --                    col_type_name_len   binary_integer := 0);
  --TYPE desc_tab4 is table of desc_rec4 index by binary_integer;

  TYPE Number_Table   is table of number	 index by binary_integer;
  TYPE Varchar2_Table is table of varchar2(4000) index by binary_integer;
  TYPE Date_Table     is table of date		 index by binary_integer;
  TYPE Blob_Table     is table of Blob		 index by binary_integer;
  TYPE Clob_Table     is table of Clob		 index by binary_integer;
  --TYPE Bfile_Table    is table of Bfile 	 index by binary_integer;
  TYPE Urowid_Table   IS TABLE OF urowid	 INDEX BY binary_integer;
  --TYPE time_Table     IS TABLE OF time_unconstrained	       INDEX BY binary_integer;
  --TYPE timestamp_Table	 IS TABLE OF timestamp_unconstrained	     INDEX BY binary_integer;
  --TYPE time_with_time_zone_Table IS TABLE OF TIME_TZ_UNCONSTRAINED INDEX BY binary_integer;
  --TYPE timestamp_with_time_zone_Table IS TABLE OF TIMESTAMP_TZ_UNCONSTRAINED INDEX BY binary_integer;
  --TYPE timestamp_with_ltz_Table IS TABLE OF TIMESTAMP_LTZ_UNCONSTRAINED INDEX BY binary_integer;
  --TYPE interval_year_to_MONTH_Table IS TABLE OF yminterval_unconstrained INDEX BY binary_integer;
  --TYPE interval_day_to_second_Table IS TABLE OF dsinterval_unconstrained INDEX BY binary_integer;
  TYPE Binary_Float_Table is table of binary_float index by binary_integer;
  TYPE Binary_Double_Table is table of binary_double index by binary_integer;

-- EXCEPTIONS
  inconsistent_type exception; 
  pragma exception_init(inconsistent_type, -6562);

-- FUNCTIONS and PROCEDURES
  FUNCTION  OPEN_CURSOR
  RETURN INTEGER;

  PROCEDURE PARSE(cursor_id       IN INTEGER,
                  sql_stmt        IN VARCHAR2,
                  language_flag   IN INTEGER);

  PROCEDURE PARSE(c IN INTEGER,
                  statement IN VARCHAR2s,
                  lb IN INTEGER,
                  ub IN INTEGER,
                  lfflg IN BOOLEAN,
                  language_flag IN INTEGER);
  PROCEDURE PARSE(c in integer, statement in varchar2a,
                lb in integer, ub in integer,
                lfflg in boolean, language_flag in integer);

  PROCEDURE BIND_VARIABLE(cursor_id   IN INTEGER,
                          name        IN VARCHAR2,
                          value       IN NUMBER);

  PROCEDURE BIND_VARIABLE(cursor_id   IN INTEGER,
                          name        IN VARCHAR2,
                          value       IN VARCHAR2);

  FUNCTION  EXECUTE(cursor_id   IN INTEGER)
  RETURN INTEGER;


  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN NUMBER);                    

  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN VARCHAR2);      

  PROCEDURE DEFINE_COLUMN (c IN INTEGER,
                          position IN INTEGER,
                          column IN VARCHAR2,
                          column_size IN INTEGER);      

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in date); 

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in binary_float);      

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in binary_double);    

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in blob);  

  procedure DEFINE_COLUMN(c in integer, 
                          position in integer, 
                          column in raw,
                          column_size IN INTEGER);                           
  
  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT NUMBER);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT VARCHAR2);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT date);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT binary_float);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT binary_double);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT blob);

  PROCEDURE COLUMN_VALUE (c IN INTEGER,
                          position IN INTEGER,
                          value OUT raw);

  FUNCTION FETCH_ROWS (c IN INTEGER)
  RETURN INTEGER;

  PROCEDURE CLOSE_CURSOR(cursor_id    IN INTEGER);

END DBMS_SQL;
//

