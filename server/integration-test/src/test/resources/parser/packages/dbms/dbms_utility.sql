CREATE OR REPLACE PACKAGE dbms_utility AS
  ------------
  --  OVERVIEW
  --
  --  This package provides various utility routines.

  ----------------------------
  --  PL/SQL TABLES
  --


  --  Lists of "USER"."NAME"."COLUMN"@LINK should be stored here
  type uncl_array IS table of VARCHAR2(32767) index by BINARY_INTEGER;

  --  Lists of NAME should be stored here
  type name_array IS table of VARCHAR2(128) index by BINARY_INTEGER;

  --  Lists of NAME that might be quoted should be stored here
  type quoted_name_array IS table of VARCHAR2(130) index by BINARY_INTEGER;

  --  Lists of Long NAME should be stored here, it includes fully
  --  qualified attribute names.
  type lname_array IS table of VARCHAR2(4000) index by BINARY_INTEGER;

  --  Lists of large VARCHAR2s should be stored here
  type maxname_array IS table of VARCHAR2(32767) index by BINARY_INTEGER;

  --  Lists of database links should be stored here
  type dblink_array IS table of VARCHAR2(128) index by BINARY_INTEGER;

  --  order in which objects should be generated is returned here
  TYPE index_table_type IS TABLE OF BINARY_INTEGER INDEX BY BINARY_INTEGER;

  --  order in which objects should be generated is returned here for users.
  TYPE number_array IS TABLE OF NUMBER INDEX BY BINARY_INTEGER;

  TYPE instance_record IS RECORD (
       inst_number   NUMBER,
       inst_name     VARCHAR2(60));

  -- list of active instance number and instance name
  -- the starting index of instance_table is 1
  -- instance_table is dense
  TYPE instance_table IS TABLE OF instance_record INDEX BY BINARY_INTEGER;

  --  Format the current error stack.  This can be used in exception
  --    handlers to look at the full error stack.
  --  Output arguments:
  --    format_error_stack
  --      Returns the error stack.  May be up to 2000 bytes.
  -- function format_error_stack return varchar2;
  --  pragma interface (C, format_error_stack);

  --  Format the current call stack.  This can be used an any stored
  --    procedure or trigger to access the call stack.  This can be
  --    useful for debugging.
  --  Output arguments:
  --    format_call_stack
  --      Returns the call stack.  May be up to 2000 bytes.
  function format_call_stack return varchar2;

  --  Format the current error stack.  This can be used in exception
  --    handlers to look at the full error stack.
  --  Output arguments:
  --    format_error_stack
  --      Returns the error stack.  May be up to 2000 bytes.
  function format_error_stack return varchar2;

  --  Format the backtrace from the point of the current error
  --  to the exception handler where the error has been caught.
  --  NULL string is returned if no error is currently being
  --  handled.
  function format_error_backtrace return varchar2;

  --     dbms_output.put_line(intval);
  --   END IF;
  --   IF partyp = 1 THEN
  --     dbms_output.put('parameter value length is: ');
  --     dbms_output.put_line(intval);
  --   END IF;
  --   dbms_output.put('parameter type is: ');
  --   IF partyp = 1 THEN
  --     dbms_output.put_line('string');
  --   ELSE
  --     dbms_output.put_line('integer');
  --   END IF;
  -- END;
  procedure name_tokenize ( NAME    IN  VARCHAR2,
                           A       OUT VARCHAR2,
                           B       OUT VARCHAR2,
                           C       OUT VARCHAR2,
                           DBLINK  OUT VARCHAR2,
                           NEXTPOS OUT BINARY_INTEGER);

    --  Resolve the given name.  Do synonym translation if necessary.  Do
  --    authorization checking.
  --  Input arguments:
  --    name
  --      The name of the object.  This can be of the form [[a.]b.]c[@d]
  --      where a,b,c are SQL identifier and d is a dblink.  No syntax
  --      checking is performed on the dblink.  If a dblink is specified,
  --      of the name resolves to something with a dblink, then object
  --      is not resolved, but the schema, part1, part2 and dblink out
  --      arguments are filled in.  a,b and c may be delimted identifiers,
  --      and may contain NLS characters (single and multi-byte).
  --    context
  --      Must be an integer between 0 and 9.
  --      0 -- table or view, error if extra name parts present
  --      1 -- pl/sql (for 2 part names)
  --      2 -- sequence, or table/view with extra trailing name parts allowed
  --      3 -- trigger
  --      4 -- Java Source
  --      5 -- Java resource
  --      6 -- Java class
  --      7 -- type
  --      8 -- Java shared data
  --      9 -- index
  --  Output arguments:
  --    schema
  --      The schema of the object.  If no schema is specified in 'name'
  --      then the schema is determined by resolving the name.
  --    part1
  --      The first part of the name.  The type of this name is specified
  --      part1_type (synonym, procedure or package).
  --    part2
  --      If this is non-null, then this is a procedure name within the
  --      package indicated by part1.
  --    dblink
  --      If this is non-null then a database link was either specified
  --      as part of 'name' or 'name' was a synonym which resolved to
  --      something with a database link.  In this later case, part1_type
  --      will indicate a synonym.
  --    part1_type
  --      The type of part1 is
  --        5 - synonym
  --        7 - procedure (top level)
  --        8 - function (top level)
  --        9 - package
  --      If a synonym, it means that 'name' is a synonym that translats
  --      to something with a database link.  In this case, if further
  --      name translation is desired, then you must call the
  --      dbms_utility.name_resolve procedure on this remote node.
  --    object_number
  --      If non-null then 'name' was successfully resolved and this is the
  --      object number which it resolved to.
  --  Exceptions:
  --    All errors are handled by raising exceptions.  A wide variety of
  --    exceptions are possible, based on the various syntax error that
  --    are possible when specifying object names.
  PROCEDURE NAME_RESOLVE(NAME IN VARCHAR2, CONTEXT IN NUMBER,
    SCHEMA1 OUT VARCHAR2, PART1 OUT VARCHAR2, PART2 OUT VARCHAR2,
    DBLINK OUT VARCHAR2, PART1_TYPE OUT NUMBER, OBJECT_NUMBER OUT NUMBER);

  --  Gets value of specified init.ora parameter.
  --  Input arguments:
  --    parnam
  --      Parameter name
  --    listno
  --      List item number. If we are retrieving the parameter values for
  --      a parameter that can be specified multiple times to accumulate
  --      values (Eg rollback_segments) then this can be used to get each
  --      individual parameter. Eg, if we have the following :
  --
  --          rollback_segments = rbs1
  --          rollback_segments = rbs2
  --
  --      then use a value of 1 to get "rbs1" and a value of 2 to get "rbs2".
  --
  --  Output arguments:
  --    intval
  --      Value of an integer parameter or value length of a string parameter
  --    strval
  --      Value of a string parameter
  --  Returns:
  --    partyp
  --      Parameter type
  --        0 if parameter is an integer/boolean parameter
  --        1 if parameter is a  string/file parameter
  --  Notes
  --    1. Certain parameters can store values much larger than can be
  --       returned by this function. When this function is requested to
  --       retrieve the setting for such parameters and "unsupported parameter"
  --       exception will be raised. The "shared_pool_size" parameter is one
  --       such parameter.
  -- Example usage:
  -- DECLARE
  --   parnam VARCHAR2(256);
  --   intval BINARY_INTEGER;
  --   strval VARCHAR2(256);
  --   partyp BINARY_INTEGER;
  -- BEGIN
  --   partyp := dbms_utility.get_parameter_value('max_dump_file_size',
  --                                               intval, strval);
  --   dbms_output.put('parameter value is: ');
  --   IF partyp = 1 THEN
  --     dbms_output.put_line(strval);
  --   ELSE
  --     dbms_output.put_line(intval);
  --   END IF;
  --   IF partyp = 1 THEN
  --     dbms_output.put('parameter value length is: ');
  --     dbms_output.put_line(intval);
  --   END IF;
  --   dbms_output.put('parameter type is: ');
  --   IF partyp = 1 THEN
  --     dbms_output.put_line('string');
  --   ELSE
  --     dbms_output.put_line('integer');
  --   END IF;
  -- END;
  FUNCTION GET_PARAMETER_VALUE(PARNAM IN     VARCHAR2,
                               INTVAL IN OUT BINARY_INTEGER,
                               STRVAL IN OUT VARCHAR2,
                               LISTNO IN     BINARY_INTEGER DEFAULT 1) RETURN BINARY_INTEGER;

  
  FUNCTION GET_SQL_HASH(NAME IN VARCHAR2, HASH OUT RAW,
                        PRE10IHASH OUT NUMBER) RETURN NUMBER;
  FUNCTION IS_BIT_SET(R IN RAW, N IN NUMBER) RETURN NUMBER;
  PROCEDURE CANONICALIZE(NAME           IN   VARCHAR2,
                        CANON_NAME     OUT  VARCHAR2,
                        CANON_LEN      IN   BINARY_INTEGER);

  --  Convert a PL/SQL table of names into a comma-separated list of names
  --  This is an overloaded version for supporting fully-qualified attribute
  --  names.
  PROCEDURE TABLE_TO_COMMA( TAB    IN  UNCL_ARRAY,
                            TABLEN OUT BINARY_INTEGER,
                            LIST   OUT VARCHAR2);

  --  Convert a PL/SQL table of names into a comma-separated list of names
  PROCEDURE TABLE_TO_COMMA( TAB    IN  LNAME_ARRAY,
                            TABLEN OUT BINARY_INTEGER,
                            LIST   OUT VARCHAR2);

  PROCEDURE COMMA_TO_TABLE( LIST        IN  VARCHAR2,
                          ARRAY_TYPE  IN  BINARY_INTEGER,
                          TABLEN      OUT BINARY_INTEGER,
                          TAB_U       OUT UNCL_ARRAY,
                          TAB_A       OUT LNAME_ARRAY );

  -- 暂时不支持，因为ob pl的限制： overloaded routine out parameter not in same position!
  -- --  Convert a comma-separated list of names into a PL/SQL table of names
  -- --  This uses name_tokenize to figure out what are names and what are commas
  -- --  See name_tokenize for the expected syntax of the names.
  -- PROCEDURE COMMA_TO_TABLE( LIST   IN  VARCHAR2,
  --                           TABLEN OUT BINARY_INTEGER,
  --                           TAB    OUT UNCL_ARRAY );

  -- --  Convert a comma-separated list of names into a PL/SQL table of names
  -- --  This is an overloaded version for supporting fully-qualified attribute
  -- --  names of the form "a [. b ]*".
  -- PROCEDURE COMMA_TO_TABLE( LIST   IN  VARCHAR2,
  --                           TABLEN OUT BINARY_INTEGER,
  --                           TAB    OUT LNAME_ARRAY );
  FUNCTION ICD_GET_DBV RETURN VARCHAR2;

  -- Return version information for the database:
  -- version -> A string which represents the internal software version
  --            of the database (e.g., 7.1.0.0.0). The length of this string
  --            is variable and is determined by the database version.
  -- compatibility -> The compatibility setting of the database determined by
  --                  the "compatible" init.ora parameter. If the parameter
  --                  is not specified in the init.ora file, NULL is returned.
  PROCEDURE DB_VERSION(VERSION OUT VARCHAR2, COMPATIBILITY OUT VARCHAR2);

  --  Convert a comma-separated list of names into a PL/SQL table of names
  --  This is an overloaded version for supporting fully-qualified attribute
  --  names of the form "a [. b ]*".
  FUNCTION PORT_STRING RETURN VARCHAR2;

  --  Compute a hash value for the given string
  --  Input arguments:
  --    name  - The string to be hashed.
  --    base  - A base value for the returned hash value to start at.
  --    hash_size -  The desired size of the hash table.
  --  Returns:
  --    A hash value based on the input string.
  --    For example, to get a hash value on a string where the hash value
  --    should be between 1000 and 3047, use 1000 as the base value and
  --    2048 as the hash_size value.  Using a power of 2 for the hash_size
  --    parameter works best.
  --  Exceptions:
  --    OBE-29261 will be raised if hash_size is 0 or if hash_size or
  --    base are null
  FUNCTION GET_HASH_VALUE(NAME VARCHAR2, BASE NUMBER, HASH_SIZE NUMBER) RETURN NUMBER;
  FUNCTION ICD_GET_TIME RETURN BINARY_INTEGER;

  --  Find out the current elapsed time in 100th's of a second.
  --  Output:
  --      The returned elapsed time is the number of 100th's
  --      of a second from some arbitrary epoch.
  FUNCTION GET_TIME RETURN NUMBER;


  FUNCTION IS_CLUSTER_DATABASE RETURN BOOLEAN;

  -- Return the current connected instance number
  -- Return NULL when connected instance is down
  FUNCTION CURRENT_INSTANCE RETURN NUMBER;

  -- instance_table contains a list of the active instance numbers and names
  -- When no instance is up ( or non-OPS setting), the list is empty
  -- instance_count is  the number of active instances, 0 under non-ops setting
  PROCEDURE ACTIVE_INSTANCES (INSTANCE_TABLE OUT INSTANCE_TABLE, INSTANCE_COUNT OUT NUMBER);

  FUNCTION OLD_CURRENT_SCHEMA RETURN VARCHAR2;
  FUNCTION OLD_CURRENT_USER RETURN VARCHAR2;
  FUNCTION ICD_GET_ENDIANNESS RETURN NUMBER;
  FUNCTION GET_ENDIANNESS RETURN NUMBER;
END dbms_utility;
//