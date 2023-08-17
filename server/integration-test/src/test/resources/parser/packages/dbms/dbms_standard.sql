CREATE OR REPLACE PACKAGE STANDARD IS

  /********** Types and subtypes, do not reorder **********/
  -- type BOOLEAN is (FALSE, TRUE);

  -- type DATE is DATE_BASE;

  -- type NUMBER is NUMBER_BASE;
  -- subtype FLOAT is NUMBER; -- NUMBER(126)
  -- subtype REAL is FLOAT; -- FLOAT(63)
  subtype "DOUBLE PRECISION" is FLOAT;
  -- subtype INTEGER is NUMBER(38,0);
  -- subtype INT is INTEGER;
  -- subtype SMALLINT is NUMBER(38,0);
  -- subtype DECIMAL is NUMBER(38,0);
  -- subtype NUMERIC is DECIMAL;
  subtype DEC is DECIMAL;

  -- subtype BINARY_INTEGER is INTEGER range '-2147483647'..2147483647;
  -- subtype NATURAL is BINARY_INTEGER range 0..2147483647;
  -- subtype NATURALN is NATURAL not null;
  -- subtype POSITIVE is BINARY_INTEGER range 1..2147483647;
  -- subtype POSITIVEN is POSITIVE not null;
  -- subtype SIGNTYPE is BINARY_INTEGER range '-1'..1;  -- for SIGN functions

  -- type VARCHAR2 is NEW CHAR_BASE;
  -- subtype VARCHAR is VARCHAR2(32760);
  subtype STRING is VARCHAR2(32760);

  subtype LONG is VARCHAR2(32760);

  -- subtype RAW is VARCHAR2;
  -- subtype "LONG RAW" is RAW(32760);

  -- subtype ROWID is VARCHAR2(256);

  -- Ansi fixed-length char
  -- Define synonyms for CHAR and CHARN.
  -- subtype CHAR is VARCHAR2;
  -- subtype CHARACTER is CHAR;

  -- type MLSLABEL is new CHAR_BASE;

  -- Large object data types.
  --  binary, character, binary file.
  -- type  BLOB is BLOB_BASE;
  -- type  CLOB is CLOB_BASE;
  -- type  BFILE is BFILE_BASE;

  -- Verbose and NCHAR type names
  subtype "CHARACTER VARYING" is VARCHAR(32767);
  subtype "CHAR VARYING" is VARCHAR(32767);
  -- subtype "NATIONAL CHARACTER" is CHAR CHARACTER SET NCHAR_CS;
  -- subtype "NATIONAL CHAR" is CHAR CHARACTER SET NCHAR_CS;
  -- subtype "NCHAR" is CHAR CHARACTER SET NCHAR_CS;
  -- subtype "NATIONAL CHARACTER VARYING" is VARCHAR CHARACTER SET NCHAR_CS;
  -- subtype "NATIONAL CHAR VARYING" is VARCHAR CHARACTER SET NCHAR_CS;
  -- subtype "NCHAR VARYING" is VARCHAR CHARACTER SET NCHAR_CS;
  -- subtype "NVARCHAR2" is VARCHAR2 CHARACTER SET NCHAR_CS;
  subtype "CHARACTER LARGE OBJECT" is CLOB;
  -- subtype "NATIONAL CHARACTER LARGE OBJEC" is CLOB CHARACTER SET NCHAR_CS;
  -- subtype "NCHAR LARGE OBJECT" is CLOB CHARACTER SET NCHAR_CS;
  -- subtype "NCLOB" is CLOB CHARACTER SET NCHAR_CS;
  subtype "BINARY LARGE OBJECT" is BLOB;
  subtype "CHAR LARGE OBJECT" is CLOB;

  -- subtype pls_integer is binary_integer;

  -- type TIME is new DATE_BASE;
  -- type TIMESTAMP is new DATE_BASE;
  -- type "TIME WITH TIME ZONE" is new DATE_BASE;
  -- type "TIMESTAMP WITH TIME ZONE" is new DATE_BASE;
  -- type "INTERVAL YEAR TO MONTH" is new DATE_BASE;
  -- type "INTERVAL DAY TO SECOND" is new DATE_BASE;

  -- SUBTYPE TIME_UNCONSTRAINED IS TIME(9);
  -- SUBTYPE TIME_TZ_UNCONSTRAINED IS TIME(9) WITH TIME ZONE;
  SUBTYPE TIMESTAMP_UNCONSTRAINED IS TIMESTAMP(9);
  SUBTYPE TIMESTAMP_TZ_UNCONSTRAINED IS TIMESTAMP(9) WITH TIME ZONE;
  SUBTYPE YMINTERVAL_UNCONSTRAINED IS INTERVAL YEAR(9) TO MONTH;
  SUBTYPE DSINTERVAL_UNCONSTRAINED IS INTERVAL DAY(9) TO SECOND (9);

  -- TYPE UROWID IS NEW CHAR_BASE;

  -- type "TIMESTAMP WITH LOCAL TIME ZONE" is new DATE_BASE;
  subtype timestamp_ltz_unconstrained is timestamp(9) with local time zone;

  -- subtype BINARY_FLOAT is NUMBER;
  -- subtype BINARY_DOUBLE is NUMBER;

  -- The following data types are generics, used specially within package
  -- STANDARD and some other Oracle packages.  They are protected against
  -- other use; sorry.  True generic types are not yet part of the language.

  type "<ADT_1>" is record (dummy char(1));
  type "<RECORD_1>" is record (dummy char(1));
  type "<TUPLE_1>" is record (dummy char(1));
  type "<VARRAY_1>" is varray (1) of char(1);
  type "<V2_TABLE_1>" is table of char(1) index by binary_integer;
  type "<TABLE_1>" is table of char(1);
  type "<COLLECTION_1>" is table of char(1);
  type "<REF_CURSOR_1>" is ref cursor;

  -- This will actually match against a Q_TABLE
  type "<TYPED_TABLE>" is table of  "<ADT_1>";
  subtype "<ADT_WITH_OID>" is "<TYPED_TABLE>";

  -- The following generic index table data types are used by the PL/SQL
  -- compiler to materialize an array attribute at the runtime (for more
  -- details about the array attributes, please see Bulk Binds document).
  type " SYS$INT_V2TABLE" is table of integer index by binary_integer;

  -- The following record type and the corresponding generic index table 
  -- data types are used by the PL/SQL compiler to materialize a table
  -- at the runtime in order to record the exceptions raised during the
  -- execution of FORALL bulk bind statement (for more details, please 
  -- see bulk binds extensions document in 8.2).
  type " SYS$BULK_ERROR_RECORD" is
          record (error_index pls_integer, error_code pls_integer);
  type " SYS$REC_V2TABLE" is table of " SYS$BULK_ERROR_RECORD"
                               index by binary_integer;

  /* Adding a generic weak ref cursor type */
  -- type sys_refcursor is ref cursor;

  /* the following data type is a generic for all opaque types */
  -- type "<OPAQUE_1>" as opaque FIXED(1) USING LIBRARY dummy_lib
  --   (static function dummy return number);

  type "<ASSOC_ARRAY_1>" is table of char(1) index by varchar2(1);

  /********** Add new types or subtypes here **********/

  -- Simple scalar types

  -- subtype SIMPLE_INTEGER is BINARY_INTEGER NOT NULL;
  -- subtype SIMPLE_FLOAT   is BINARY_FLOAT   NOT NULL;
  -- subtype SIMPLE_DOUBLE  is BINARY_DOUBLE  NOT NULL;

  /********** Add new types or subtypes here **********/

  /********** Predefined constants **********/

  -- BINARY_FLOAT_NAN constant BINARY_FLOAT;
  -- BINARY_FLOAT_INFINITY constant BINARY_FLOAT;
  -- BINARY_FLOAT_MAX_NORMAL constant BINARY_FLOAT := 3.40282347E+038;
  -- BINARY_FLOAT_MIN_NORMAL constant BINARY_FLOAT := 1.17549435E-038;
  -- BINARY_FLOAT_MAX_SUBNORMAL constant BINARY_FLOAT := 1.17549421E-038;
  -- BINARY_FLOAT_MIN_SUBNORMAL constant BINARY_FLOAT := 1.40129846E-045;
  -- BINARY_DOUBLE_NAN constant BINARY_DOUBLE;
  -- BINARY_DOUBLE_INFINITY constant BINARY_DOUBLE;
  -- BINARY_DOUBLE_MAX_NORMAL constant BINARY_DOUBLE := 1.7976931348623157E+308;
  -- BINARY_DOUBLE_MIN_NORMAL constant BINARY_DOUBLE := 2.2250738585072014E-308;
  -- BINARY_DOUBLE_MAX_SUBNORMAL constant BINARY_DOUBLE := 2.2250738585072009E-308;
  -- BINARY_DOUBLE_MIN_SUBNORMAL constant BINARY_DOUBLE := 4.9406564584124654E-324;

  /********** Add new constants here **********/

  /********** Predefined exceptions **********/

  CURSOR_ALREADY_OPEN exception;
    pragma EXCEPTION_INIT(CURSOR_ALREADY_OPEN, '-5589');

  -- DUP_VAL_ON_INDEX exception;
  --   pragma EXCEPTION_INIT(DUP_VAL_ON_INDEX, '-0001');

  -- TIMEOUT_ON_RESOURCE exception;
  --   pragma EXCEPTION_INIT(TIMEOUT_ON_RESOURCE, '-0051');

  -- INVALID_CURSOR exception;
  --   pragma EXCEPTION_INIT(INVALID_CURSOR, '-1001');

  -- NOT_LOGGED_ON exception;
  --   pragma EXCEPTION_INIT(NOT_LOGGED_ON, '-1012');

  -- LOGIN_DENIED exception;
  --   pragma EXCEPTION_INIT(LOGIN_DENIED, '-1017');

  NO_DATA_FOUND exception;
    pragma EXCEPTION_INIT(NO_DATA_FOUND, '-4026');

  ZERO_DIVIDE exception;
    pragma EXCEPTION_INIT(ZERO_DIVIDE, '-4333');

  -- INVALID_NUMBER exception;
  --   pragma EXCEPTION_INIT(INVALID_NUMBER, '-1722');

  TOO_MANY_ROWS exception;
    pragma EXCEPTION_INIT(TOO_MANY_ROWS, '-5294');

  -- STORAGE_ERROR exception;
  --   pragma EXCEPTION_INIT(STORAGE_ERROR, '-6500');

  -- PROGRAM_ERROR exception;
  --   pragma EXCEPTION_INIT(PROGRAM_ERROR, '-6501');

  VALUE_ERROR exception;
    pragma EXCEPTION_INIT(VALUE_ERROR, '-5677');

  -- ACCESS_INTO_NULL exception;
  --   pragma EXCEPTION_INIT(ACCESS_INTO_NULL, '-6530');

  -- COLLECTION_IS_NULL exception;
  --   pragma EXCEPTION_INIT(COLLECTION_IS_NULL , '-6531');

  -- SUBSCRIPT_OUTSIDE_LIMIT exception;
  --   pragma EXCEPTION_INIT(SUBSCRIPT_OUTSIDE_LIMIT,'-6532');

  SUBSCRIPT_BEYOND_COUNT exception;
    pragma EXCEPTION_INIT(SUBSCRIPT_BEYOND_COUNT ,'-5828');

  -- -- exception for ref cursors
  -- ROWTYPE_MISMATCH exception;
  -- pragma EXCEPTION_INIT(ROWTYPE_MISMATCH, '-6504');

  -- SYS_INVALID_ROWID  EXCEPTION;
  -- PRAGMA EXCEPTION_INIT(SYS_INVALID_ROWID, '-1410');

  -- -- The object instance i.e. SELF is null
  -- SELF_IS_NULL exception;
  --   pragma EXCEPTION_INIT(SELF_IS_NULL, '-30625');

  CASE_NOT_FOUND exception;
    pragma EXCEPTION_INIT(CASE_NOT_FOUND, '-5571');

  -- -- Added for USERENV enhancement, bug 1622213.
  -- USERENV_COMMITSCN_ERROR exception;
  --   pragma EXCEPTION_INIT(USERENV_COMMITSCN_ERROR, '-1725');

  -- -- Parallel and pipelined support
  -- NO_DATA_NEEDED exception;
  --   pragma EXCEPTION_INIT(NO_DATA_NEEDED, '-6548');
  -- End of 8.2 parallel and pipelined support

  /********** Add new exceptions here **********/

END;
//

