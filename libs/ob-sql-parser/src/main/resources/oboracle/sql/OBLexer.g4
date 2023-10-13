/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

lexer grammar OBLexer;
@members {
public boolean inRangeOperator = false;
}

M_SIZE
    : M
    ;

E_SIZE
    : E
    ;

T_SIZE
    : T
    ;

K_SIZE
    : K
    ;

G_SIZE
    : G
    ;

P_SIZE
    : P
    ;

HIDE
    : H I D E
    ;

DEFAULTS
    : D E F A U L T S
    ;

ACCESS
    : ( A C C E S S )
    ;

ADD
    : ( A D D )
    ;

NAMESPACE
    : N A M E S P A C E
    ;

ALL
    : ( A L L )
    ;

ALTER
    : ( A L T E R )
    ;

AND
    : ( A N D )
    ;

ANY
    : ( A N Y )
    ;

AS
    : ( A S )
    ;

ASC
    : ( A S C )
    ;

XMLATTRIBUTES
    : X M L A T T R I B U T E S
    ;

AUDIT
    : ( A U D I T )
    ;

JSON_OBJECT_VALUE
	: '\'' ([A-Za-z0-9$_ ]* | [A-Za-z][A-Za-z0-9$_#]*) '\'' ' '* ':' ([A-Za-z$_]+ | [A-Za-z][A-Za-z0-9$_#]* | [0-9]+)
    ;

VALUE
    : V A L U E
    ;

BETWEEN
    : ( B E T W E E N )
    ;

BLOB
    : ( B L O B )
    ;

BY
    : ( B Y )
    ;

BINARY_DOUBLE
    : ( B I N A R Y '_' D O U B L E )
    ;

BINARY_FLOAT
    : ( B I N A R Y '_' F L O A T )
    ;

CHAR
    : ( C H A R )
    ;

CHECK
    : ( C H E C K )
    ;

CIPHER
    : ( C I P H E R )
    ;

CLOB
    : ( C L O B )
    ;

CLUSTER
    : ( C L U S T E R )
    ;

COLUMN
    : ( C O L U M N )
    ;

COMMENT
    : ( C O M M E N T )
    ;

COMPRESS
    : ( C O M P R E S S )
    ;

CONNECT
    : ( C O N N E C T )
    ;

CREATE
    : ( C R E A T E )
    ;

CURRENT
    : ( C U R R E N T )
    ;

DATE
    : ( D A T E )
    ;

DECIMAL
    : ( D E C I M A L )
    ;

DEFAULT
    : ( D E F A U L T )
    ;

DELETE
    : ( D E L E T E )
    ;

DESC
    : ( D E S C )
    ;

DISTINCT
    : ( D I S T I N C T )
    ;

DOT
    : D O T
    ;

DROP
    : ( D R O P )
    ;

MULTISET
    : M U L T I S E T
    ;

JSON_ARRAYAGG
    : J S O N '_' A R R A Y A G G
    ;

ARRAY
    : A R R A Y
    ;

JSON_ARRAY
    : J S O N '_' A R R A Y
    ;

JSON_EMPTY
    : J S O N '_' E M P T Y
    ;

PASSING
    : P A S S I N G
    ;

ELSE
    : ( E L S E )
    ;

EXCLUSIVE
    : ( E X C L U S I V E )
    ;

EXISTS
    : ( E X I S T S )
    ;

EXCLUDE
    : ( E X C L U D E )
    ;

FILE_KEY
    : ( F I L E )
    ;

FLOAT
    : ( F L O A T )
    ;

FOR
    : ( F O R )
    ;

FROM
    : ( F R O M )
    ;

GRANT
    : ( G R A N T )
    ;

GROUP
    : ( G R O U P )
    ;

HAVING
    : ( H A V I N G )
    ;

HOST
    : ( H O S T )
    ;

ABSENT
    : A B S E N T
    ;

IDENTIFIED
    : ( I D E N T I F I E D )
    ;

IMMEDIATE
    : ( I M M E D I A T E )
    ;

IN
    : ( I N )
    ;

INCREMENT
    : ( I N C R E M E N T )
    ;

INCLUDE
    : ( I N C L U D E )
    ;

INDEX
    : ( I N D E X )
    ;

INITIAL_
    : ( I N I T I A L )
    ;

INSERT
    : ( I N S E R T )
    ;

FIELD_DELIMITER
    : F I E L D '_' D E L I M I T E R
    ;

INTEGER
    : ( I N T E G E R )
    ;

INTERSECT
    : ( I N T E R S E C T )
    ;

INTO
    : ( I N T O )
    ;

ORDINALITY
    : O R D I N A L I T Y
    ;

IS
    : ( I S )
    ;

ISSUER
    : ( I S S U E R )
    ;

LEVEL
    : ( L E V E L )
    ;

LIKE
    : ( L I K E )
    ;

LOCK
    : ( L O C K )
    ;

LONG
    : ( L O N G )
    ;

MAXEXTENTS
    : ( M A X E X T E N T S )
    ;

MINUS
    : ( M I N U S )
    ;

MODE
    : ( M O D E )
    ;

MODIFY
    : ( M O D I F Y )
    ;

NOAUDIT
    : ( N O A U D I T )
    ;

NOCOMPRESS
    : ( N O C O M P R E S S )
    ;

NOT
    : ( N O T )
    ;

NOTFOUND
    : ( N O T F O U N D )
    ;

NOWAIT
    : ( N O W A I T )
    ;

NULLX
    : ( N U L L )
    ;

MISSING
    : M I S S I N G
    ;

NUMBER
    : ( N U M B E R )
    ;

OF
    : ( O F )
    ;

OFFLINE
    : ( O F F L I N E )
    ;

ON
    : ( O N )
    ;

ONLINE
    : ( O N L I N E )
    ;

OPTION
    : ( O P T I O N )
    ;

OR
    : ( O R )
    ;

ORDER
    : ( O R D E R )
    ;

PCTFREE
    : ( P C T F R E E )
    ;

PIVOT
    : ( P I V O T )
    ;

PRIOR
    : ( P R I O R )
    ;

PRIVILEGES
    : ( P R I V I L E G E S )
    ;

PUBLIC
    : ( P U B L I C )
    ;

RAW
    : ( R A W )
    ;

REAL
    : ( R E A L )
    ;

RENAME
    : ( R E N A M E )
    ;

RESOURCE
    : ( R E S O U R C E )
    ;

REVOKE
    : ( R E V O K E )
    ;

ROW
    : ( R O W )
    ;

ROWID
    : ( R O W I D )
    ;

ROWLABEL
    : ( R O W L A B E L )
    ;

ACTIVATE
    : A C T I V A T E
    ;

ROWNUM
    : ( R O W N U M )
    ;

ROWS
    : ( R O W S )
    ;

START
    : ( S T A R T )
    ;

SELECT
    : ( S E L E C T )
    ;

SESSION
    : ( S E S S I O N )
    ;

SET
    : ( S E T )
    ;

SETS
    : ( S E T S )
    ;

SHARE
    : ( S H A R E )
    ;

SIZE
    : ( S I Z E )
    ;

SMALLINT
    : ( S M A L L I N T )
    ;

SUCCESSFUL
    : ( S U C C E S S F U L )
    ;

SYNONYM
    : ( S Y N O N Y M )
    ;

SYSDATE
    : ( S Y S D A T E )
    ;

SYSTIMESTAMP
    : ( S Y S T I M E S T A M P )
    ;

TABLE
    : ( T A B L E )
    ;

THEN
    : ( T H E N )
    ;

TO
    : ( T O )
    ;

TRIGGER
    : ( T R I G G E R )
    ;

UID
    : ( U I D )
    ;

UNION
    : ( U N I O N )
    ;

UNIQUE
    : ( U N I Q U E )
    ;

UNPIVOT
    : ( U N P I V O T )
    ;

UPDATE
    : ( U P D A T E )
    ;

USER
    : ( U S E R )
    ;

VALIDATE
    : ( V A L I D A T E )
    ;

VALUES
    : ( V A L U E S )
    ;

VARCHAR
    : ( V A R C H A R )
    | ( V A R C H A R A C T E R )
    ;

VARCHAR2
    : ( V A R C H A R '2')
    ;

VIEW
    : ( V I E W )
    ;

WHENEVER
    : ( W H E N E V E R )
    ;

WHERE
    : ( W H E R E )
    ;

REDUNDANCY
    : R E D U N D A N C Y
    ;

WITH
    : ( W I T H )
    ;

STANDBY
    : S T A N D B Y
    ;

WITHOUT
    : W I T H O U T
    ;

WITHIN
    : ( W I T H I N )
    ;

ACCESSIBLE
    : ( A C C E S S I B L E )
    ;

AGAINST
    : ( A G A I N S T )
    ;

ALWAYS
    : ( A L W A Y S )
    ;

ANALYZE
    : ( A N A L Y Z E )
    ;

ASENSITIVE
    : ( A S E N S I T I V E )
    ;

BEFORE
    : ( B E F O R E )
    ;

BINARY
    : ( B I N A R Y )
    ;

BOTH
    : ( B O T H )
    ;

BULK
    : ( B U L K )
    ;

CALL
    : ( C A L L )
    ;

CASCADE
    : ( C A S C A D E )
    ;

CASE
    : ( C A S E )
    ;

CHANGE
    : ( C H A N G E )
    ;

CHARACTER
    : ( C H A R A C T E R )
    ;

CONSTRAINT
    : ( C O N S T R A I N T )
    ;

CONTINUE
    : ( C O N T I N U E )
    ;

COLLATE
    : ( C O L L A T E )
    ;

COLLECT
    : ( C O L L E C T )
    ;

CROSS
    : ( C R O S S )
    ;

CYCLE
    : ( C Y C L E )
    ;

CURRENT_DATE
    : ( C U R R E N T '_' D A T E )
    ;

CURRENT_TIMESTAMP
    : ( C U R R E N T '_' T I M E S T A M P )
    ;

CURRENT_USER
    : ( C U R R E N T '_' U S E R )
    ;

CURSOR
    : ( C U R S O R )
    ;

DAY_HOUR
    : ( D A Y '_' H O U R )
    ;

DAY_MICROSECOND
    : ( D A Y '_' M I C R O S E C O N D )
    ;

DAY_MINUTE
    : ( D A Y '_' M I N U T E )
    ;

DAY_SECOND
    : ( D A Y '_' S E C O N D )
    ;

DATABASE
    : ( D A T A B A S E )
    ;

DATABASES
    : ( D A T A B A S E S )
    ;

DEC
    : ( D E C )
    ;

DECLARE
    : ( D E C L A R E )
    ;

DELAYED
    : ( D E L A Y E D )
    ;

DISTINCTROW
    : ( D I S T I N C T R O W )
    ;

DOUBLE
    : ( D O U B L E )
    ;

DUAL
    : ( D U A L )
    | ('SYS.DUAL')
    ;

EACH
    : ( E A C H )
    ;

ENCLOSED
    : ( E N C L O S E D )
    ;

ELSEIF
    : ( E L S E I F )
    ;

ESCAPED
    : ( E S C A P E D )
    ;

ALLOW
    : A L L O W
    ;

EXIT
    : ( E X I T )
    ;

EXPLAIN
    : ( E X P L A I N )
    ;

FETCH
    : ( F E T C H )
    ;

EVALNAME
    : E V A L N A M E
    ;

FLOAT4
    : ( F L O A T '4')
    ;

FLOAT8
    : ( F L O A T '8')
    ;

FORCE
    : ( F O R C E )
    ;

FULL
    : ( F U L L )
    ;

GET
    : ( G E T )
    ;

GENERATED
    : ( G E N E R A T E D )
    ;

HIGH_PRIORITY
    : ( H I G H '_' P R I O R I T Y )
    ;

HOUR_MICROSECOND
    : ( H O U R '_' M I C R O S E C O N D )
    ;

HOUR_MINUTE
    : ( H O U R '_' M I N U T E )
    ;

HOUR_SECOND
    : ( H O U R '_' S E C O N D )
    ;

ID
    : ( I D )
    ;

IF
    : ( I F )
    ;

IFIGNORE
    : ( I F I G N O R E )
    ;

INNER
    : ( I N N E R )
    ;

INFILE
    : ( I N F I L E )
    ;

INOUT
    : ( I N O U T )
    ;

INSENSITIVE
    : ( I N S E N S I T I V E )
    ;

INT1
    : ( I N T '1')
    ;

INT2
    : ( I N T '2')
    ;

LIB
    : L I B
    ;

INT3
    : ( I N T '3')
    ;

INT4
    : ( I N T '4')
    ;

INT8
    : ( I N T '8')
    ;

INTERVAL
    : ( I N T E R V A L )
    ;

IO_AFTER_GTIDS
    : ( I O '_' A F T E R '_' G T I D S )
    ;

IO_BEFORE_GTIDS
    : ( I O '_' B E F O R E '_' G T I D S )
    ;

ITERATE
    : ( I T E R A T E )
    ;

JOIN
    : ( J O I N )
    ;

KEYS
    : ( K E Y S )
    ;

KILL
    : ( K I L L )
    ;

LANGUAGE
    : ( L A N G U A G E )
    ;

LEADING
    : ( L E A D I N G )
    ;

LEAVE
    : ( L E A V E )
    ;

LEFT
    : ( L E F T )
    ;

LINEAR
    : ( L I N E A R )
    ;

LINES
    : ( L I N E S )
    ;

BADFILE
    : B A D F I L E
    ;

LOG_DISK_SIZE
    : L O G '_' D I S K '_' S I Z E
    ;

LOAD
    : ( L O A D )
    ;

LOCALTIMESTAMP
    : ( L O C A L T I M E S T A M P )
    ;

LONGBLOB
    : ( L O N G B L O B )
    ;

SWITCHOVER
    : S W I T C H O V E R
    ;

LONGTEXT
    : ( L O N G T E X T )
    ;

LOOP
    : ( L O O P )
    ;

LOW_PRIORITY
    : ( L O W '_' P R I O R I T Y )
    ;

MASTER_BIND
    : ( M A S T E R '_' B I N D )
    ;

MASTER_SSL_VERIFY_SERVER_CERT
    : ( M A S T E R '_' S S L '_' V E R I F Y '_' S E R V E R '_' C E R T )
    ;

MATCH
    : ( M A T C H )
    ;

MAXVALUE
    : ( M A X V A L U E )
    ;

MEDIUMBLOB
    : ( M E D I U M B L O B )
    ;

MEDIUMINT
    : ( M E D I U M I N T )
    ;

MERGE
    : ( M E R G E )
    ;

REJECT
    : R E J E C T
    ;

MEDIUMTEXT
    : ( M E D I U M T E X T )
    ;

MIDDLEINT
    : ( M I D D L E I N T )
    ;

MINUTE_MICROSECOND
    : ( M I N U T E '_' M I C R O S E C O N D )
    ;

MINUTE_SECOND
    : ( M I N U T E '_' S E C O N D )
    ;

MOD
    : ( M O D )
    ;

MODIFIES
    : ( M O D I F I E S )
    ;

MOVEMENT
    : ( M O V E M E N T )
    ;

NATURAL
    : ( N A T U R A L )
    ;

NOCYCLE
    : ( N O C Y C L E )
    ;

NO_WRITE_TO_BINLOG
    : ( N O '_' W R I T E '_' T O '_' B I N L O G )
    ;

NUMERIC
    : ( N U M E R I C )
    ;

OPTIMIZE
    : ( O P T I M I Z E )
    ;

OPTIONALLY
    : ( O P T I O N A L L Y )
    ;

OUT
    : ( O U T )
    ;

OUTER
    : ( O U T E R )
    ;

IOPS_WEIGHT
    : I O P S '_' W E I G H T
    ;

OUTFILE
    : ( O U T F I L E )
    ;

PARSER
    : ( P A R S E R )
    ;

PROCEDURE
    : ( P R O C E D U R E )
    ;

PURGE
    : ( P U R G E )
    ;

PARTITION
    : ( P A R T I T I O N )
    ;

RANGE
    : ( R A N G E )
    ;

PLUS
    : P L U S
    ;

READ
    : ( R E A D )
    ;

READ_WRITE
    : ( R E A D '_' W R I T E )
    ;

READS
    : ( R E A D S )
    ;

RELEASE
    : ( R E L E A S E )
    ;

REFERENCES
    : ( R E F E R E N C E S )
    ;

REPLACE
    : ( R E P L A C E )
    ;

REPEAT
    : ( R E P E A T )
    ;

REQUIRE
    : ( R E Q U I R E )
    ;

RESIGNAL
    : ( R E S I G N A L )
    ;

RESTRICT
    : ( R E S T R I C T )
    ;

RETURN
    : ( R E T U R N )
    ;

RIGHT
    : ( R I G H T )
    ;

SCALARS
    : S C A L A R S
    ;

SECOND_MICROSECOND
    : ( S E C O N D '_' M I C R O S E C O N D )
    ;

SCHEMA
    : ( S C H E M A )
    ;

SCHEMAS
    : ( S C H E M A S )
    ;

SEPARATOR
    : ( S E P A R A T O R )
    ;

SENSITIVE
    : ( S E N S I T I V E )
    ;

SIGNAL
    : ( S I G N A L )
    ;

SPATIAL
    : ( S P A T I A L )
    ;

SPECIFIC
    : ( S P E C I F I C )
    ;

SQL
    : ( S Q L )
    ;

SQLEXCEPTION
    : ( S Q L E X C E P T I O N )
    ;

SQLSTATE
    : ( S Q L S T A T E )
    ;

SQLWARNING
    : ( S Q L W A R N I N G )
    ;

SQL_BIG_RESULT
    : ( S Q L '_' B I G '_' R E S U L T )
    ;

SQL_CALC_FOUND_ROWS
    : ( S Q L '_' C A L C '_' F O U N D '_' R O W S )
    ;

SQL_SMALL_RESULT
    : ( S Q L '_' S M A L L '_' R E S U L T )
    ;

SEARCH
    : ( S E A R C H )
    ;

SSL
    : ( S S L )
    ;

STARTING
    : ( S T A R T I N G )
    ;

STATEMENTS
    : ( S T A T E M E N T S )
    ;

STORED
    : ( S T O R E D )
    ;

STRAIGHT_JOIN
    : ( S T R A I G H T '_' J O I N )
    ;

TERMINATED
    : ( T E R M I N A T E D )
    ;

TINYBLOB
    : ( T I N Y B L O B )
    ;

TINYTEXT
    : ( T I N Y T E X T )
    ;

TABLEGROUP
    : ( T A B L E G R O U P )
    ;

TRAILING
    : ( T R A I L I N G )
    ;

TIMEZONE_HOUR
    : ( T I M E Z O N E '_' H O U R )
    ;

TIMEZONE_MINUTE
    : ( T I M E Z O N E '_' M I N U T E )
    ;

TIMEZONE_REGION
    : ( T I M E Z O N E '_' R E G I O N )
    ;

TIMEZONE_ABBR
    : ( T I M E Z O N E '_' A B B R )
    ;

UNDO
    : ( U N D O )
    ;

UNLOCK
    : ( U N L O C K )
    ;

LINE_DELIMITER
    : L I N E '_' D E L I M I T E R
    ;

USE
    : ( U S E )
    ;

USING
    : ( U S I N G )
    ;

UTC_DATE
    : ( U T C '_' D A T E )
    ;

UTC_TIMESTAMP
    : ( U T C '_' T I M E S T A M P )
    ;

VARYING
    : ( V A R Y I N G )
    ;

VIRTUAL
    : ( V I R T U A L )
    ;

WHEN
    : ( W H E N )
    ;

WHILE
    : ( W H I L E )
    ;

WRITE
    : ( W R I T E )
    ;

XOR
    : ( X O R )
    ;

X509
    : ( X '5''0''9')
    ;

YEAR_MONTH
    : ( Y E A R '_' M O N T H )
    ;

ZEROFILL
    : ( Z E R O F I L L )
    ;

GLOBAL_ALIAS
    : ('@''@' G L O B A L )
    ;

SESSION_ALIAS
    : ('@''@' S E S S I O N )
    | ('@''@' L O C A L )
    ;

STRONG
    : ( S T R O N G )
    ;

WEAK
    : ( W E A K )
    ;

FROZEN
    : ( F R O Z E N )
    ;

EXCEPT
    : ( E X C E P T )
    ;

ISNULL
    : ( I S N U L L )
    ;

RETURNING
    : ( R E T U R N I N G )
    ;

ISOPEN
    : ( I S O P E N )
    ;

ROWCOUNT
    : ( R O W C O U N T )
    ;

BULK_ROWCOUNT
    : ( B U L K '_' R O W C O U N T )
    ;

ERROR_INDEX
    : ( E R R O R '_' I N D E X )
    ;

BULK_EXCEPTIONS
    : ( B U L K '_' E X C E P T I O N S )
    ;

PARAM_ASSIGN_OPERATOR
    : ('=>')
    ;

COLUMN_OUTER_JOIN_SYMBOL
    : (('('|[\uff08])([ \t\n\r\f]+|('--'(~[\n\r])*))*'+'([ \t\n\r\f]+|('--'(~[\n\r])*))*(')'|[\uff09]))
    ;

DATA_TABLE_ID
    : ( D A T A '_' T A B L E '_' I D )
    ;

WARNINGS
    : W A R N I N G S
    ;

LOWER_JOIN
    : L O W E R '_' J O I N
    ;

GROUPS
    : G R O U P S
    ;

CONNECT_BY_ISCYCLE
    : C O N N E C T '_' B Y '_' I S C Y C L E
    ;

FORMAT
    : F O R M A T
    ;

MINVALUE
    : M I N V A L U E
    ;

EXTRA
    : E X T R A
    ;

EMPTY_FIELD_AS_NULL
    : E M P T Y '_' F I E L D '_' A S '_' N U L L
    ;

UNINSTALL
    : U N I N S T A L L
    ;

UNDOFILE
    : U N D O F I L E
    ;

MASTER_SSL_CA
    : M A S T E R '_' S S L '_' C A
    ;

YEAR
    : Y E A R
    ;

STOP
    : S T O P
    ;

STORAGE_FORMAT_WORK_VERSION
    : S T O R A G E '_' F O R M A T '_' W O R K '_' V E R S I O N
    ;

DISABLE_PARALLEL_DML
    : D I S A B L E '_' P A R A L L E L '_' D M L
    ;

PACKAGE_KEY
    : P A C K A G E '_' K E Y
    ;

PACKAGE
    : P A C K A G E
    ;

AT
    : A T
    ;

RELAY_LOG_POS
    : R E L A Y '_' L O G '_' P O S
    ;

POOL
    : P O O L
    ;

ZONE_TYPE
    : Z O N E '_' T Y P E
    ;

LOCATION
    : L O C A T I O N
    ;

WEIGHT_STRING
    : W E I G H T '_' S T R I N G
    ;

MAXLOGMEMBERS
    : M A X L O G M E M B E R S
    ;

CHANGED
    : C H A N G E D
    ;

MASTER_SSL_CAPATH
    : M A S T E R '_' S S L '_' C A P A T H
    ;

PRECISION
    : P R E C I S I O N
    ;

ROLE
    : R O L E
    ;

JSON_QUERY
    : J S O N '_' Q U E R Y
    ;

REWRITE_MERGE_VERSION
    : R E W R I T E '_' M E R G E '_' V E R S I O N
    ;

NTH_VALUE
    : N T H '_' V A L U E
    ;

SERIAL
    : S E R I A L
    ;

REDACTION
    : R E D A C T I O N
    ;

QUEUE_TIME
    : Q U E U E '_' T I M E
    ;

PROGRESSIVE_MERGE_NUM
    : P R O G R E S S I V E '_' M E R G E '_' N U M
    ;

TABLET_MAX_SIZE
    : T A B L E T '_' M A X '_' S I Z E
    ;

ILOGCACHE
    : I L O G C A C H E
    ;

AUTHORS
    : A U T H O R S
    ;

MIGRATE
    : M I G R A T E
    ;

DIV
    : D I V
    ;

CONSISTENT
    : C O N S I S T E N T
    ;

SUSPEND
    : S U S P E N D
    ;

SYSKM
    : S Y S K M
    ;

REMOTE_OSS
    : R E M O T E '_' O S S
    ;

SECURITY
    : S E C U R I T Y
    ;

SET_SLAVE_CLUSTER
    : S E T '_' S L A V E '_' C L U S T E R
    ;

FAST
    : F A S T
    ;

KEYSTORE
    : K E Y S T O R E
    ;

TRUNCATE
    : T R U N C A T E
    ;

CONSTRAINT_SCHEMA
    : C O N S T R A I N T '_' S C H E M A
    ;

MASTER_SSL_CERT
    : M A S T E R '_' S S L '_' C E R T
    ;

TABLE_NAME
    : T A B L E '_' N A M E
    ;

PRIORITY
    : P R I O R I T Y
    ;

DO
    : D O
    ;

MASTER_RETRY_COUNT
    : M A S T E R '_' R E T R Y '_' C O U N T
    ;

EXCEPTIONS
    : E X C E P T I O N S
    ;

REPLICA
    : R E P L I C A
    ;

KILL_EXPR
    : K I L L '_' E X P R
    ;

UROWID
    : U R O W I D
    ;

ADMIN
    : A D M I N
    ;

CONNECT_BY_ISLEAF
    : C O N N E C T '_' B Y '_' I S L E A F
    ;

NCHAR_CS
    : N C H A R '_' C S
    ;

OLD_KEY
    : O L D '_' K E Y
    ;

DISABLE
    : D I S A B L E
    ;

STRICT
    : S T R I C T
    ;

PORT
    : P O R T
    ;

MAXDATAFILES
    : M A X D A T A F I L E S
    ;

EXEC
    : E X E C
    ;

NOVALIDATE
    : N O V A L I D A T E
    ;

REBUILD
    : R E B U I L D
    ;

FOLLOWER
    : F O L L O W E R
    ;

LIST
    : L I S T
    ;

LOWER_OVER
    : L O W E R '_' O V E R
    ;

ROOT
    : R O O T
    ;

REDOFILE
    : R E D O F I L E
    ;

MASTER_SERVER_ID
    : M A S T E R '_' S E R V E R '_' I D
    ;

NCHAR
    : N C H A R
    ;

KEY_BLOCK_SIZE
    : K E Y '_' B L O C K '_' S I Z E
    ;

NOLOGGING
    : N O L O G G I N G
    ;

SEQUENCE
    : S E Q U E N C E
    ;

PRETTY
    : P R E T T Y
    ;

PRETTY_COLOR
    : P R E T T Y '_' C O L O R
    ;

COLUMNS
    : C O L U M N S
    ;

MIGRATION
    : M I G R A T I O N
    ;

SUBPARTITION
    : S U B P A R T I T I O N
    ;

DML
    : D M L
    ;

MYSQL_DRIVER
    : M Y S Q L '_' D R I V E R
    ;

GO
    : G O
    ;

ROW_NUMBER
    : R O W '_' N U M B E R
    ;

COMPRESSION
    : C O M P R E S S I O N
    ;

BIT
    : B I T
    ;

MAX_DISK_SIZE
    : M A X '_' D I S K '_' S I Z E
    ;

SAMPLE
    : S A M P L E
    ;

PCTUSED
    : P C T U S E D
    ;

UNLOCKED
    : U N L O C K E D
    ;

CLASS_ORIGIN
    : C L A S S '_' O R I G I N
    ;

ACTION
    : A C T I O N
    ;

REDUNDANT
    : R E D U N D A N T
    ;

MAXLOGFILES
    : M A X L O G F I L E S
    ;

UPGRADE
    : U P G R A D E
    ;

TEMPTABLE
    : T E M P T A B L E
    ;

EXTERNALLY
    : E X T E R N A L L Y
    ;

RECYCLEBIN
    : R E C Y C L E B I N
    ;

PROFILES
    : P R O F I L E S
    ;

TIMESTAMP_VALUE
    : T I M E S T A M P '_' V A L U E
    ;

ERRORS
    : E R R O R S
    ;

BINARY_DOUBLE_NAN
    : B I N A R Y '_' D O U B L E '_' N A N
    ;

LEAVES
    : L E A V E S
    ;

UNDEFINED
    : U N D E F I N E D
    ;

EVERY
    : E V E R Y
    ;

BYTE
    : B Y T E
    ;

SHARDING
    : S H A R D I N G
    ;

FLUSH
    : F L U S H
    ;

MIN_ROWS
    : M I N '_' R O W S
    ;

ERROR_P
    : E R R O R '_' P
    ;

MAX_USER_CONNECTIONS
    : M A X '_' U S E R '_' C O N N E C T I O N S
    ;

FIELDS
    : F I E L D S
    ;

MAX_CPU
    : M A X '_' C P U
    ;

LOCKED
    : L O C K E D
    ;

DOP
    : D O P
    ;

IO
    : I O
    ;

BTREE
    : B T R E E
    ;

APPROXNUM
    : A P P R O X N U M
    ;

HASH
    : H A S H
    ;

REGR_INTERCEPT
    : R E G R '_' I N T E R C E P T
    ;

OPTIMAL
    : O P T I M A L
    ;

CONNECT_BY_ROOT
    : C O N N E C T '_' B Y '_' R O O T
    ;

OLTP
    : O L T P
    ;

SYSOPER
    : S Y S O P E R
    ;

GOTO
    : G O T O
    ;

COLLATION
    : C O L L A T I O N
    ;

MASTER
    : M A S T E R
    ;

ENCRYPTION
    : E N C R Y P T I O N
    ;

INSERTING
    : I N S E R T I N G
    ;

MAX
    : M A X
    ;

TRANSACTION
    : T R A N S A C T I O N
    ;

SQL_TSI_MONTH
    : S Q L '_' T S I '_' M O N T H
    ;

BECOME
    : B E C O M E
    ;

IGNORE
    : I G N O R E
    ;

MAX_QUERIES_PER_HOUR
    : M A X '_' Q U E R I E S '_' P E R '_' H O U R
    ;

OFF
    : O F F
    ;

MIN_IOPS
    : M I N '_' I O P S
    ;

NVARCHAR
    : N V A R C H A R
    ;

PAUSE
    : P A U S E
    ;

QUICK
    : Q U I C K
    ;

DUPLICATE
    : D U P L I C A T E
    ;

XMLTYPE
    : X M L T Y P E
    ;

USAGE
    : U S A G E
    ;

FIELD_OPTIONALLY_ENCLOSED_BY
    : F I E L D '_' O P T I O N A L L Y '_' E N C L O S E D '_' B Y
    ;

WAIT
    : W A I T
    ;

DES_KEY_FILE
    : D E S '_' K E Y '_' F I L E
    ;

ENGINES
    : E N G I N E S
    ;

RETURNS
    : R E T U R N S
    ;

MASTER_USER
    : M A S T E R '_' U S E R
    ;

SOCKET
    : S O C K E T
    ;

SIBLINGS
    : S I B L I N G S
    ;

MASTER_DELAY
    : M A S T E R '_' D E L A Y
    ;

FILE_ID
    : F I L E '_' I D
    ;

FIRST
    : F I R S T
    ;

TABLET
    : T A B L E T
    ;

CLIENT
    : C L I E N T
    ;

PRIVATE
    : P R I V A T E
    ;

TABLES
    : T A B L E S
    ;

ENGINE_
    : E N G I N E
    ;

TRADITIONAL
    : T R A D I T I O N A L
    ;

BOOTSTRAP
    : B O O T S T R A P
    ;

STDDEV
    : S T D D E V
    ;

DATAFILE
    : D A T A F I L E
    ;

INVOKER
    : I N V O K E R
    ;

LAYER
    : L A Y E R
    ;

DEPTH
    : D E P T H
    ;

THREAD
    : T H R E A D
    ;

TRIGGERS
    : T R I G G E R S
    ;

COLUMN_NAME
    : C O L U M N '_' N A M E
    ;

ENABLE_PARALLEL_DML
    : E N A B L E '_' P A R A L L E L '_' D M L
    ;

RESET
    : R E S E T
    ;

EVENT
    : E V E N T
    ;

COALESCE
    : C O A L E S C E
    ;

RESPECT
    : R E S P E C T
    ;

STATUS
    : S T A T U S
    ;

UNBOUNDED
    : U N B O U N D E D
    ;

REGR_SLOPE
    : R E G R '_' S L O P E
    ;

WRAPPER
    : W R A P P E R
    ;

TIMESTAMP
    : T I M E S T A M P
    ;

EXTENT
    : E X T E N T
    ;

PARTITIONS
    : P A R T I T I O N S
    ;

SUBSTR
    : S U B S T R
    ;

FILEX
    : F I L E X
    ;

UNIT
    : U N I T
    ;

LOWER_ON
    : L O W E R '_' O N
    ;

SWITCH
    : S W I T C H
    ;

LESS
    : L E S S
    ;

BODY
    : B O D Y
    ;

DIAGNOSTICS
    : D I A G N O S T I C S
    ;

REDO_BUFFER_SIZE
    : R E D O '_' B U F F E R '_' S I Z E
    ;

NO
    : N O
    ;

MAJOR
    : M A J O R
    ;

ACTIVE
    : A C T I V E
    ;

TIES
    : T I E S
    ;

ROUTINE
    : R O U T I N E
    ;

ROLLBACK
    : R O L L B A C K
    ;

FOLLOWING
    : F O L L O W I N G
    ;

READ_ONLY
    : R E A D '_' O N L Y
    ;

MEMBER
    : M E M B E R
    ;

PARTITION_ID
    : P A R T I T I O N '_' I D
    ;

SHARED
    : S H A R E D
    ;

EXTERNAL
    : E X T E R N A L
    ;

DUMP
    : D U M P
    ;

APPROX_COUNT_DISTINCT_SYNOPSIS
    : A P P R O X '_' C O U N T '_' D I S T I N C T '_' S Y N O P S I S
    ;

GROUPING
    : G R O U P I N G
    ;

PRIMARY
    : P R I M A R Y
    ;

ARCHIVELOG
    : A R C H I V E L O G
    ;

MATCHED
    : M A T C H E D
    ;

MAX_CONNECTIONS_PER_HOUR
    : M A X '_' C O N N E C T I O N S '_' P E R '_' H O U R
    ;

FAILED_LOGIN_ATTEMPTS
    : F A I L E D '_' L O G I N '_' A T T E M P T S
    ;

ENCODING
    : E N C O D I N G
    ;

SECOND
    : S E C O N D
    ;

UNKNOWN
    : U N K N O W N
    ;

POINT
    : P O I N T
    ;

MEMSTORE_PERCENT
    : M E M S T O R E '_' P E R C E N T
    ;

POLYGON
    : P O L Y G O N
    ;

ORA_ROWSCN
    : O R A '_' R O W S C N
    ;

OLD
    : O L D
    ;

TABLE_ID
    : T A B L E '_' I D
    ;

CONTEXT
    : C O N T E X T
    ;

FINAL_COUNT
    : F I N A L '_' C O U N T
    ;

MASTER_CONNECT_RETRY
    : M A S T E R '_' C O N N E C T '_' R E T R Y
    ;

POSITION
    : P O S I T I O N
    ;

DISCARD
    : D I S C A R D
    ;

PATTERN
    : P A T T E R N
    ;

RECOVERY_WINDOW
    : R E C O V E R Y '_' W I N D O W
    ;

RECOVER
    : R E C O V E R
    ;

PREV
    : P R E V
    ;

PROCESS
    : P R O C E S S
    ;

ERROR
    : E R R O R
    ;

DEALLOCATE
    : D E A L L O C A T E
    ;

OLD_PASSWORD
    : O L D '_' P A S S W O R D
    ;

CONTROLFILE
    : C O N T R O L F I L E
    ;

LISTAGG
    : L I S T A G G
    ;

SLOW
    : S L O W
    ;

SUM
    : S U M
    ;

OPTIONS
    : O P T I O N S
    ;

MIN
    : M I N
    ;

ROLES
    : R O L E S
    ;

UPDATING
    : U P D A T I N G
    ;

KEY
    : K E Y
    ;

RT
    : R T
    ;

RELOAD
    : R E L O A D
    ;

ONE
    : O N E
    ;

DELAY_KEY_WRITE
    : D E L A Y '_' K E Y '_' W R I T E
    ;

ORIG_DEFAULT
    : O R I G '_' D E F A U L T
    ;

INDEXED
    : I N D E X E D
    ;

RLIKE
    : R L I K E
    ;

SQL_TSI_HOUR
    : S Q L '_' T S I '_' H O U R
    ;

TIMESTAMPDIFF
    : T I M E S T A M P D I F F
    ;

RESTORE
    : R E S T O R E
    ;

OFFSET
    : O F F S E T
    ;

TEMPORARY
    : T E M P O R A R Y
    ;

VARIANCE
    : V A R I A N C E
    ;

SNAPSHOT
    : S N A P S H O T
    ;

JSON_EXISTS
    : J S O N '_' E X I S T S
    ;

STATISTICS
    : S T A T I S T I C S
    ;

COBOL
    : C O B O L
    ;

SERVER_TYPE
    : S E R V E R '_' T Y P E
    ;

COMMITTED
    : C O M M I T T E D
    ;

PERCENT
    : P E R C E N T
    ;

RATIO_TO_REPORT
    : R A T I O '_' T O '_' R E P O R T
    ;

SUBJECT
    : S U B J E C T
    ;

DBTIMEZONE
    : D B T I M E Z O N E
    ;

INDEXES
    : I N D E X E S
    ;

FREEZE
    : F R E E Z E
    ;

SCOPE
    : S C O P E
    ;

OUTLINE_DEFAULT_TOKEN
    : O U T L I N E '_' D E F A U L T '_' T O K E N
    ;

IDC
    : I D C
    ;

SYS_CONNECT_BY_PATH
    : S Y S '_' C O N N E C T '_' B Y '_' P A T H
    ;

ONE_SHOT
    : O N E '_' S H O T
    ;

ACCOUNT
    : A C C O U N T
    ;

LOCALITY
    : L O C A L I T Y
    ;

ARCHIVE
    : A R C H I V E
    ;

CONSTRAINTS
    : C O N S T R A I N T S
    ;

REVERSE
    : R E V E R S E
    ;

CLUSTER_ID
    : C L U S T E R '_' I D
    ;

NOARCHIVELOG
    : N O A R C H I V E L O G
    ;

WM_CONCAT
    : W M '_' C O N C A T
    ;

MAX_SIZE
    : M A X '_' S I Z E
    ;

COVAR_SAMP
    : C O V A R '_' S A M P
    ;

PAGE
    : P A G E
    ;

NAME
    : N A M E
    ;

ADMINISTER
    : A D M I N I S T E R
    ;

ROW_COUNT
    : R O W '_' C O U N T
    ;

LAST
    : L A S T
    ;

LOGONLY_REPLICA_NUM
    : L O G O N L Y '_' R E P L I C A '_' N U M
    ;

DELAY
    : D E L A Y
    ;

SUBDATE
    : S U B D A T E
    ;

INCREMENTAL
    : I N C R E M E N T A L
    ;

QUOTA
    : Q U O T A
    ;

VERIFY
    : V E R I F Y
    ;

CONTAINS
    : C O N T A I N S
    ;

GENERAL
    : G E N E R A L
    ;

VISIBLE
    : V I S I B L E
    ;

REGR_COUNT
    : R E G R '_' C O U N T
    ;

SIGNED
    : S I G N E D
    ;

SERVER
    : S E R V E R
    ;

NEXT
    : N E X T
    ;

GLOBAL
    : G L O B A L
    ;

ENDS
    : E N D S
    ;

ROOTSERVICE_LIST
    : R O O T S E R V I C E '_' L I S T
    ;

SHOW
    : S H O W
    ;

SHUTDOWN
    : S H U T D O W N
    ;

VERBOSE
    : V E R B O S E
    ;

JSON_EQUAL
    : J S O N '_' E Q U A L
    ;

CLUSTER_NAME
    : C L U S T E R '_' N A M E
    ;

MASTER_PORT
    : M A S T E R '_' P O R T
    ;

MYSQL_ERRNO
    : M Y S Q L '_' E R R N O
    ;

XA
    : X A
    ;

TIME
    : T I M E
    ;

REUSE
    : R E U S E
    ;

NOMINVALUE
    : N O M I N V A L U E
    ;

DATETIME
    : D A T E T I M E
    ;

BOOL
    : B O O L
    ;

DIRECTORY
    : D I R E C T O R Y
    ;

SECTION
    : S E C T I O N
    ;

PERCENTILE_CONT
    : P E R C E N T I L E '_' C O N T
    ;

VALID
    : V A L I D
    ;

MASTER_SSL_KEY
    : M A S T E R '_' S S L '_' K E Y
    ;

MASTER_PASSWORD
    : M A S T E R '_' P A S S W O R D
    ;

PLAN
    : P L A N
    ;

MULTIPOLYGON
    : M U L T I P O L Y G O N
    ;

STDDEV_SAMP
    : S T D D E V '_' S A M P
    ;

USE_BLOOM_FILTER
    : U S E '_' B L O O M '_' F I L T E R
    ;

LOCAL
    : L O C A L
    ;

CONSTRAINT_CATALOG
    : C O N S T R A I N T '_' C A T A L O G
    ;

DICTIONARY
    : D I C T I O N A R Y
    ;

SYSDBA
    : S Y S D B A
    ;

EXCHANGE
    : E X C H A N G E
    ;

GRANTS
    : G R A N T S
    ;

CAST
    : C A S T
    ;

SERVER_PORT
    : S E R V E R '_' P O R T
    ;

SQL_CACHE
    : S Q L '_' C A C H E
    ;

MAX_USED_PART_ID
    : M A X '_' U S E D '_' P A R T '_' I D
    ;

RELY
    : R E L Y
    ;

INSTANCE
    : I N S T A N C E
    ;

HYBRID_HIST
    : H Y B R I D '_' H I S T
    ;

FUNCTION
    : F U N C T I O N
    ;

NAN_VALUE
    : N A N '_' V A L U E
    ;

INVISIBLE
    : I N V I S I B L E
    ;

BINARY_DOUBLE_INFINITY
    : B I N A R Y '_' D O U B L E '_' I N F I N I T Y
    ;

REGR_SXX
    : R E G R '_' S X X
    ;

REGR_SXY
    : R E G R '_' S X Y
    ;

DENSE_RANK
    : D E N S E '_' R A N K
    ;

COUNT
    : C O U N T
    ;

SQL_CALC_FOUND_ROW
    : S Q L '_' C A L C '_' F O U N D '_' R O W
    ;

TREAT
    : T R E A T
    ;

TYPENAME
    : T Y P E N A M E
    ;

MY_NAME
    : M Y '_' N A M E
    ;

NAMES
    : N A M E S
    ;

LOWER_THAN_NEG
    : L O W E R '_' T H A N '_' N E G
    ;

MAX_ROWS
    : M A X '_' R O W S
    ;

ISOLATION
    : I S O L A T I O N
    ;

REPLICATION
    : R E P L I C A T I O N
    ;

REGR_SYY
    : R E G R '_' S Y Y
    ;

INITIALIZED
    : I N I T I A L I Z E D
    ;

REMOVE
    : R E M O V E
    ;

STATS_AUTO_RECALC
    : S T A T S '_' A U T O '_' R E C A L C
    ;

CONSISTENT_MODE
    : C O N S I S T E N T '_' M O D E
    ;

SUBMULTISET
    : S U B M U L T I S E T
    ;

SEGMENT
    : S E G M E N T
    ;

UNCOMMITTED
    : U N C O M M I T T E D
    ;

CURRENT_SCHEMA
    : C U R R E N T '_' S C H E M A
    ;

OWN
    : O W N
    ;

NO_WAIT
    : N O '_' W A I T
    ;

BACKUP_COPIES
    : B A C K U P '_' C O P I E S
    ;

UNIT_NUM
    : U N I T '_' N U M
    ;

PERCENTAGE
    : P E R C E N T A G E
    ;

MAX_IOPS
    : M A X '_' I O P S
    ;

SPFILE
    : S P F I L E
    ;

REPEATABLE
    : R E P E A T A B L E
    ;

PCTINCREASE
    : P C T I N C R E A S E
    ;

COMPLETION
    : C O M P L E T I O N
    ;

ROOTTABLE
    : R O O T T A B L E
    ;

ZONE
    : Z O N E
    ;

REGR_AVGY
    : R E G R '_' A V G Y
    ;

REGR_AVGX
    : R E G R '_' A V G X
    ;

TEMPLATE
    : T E M P L A T E
    ;

INCLUDING
    : I N C L U D I N G
    ;

DATE_SUB
    : D A T E '_' S U B
    ;

EXPIRE_INFO
    : E X P I R E '_' I N F O
    ;

EXPIRE
    : E X P I R E
    ;

KEEP
    : K E E P
    ;

ENABLE
    : E N A B L E
    ;

HOSTS
    : H O S T S
    ;

SCHEMA_NAME
    : S C H E M A '_' N A M E
    ;

SHRINK
    : S H R I N K
    ;

EXPANSION
    : E X P A N S I O N
    ;

REORGANIZE
    : R E O R G A N I Z E
    ;

TRIM_SPACE
    : T R I M '_' S P A C E
    ;

BLOCK_SIZE
    : B L O C K '_' S I Z E
    ;

INNER_PARSE
    : I N N E R '_' P A R S E
    ;

MINOR
    : M I N O R
    ;

RESTRICTED
    : R E S T R I C T E D
    ;

GLOBALLY
    : G L O B A L L Y
    ;

RESUME
    : R E S U M E
    ;

INT
    : I N T
    ;

STATS_PERSISTENT
    : S T A T S '_' P E R S I S T E N T
    ;

NODEGROUP
    : N O D E G R O U P
    ;

PARTITIONING
    : P A R T I T I O N I N G
    ;

MAXTRANS
    : M A X T R A N S
    ;

SUPER
    : S U P E R
    ;

JSON_OBJECT
    : J S O N '_' O B J E C T
    ;

COMMIT
    : C O M M I T
    ;

DETERMINISTIC
    : D E T E R M I N I S T I C
    ;

SAVEPOINT
    : S A V E P O I N T
    ;

UNTIL
    : U N T I L
    ;

NVARCHAR2
    : N V A R C H A R '2'
    ;

MEMTABLE
    : M E M T A B L E
    ;

CHARSET
    : C H A R S E T
    ;

FREELIST
    : F R E E L I S T
    ;

MOVE
    : M O V E
    ;

XML
    : X M L
    ;

PASSWORD_LIFE_TIME
    : P A S S W O R D '_' L I F E '_' T I M E
    ;

IPC
    : I P C
    ;

PATH
    : P A T H
    ;

TRIM
    : T R I M
    ;

RANK
    : R A N K
    ;

VAR_POP
    : V A R '_' P O P
    ;

DEFAULT_AUTH
    : D E F A U L T '_' A U T H
    ;

EXTENT_SIZE
    : E X T E N T '_' S I Z E
    ;

BINLOG
    : B I N L O G
    ;

CLOG
    : C L O G
    ;

GEOMETRYCOLLECTION
    : G E O M E T R Y C O L L E C T I O N
    ;

STORAGE
    : S T O R A G E
    ;

MEDIUM
    : M E D I U M
    ;

XMLPARSE
    : X M L P A R S E
    ;

WELLFORMED
    : W E L L F O R M E D
    ;

DOCUMENT
    : D O C U M E N T
    ;

XMLAGG
    : X M L A G G
    ;

USE_FRM
    : U S E '_' F R M
    ;

CLIENT_VERSION
    : C L I E N T '_' V E R S I O N
    ;

MASTER_HEARTBEAT_PERIOD
    : M A S T E R '_' H E A R T B E A T '_' P E R I O D
    ;

DELETING
    : D E L E T I N G
    ;

SUBPARTITIONS
    : S U B P A R T I T I O N S
    ;

CUBE
    : C U B E
    ;

REGR_R2
    : R E G R '_' R '2'
    ;

BALANCE
    : B A L A N C E
    ;

POLICY
    : P O L I C Y
    ;

QUERY
    : Q U E R Y
    ;

THROTTLE
    : T H R O T T L E
    ;

SQL_TSI_QUARTER
    : S Q L '_' T S I '_' Q U A R T E R
    ;

SPACE
    : S P A C E
    ;

REPAIR
    : R E P A I R
    ;

MASTER_SSL_CIPHER
    : M A S T E R '_' S S L '_' C I P H E R
    ;

KEY_VERSION
    : K E Y '_' V E R S I O N
    ;

CATALOG_NAME
    : C A T A L O G '_' N A M E
    ;

NDBCLUSTER
    : N D B C L U S T E R
    ;

CONNECTION
    : C O N N E C T I O N
    ;

COMPACT
    : C O M P A C T
    ;

INCR
    : I N C R
    ;

CANCEL
    : C A N C E L
    ;

SIMPLE
    : S I M P L E
    ;

BEGIN
    : B E G I N
    ;

VARIABLES
    : V A R I A B L E S
    ;

FREELISTS
    : F R E E L I S T S
    ;

SQL_TSI_WEEK
    : S Q L '_' T S I '_' W E E K
    ;

SYSTEM
    : S Y S T E M
    ;

SQLERROR
    : S Q L E R R O R
    ;

ROOTSERVICE
    : R O O T S E R V I C E
    ;

PLUGIN_DIR
    : P L U G I N '_' D I R
    ;

ASCII
    : A S C I I
    ;

INFO
    : I N F O
    ;

SQL_THREAD
    : S Q L '_' T H R E A D
    ;

SKIP_HEADER
    : S K I P '_' H E A D E R
    ;

TYPES
    : T Y P E S
    ;

LEADER
    : L E A D E R
    ;

LOWER_KEY
    : L O W E R '_' K E Y
    ;

FOUND
    : F O U N D
    ;

EXTRACT
    : E X T R A C T
    ;

PERCENTILE_DISC
    : P E R C E N T I L E '_' D I S C
    ;

XMLCAST
    : X M L C A S T
    ;

XMLSERIALIZE
    : X M L S E R I A L I Z E
    ;

FIXED
    : F I X E D
    ;

CACHE
    : C A C H E
    ;

RETURNED_SQLSTATE
    : R E T U R N E D '_' S Q L S T A T E
    ;

END
    : E N D
    ;

PRESERVE
    : P R E S E R V E
    ;

ASIS
    : A S I S
    ;

SQL_BUFFER_RESULT
    : S Q L '_' B U F F E R '_' R E S U L T
    ;

LOCK_
    : L O C K
    ;

JSON
    : J S O N
    ;

SOME
    : S O M E
    ;

INDEX_TABLE_ID
    : ( I N D E X '_' T A B L E '_' I D )
    ;

FREQUENCY
    : F R E Q U E N C Y
    ;

PQ_MAP
    : P Q '_' M A P
    ;

MANUAL
    : M A N U A L
    ;

LOCKS
    : L O C K S
    ;

SYSBACKUP
    : S Y S B A C K U P
    ;

GEOMETRY
    : G E O M E T R Y
    ;

NO_PARALLEL
    : N O '_' P A R A L L E L
    ;

STORAGE_FORMAT_VERSION
    : S T O R A G E '_' F O R M A T '_' V E R S I O N
    ;

VERSION
    : V E R S I O N
    ;

INDENT
    : I N D E N T
    ;

ISOLATION_LEVEL
    : I S O L A T I O N '_' L E V E L
    ;

OVER
    : O V E R
    ;

MAX_SESSION_NUM
    : M A X '_' S E S S I O N '_' N U M
    ;

USER_RESOURCES
    : U S E R '_' R E S O U R C E S
    ;

DESTINATION
    : D E S T I N A T I O N
    ;

SONAME
    : S O N A M E
    ;

OUTLINE
    : O U T L I N E
    ;

MASTER_LOG_FILE
    : M A S T E R '_' L O G '_' F I L E
    ;

WMSYS
    : W M S Y S
    ;

NOMAXVALUE
    : N O M A X V A L U E
    ;

ESTIMATE
    : E S T I M A T E
    ;

SLAVE
    : S L A V E
    ;

GTS
    : G T S
    ;

SKIP_BLANK_LINES
    : S K I P '_' B L A N K '_' L I N E S
    ;

EXPORT
    : E X P O R T
    ;

TEXT
    : T E X T
    ;

AVG_ROW_LENGTH
    : A V G '_' R O W '_' L E N G T H
    ;

FLASHBACK
    : F L A S H B A C K
    ;

SESSION_USER
    : S E S S I O N '_' U S E R
    ;

TABLEGROUPS
    : T A B L E G R O U P S
    ;

REPLICA_TYPE
    : R E P L I C A '_' T Y P E
    ;

AGGREGATE
    : A G G R E G A T E
    ;

PERCENT_RANK
    : P E R C E N T '_' R A N K
    ;

ENUM
    : E N U M
    ;

NATIONAL
    : N A T I O N A L
    ;

RECYCLE
    : R E C Y C L E
    ;

REGION
    : R E G I O N
    ;

MATERIALIZE
    : M A T E R I A L I Z E
    ;

FORTRAN
    : F O R T R A N
    ;

MUTEX
    : M U T E X
    ;

PARALLEL
    : P A R A L L E L
    ;

NOPARALLEL
    : N O P A R A L L E L
    ;

LOWER_PARENS
    : L O W E R '_' P A R E N S
    ;

MONITOR
    : M O N I T O R
    ;

NDB
    : N D B
    ;

SYSTEM_USER
    : S Y S T E M '_' U S E R
    ;

MAX_UPDATES_PER_HOUR
    : M A X '_' U P D A T E S '_' P E R '_' H O U R
    ;

R_SKIP
    : S K I P
    ;

CONCURRENT
    : C O N C U R R E N T
    ;

DUMPFILE
    : D U M P F I L E
    ;

COMPILE
    : C O M P I L E
    ;

COMPRESSED
    : C O M P R E S S E D
    ;

LINESTRING
    : L I N E S T R I N G
    ;

EXEMPT
    : E X E M P T
    ;

DYNAMIC
    : D Y N A M I C
    ;

CHAIN
    : C H A I N
    ;

NEG
    : N E G
    ;

LAG
    : L A G
    ;

NEW
    : N E W
    ;

BASELINE_ID
    : B A S E L I N E '_' I D
    ;

HIGH
    : H I G H
    ;

LAX
    : L A X
    ;

SQL_TSI_YEAR
    : S Q L '_' T S I '_' Y E A R
    ;

THAN
    : T H A N
    ;

CPU
    : C P U
    ;

LOGS
    : L O G S
    ;

SERIALIZABLE
    : S E R I A L I Z A B L E
    ;

DBA_RECYCLEBIN
    : D B A '_' R E C Y C L E B I N
    ;

BACKUP
    : B A C K U P
    ;

LOGFILE
    : L O G F I L E
    ;

ROW_FORMAT
    : R O W '_' F O R M A T
    ;

ALLOCATE
    : A L L O C A T E
    ;

SET_MASTER_CLUSTER
    : S E T '_' M A S T E R '_' C L U S T E R
    ;

MAXLOGHISTORY
    : M A X L O G H I S T O R Y
    ;

MINUTE
    : M I N U T E
    ;

SWAPS
    : S W A P S
    ;

RESETLOGS
    : R E S E T L O G S
    ;

DESCRIBE
    : D E S C R I B E
    ;

NORESETLOGS
    : N O R E S E T L O G S
    ;

TASK
    : T A S K
    ;

IO_THREAD
    : I O '_' T H R E A D
    ;

BC2HOST
    : B C '2' H O S T
    ;

PARAMETERS
    : P A R A M E T E R S
    ;

OBJECT
    : O B J E C T
    ;

TABLESPACE
    : T A B L E S P A C E
    ;

AUTO
    : A U T O
    ;

REGEXP_LIKE
    : R E G E X P '_' L I K E
    ;

MODULE
    : M O D U L E
    ;

PASSWORD
    : P A S S W O R D
    ;

SQLCODE
    : S Q L C O D E
    ;

SORT
    : S O R T
    ;

LOWER_THAN_BY_ACCESS_SESSION
    : L O W E R '_' T H A N '_' B Y '_' A C C E S S '_' S E S S I O N
    ;

MESSAGE_TEXT
    : M E S S A G E '_' T E X T
    ;

DISK
    : D I S K
    ;

FAULTS
    : F A U L T S
    ;

HOUR
    : H O U R
    ;

REFRESH
    : R E F R E S H
    ;

COLUMN_STAT
    : C O L U M N '_' S T A T
    ;

PLI
    : P L I
    ;

UNIT_GROUP
    : U N I T '_' G R O U P
    ;

ERROR_CODE
    : E R R O R '_' C O D E
    ;

UPDATEXML
    : U P D A T E X M L
    ;

PHASE
    : P H A S E
    ;

PROFILE
    : P R O F I L E
    ;

NORELY
    : N O R E L Y
    ;

LAST_VALUE
    : L A S T '_' V A L U E
    ;

RESTART
    : R E S T A R T
    ;

TRACE
    : T R A C E
    ;

LOGICAL_READS
    : L O G I C A L '_' R E A D S
    ;

MANAGEMENT
    : M A N A G E M E N T
    ;

DATE_ADD
    : D A T E '_' A D D
    ;

BLOCK_INDEX
    : B L O C K '_' I N D E X
    ;

DEBUG
    : D E B U G
    ;

SERVER_IP
    : S E R V E R '_' I P
    ;

SESSIONTIMEZONE
    : S E S S I O N T I M E Z O N E
    ;

CODE
    : C O D E
    ;

PLUGINS
    : P L U G I N S
    ;

ADDDATE
    : A D D D A T E
    ;

PASSWORD_LOCK_TIME
    : P A S S W O R D '_' L O C K '_' T I M E
    ;

TRANSLATE
    : T R A N S L A T E
    ;

COLUMN_FORMAT
    : C O L U M N '_' F O R M A T
    ;

MAX_MEMORY
    : M A X '_' M E M O R Y
    ;

CLEAN
    : C L E A N
    ;

NESTED
    : N E S T E D
    ;

MASTER_SSL
    : M A S T E R '_' S S L
    ;

CLEAR
    : C L E A R
    ;

SORTKEY
    : S O R T K E Y
    ;

CHECKSUM
    : C H E C K S U M
    ;

INSTALL
    : I N S T A L L
    ;

MONTH
    : M O N T H
    ;

AFTER
    : A F T E R
    ;

MAXINSTANCES
    : M A X I N S T A N C E S
    ;

CLOSE
    : C L O S E
    ;

JSON_OBJECTAGG
    : J S O N '_' O B J E C T A G G
    ;

SET_TP
    : S E T '_' T P
    ;

OWNER
    : O W N E R
    ;

BLOOM_FILTER
    : B L O O M '_' F I L T E R
    ;

ILOG
    : I L O G
    ;

META
    : M E T A
    ;

PASSWORD_VERIFY_FUNCTION
    : P A S S W O R D '_' V E R I F Y '_' F U N C T I O N
    ;

LOWEST_PARENS
    : L O W E S T '_' P A R E N S
    ;

IDENTITY
    : I D E N T I T Y
    ;

STARTS
    : S T A R T S
    ;

PLANREGRESS
    : P L A N R E G R E S S
    ;

AUTOEXTEND_SIZE
    : A U T O E X T E N D '_' S I Z E
    ;

SOURCE
    : S O U R C E
    ;

POW
    : P O W
    ;

IGNORE_SERVER_IDS
    : I G N O R E '_' S E R V E R '_' I D S
    ;

REPLICA_NUM
    : R E P L I C A '_' N U M
    ;

LOWER_THAN_COMP
    : L O W E R '_' T H A N '_' C O M P
    ;

BINDING
    : B I N D I N G
    ;

MICROSECOND
    : M I C R O S E C O N D
    ;

INDICATOR
    : I N D I C A T O R
    ;

UNDO_BUFFER_SIZE
    : U N D O '_' B U F F E R '_' S I Z E
    ;

EXTENDED_NOADDR
    : E X T E N D E D '_' N O A D D R
    ;

JSON_MERGEPATCH
    : J S O N '_' M E R G E P A T C H
    ;

SPLIT
    : S P L I T
    ;

BASELINE
    : B A S E L I N E
    ;

MEMORY
    : M E M O R Y
    ;

COVAR_POP
    : C O V A R '_' P O P
    ;

SEED
    : S E E D
    ;

DESCRIPTION
    : D E S C R I P T I O N
    ;

RTREE
    : R T R E E
    ;

MEDIAN
    : M E D I A N
    ;

UNLIMITED
    : U N L I M I T E D
    ;

STDDEV_POP
    : S T D D E V '_' P O P
    ;

UNDER
    : U N D E R
    ;

RUN
    : R U N
    ;

SQL_AFTER_GTIDS
    : S Q L '_' A F T E R '_' G T I D S
    ;

OPEN
    : O P E N
    ;

REFERENCING
    : R E F E R E N C I N G
    ;

SQL_TSI_DAY
    : S Q L '_' T S I '_' D A Y
    ;

MANAGE
    : M A N A G E
    ;

RELAY_THREAD
    : R E L A Y '_' T H R E A D
    ;

BREADTH
    : B R E A D T H
    ;

NOCACHE
    : N O C A C H E
    ;

DISALLOW
    : D I S A L L O W
    ;

PRIVILEGE
    : P R I V I L E G E
    ;

PRIMARY_ROOTSERVICE_LIST
    : P R I M A R Y '_' R O O T S E R V I C E '_' L I S T
    ;

UNUSUAL
    : U N U S U A L
    ;

RELAYLOG
    : R E L A Y L O G
    ;

SQL_BEFORE_GTIDS
    : S Q L '_' B E F O R E '_' G T I D S
    ;

PRIMARY_ZONE
    : P R I M A R Y '_' Z O N E
    ;

TABLE_CHECKSUM
    : T A B L E '_' C H E C K S U M
    ;

ZONE_LIST
    : Z O N E '_' L I S T
    ;

DATABASE_ID
    : D A T A B A S E '_' I D
    ;

TP_NO
    : T P '_' N O
    ;

NETWORK
    : N E T W O R K
    ;

LOWER_THAN_TO
    : L O W E R '_' T H A N '_' T O
    ;

HIDDEN_
    : H I D D E N
    ;

BOOLEAN
    : B O O L E A N
    ;

AVG
    : A V G
    ;

MULTILINESTRING
    : M U L T I L I N E S T R I N G
    ;

APPROX_COUNT_DISTINCT_SYNOPSIS_MERGE
    : A P P R O X '_' C O U N T '_' D I S T I N C T '_' S Y N O P S I S '_' M E R G E
    ;

NOW
    : N O W
    ;

PROXY
    : P R O X Y
    ;

DUPLICATE_SCOPE
    : D U P L I C A T E '_' S C O P E
    ;

STATS_SAMPLE_PAGES
    : S T A T S '_' S A M P L E '_' P A G E S
    ;

TABLET_SIZE
    : T A B L E T '_' S I Z E
    ;

BASE
    : B A S E
    ;

FOREIGN
    : F O R E I G N
    ;

KVCACHE
    : K V C A C H E
    ;

RELAY
    : R E L A Y
    ;

MINEXTENTS
    : M I N E X T E N T S
    ;

CONTRIBUTORS
    : C O N T R I B U T O R S
    ;

MEMORY_SIZE
    : M E M O R Y '_' S I Z E
    ;

EMPTY
    : E M P T Y
    ;

PARTIAL
    : P A R T I A L
    ;

REPORT
    : R E P O R T
    ;

ESCAPE
    : E S C A P E
    ;

MASTER_AUTO_POSITION
    : M A S T E R '_' A U T O '_' P O S I T I O N
    ;

CALC_PARTITION_ID
    : C A L C '_' P A R T I T I O N '_' I D
    ;

TP_NAME
    : T P '_' N A M E
    ;

SQL_AFTER_MTS_GAPS
    : S Q L '_' A F T E R '_' M T S '_' G A P S
    ;

EFFECTIVE
    : E F F E C T I V E
    ;

FIRST_VALUE
    : F I R S T '_' V A L U E
    ;

SQL_TSI_MINUTE
    : S Q L '_' T S I '_' M I N U T E
    ;

UNICODE
    : U N I C O D E
    ;

QUARTER
    : Q U A R T E R
    ;

ANALYSE
    : A N A L Y S E
    ;

DEFINER
    : D E F I N E R
    ;

NONE
    : N O N E
    ;

PROCESSLIST
    : P R O C E S S L I S T
    ;

TYPE
    : T Y P E
    ;

CHAR_CS
    : C H A R '_' C S
    ;

INSERT_METHOD
    : I N S E R T '_' M E T H O D
    ;

EXTENDED
    : E X T E N D E D
    ;

LISTS
    : L I S T S
    ;

LOG
    : L O G
    ;

TIME_ZONE_INFO
    : T I M E '_' Z O N E '_' I N F O
    ;

TIMESTAMPADD
    : T I M E S T A M P A D D
    ;

DISMOUNT
    : D I S M O U N T
    ;

BINARY_FLOAT_INFINITY
    : B I N A R Y '_' F L O A T '_' I N F I N I T Y
    ;

GET_FORMAT
    : G E T '_' F O R M A T
    ;

LOW
    : L O W
    ;

PREPARE
    : P R E P A R E
    ;

WORK
    : W O R K
    ;

MATERIALIZED
    : M A T E R I A L I Z E D
    ;

HANDLER
    : H A N D L E R
    ;

CUME_DIST
    : C U M E '_' D I S T
    ;

NOSORT
    : N O S O R T
    ;

INITIAL_SIZE
    : I N I T I A L '_' S I Z E
    ;

RELAY_LOG_FILE
    : R E L A Y '_' L O G '_' F I L E
    ;

STORING
    : S T O R I N G
    ;

IMPORT
    : I M P O R T
    ;

MIN_MEMORY
    : M I N '_' M E M O R Y
    ;

HELP
    : H E L P
    ;

CREATE_TIMESTAMP
    : C R E A T E '_' T I M E S T A M P
    ;

COMPUTE
    : C O M P U T E
    ;

RANDOM
    : R A N D O M
    ;

SOUNDS
    : S O U N D S
    ;

TABLE_MODE
    : T A B L E '_' M O D E
    ;

COPY
    : C O P Y
    ;

SQL_NO_CACHE
    : S Q L '_' N O '_' C A C H E
    ;

MISMATCH
    : M I S M A T C H
    ;

EXECUTE
    : E X E C U T E
    ;

PRECEDING
    : P R E C E D I N G
    ;

SWITCHES
    : S W I T C H E S
    ;

PACK_KEYS
    : P A C K '_' K E Y S
    ;

ENABLE_EXTENDED_ROWID
    : E N A B L E '_' E X T E N D E D '_' R O W I D
    ;

SQL_ID
    : S Q L '_' I D
    ;

NOORDER
    : N O O R D E R
    ;

CHECKPOINT
    : C H E C K P O I N T
    ;

DAY
    : D A Y
    ;

AUTHORIZATION
    : A U T H O R I Z A T I O N
    ;

LEAD
    : L E A D
    ;

JSON_TABLE
    : J S O N '_' T A B L E
    ;

JSON_VALUE
    : J S O N '_' V A L U E
    ;

DBA
    : D B A
    ;

EVENTS
    : E V E N T S
    ;

RECURSIVE
    : R E C U R S I V E
    ;

ONLY
    : O N L Y
    ;

TABLEGROUP_ID
    : T A B L E G R O U P '_' I D
    ;

GROUP_ID
    : G R O U P '_' I D
    ;

GROUPING_ID
    : G R O U P I N G '_' I D
    ;

TOP_K_FRE_HIST
    : T O P '_' K '_' F R E '_' H I S T
    ;

MASTER_SSL_CRL
    : M A S T E R '_' S S L '_' C R L
    ;

RESOURCE_POOL_LIST
    : R E S O U R C E '_' P O O L '_' L I S T
    ;

TRACING
    : T R A C I N G
    ;

NTILE
    : N T I L E
    ;

NULL_IF_EXETERNAL
    : N U L L '_' I F '_' E X E T E R N A L
    ;

SKEWONLY
    : S K E W O N L Y
    ;

IS_TENANT_SYS_POOL
    : I S '_' T E N A N T '_' S Y S '_' P O O L
    ;

INLINE
    : I N L I N E
    ;

MOUNT
    : M O U N T
    ;

SCHEDULE
    : S C H E D U L E
    ;

JOB
    : J O B
    ;

MASTER_LOG_POS
    : M A S T E R '_' L O G '_' P O S
    ;

SUBCLASS_ORIGIN
    : S U B C L A S S '_' O R I G I N
    ;

MULTIPOINT
    : M U L T I P O I N T
    ;

INFINITE_VALUE
    : I N F I N I T E '_' V A L U E
    ;

BLOCK
    : B L O C K
    ;

SQL_TSI_SECOND
    : S Q L '_' T S I '_' S E C O N D
    ;

ROLLUP
    : R O L L U P
    ;

CORR
    : C O R R
    ;

MIN_CPU
    : M I N '_' C P U
    ;

OCCUR
    : O C C U R
    ;

ACCESSED
    : A C C E S S E D
    ;

DATA
    : D A T A
    ;

BINARY_FLOAT_NAN
    : B I N A R Y '_' F L O A T '_' N A N
    ;

MASTER_HOST
    : M A S T E R '_' H O S T
    ;

PASSWORD_GRACE_TIME
    : P A S S W O R D '_' G R A C E '_' T I M E
    ;

VAR_SAMP
    : V A R '_' S A M P
    ;

ALGORITHM
    : A L G O R I T H M
    ;

CONSTRAINT_NAME
    : C O N S T R A I N T '_' N A M E
    ;

LIMIT
    : L I M I T
    ;

APPROX_COUNT_DISTINCT
    : A P P R O X '_' C O U N T '_' D I S T I N C T
    ;

DDL
    : D D L
    ;

BASIC
    : B A S I C
    ;

DEFAULT_TABLEGROUP
    : D E F A U L T '_' T A B L E G R O U P
    ;

CONTENTS
    : C O N T E N T S
    ;

CONTENT
    : C O N T E N T
    ;

XMLELEMENT
    : X M L E L E M E N T
    ;

ENTITYESCAPING
    : E N T I T Y E S C A P I N G
    ;

EXTRACTVALUE
    : E X T R A C T V A L U E
    ;

NOENTITYESCAPING
    : N O E N T I T Y E S C A P I N G
    ;

NOSCHEMACHECK
    : N O S C H E M A C H E C K
    ;

SCHEMACHECK
    : S C H E M A C H E C K
    ;

NO_PX_JOIN_FILTER
    : N O '_' P X '_' J O I N '_' F I L T E R
    ;

STATEMENT_ID
    : S T A T E M E N T '_' I D
    ;

HIGHER_THAN_TO
    : H I G H E R '_' T H A N '_' T O
    ;

LINK
    : L I N K
    ;

WEEK
    : W E E K
    ;

UNCONDITIONAL
    : U N C O N D I T I O N A L
    ;

CONDITIONAL
    : C O N D I T I O N A L
    ;

NULLS
    : N U L L S
    ;

MASTER_SSL_CRLPATH
    : M A S T E R '_' S S L '_' C R L P A T H
    ;

CASCADED
    : C A S C A D E D
    ;

PLUGIN
    : P L U G I N
    ;

ENCRYPTED
    : E N C R Y P T E D
    ;

TENANT
    : T E N A N T
    ;

INITRANS
    : I N I T R A N S
    ;

SCN
    : S C N
    ;

LNNVL
    : ( L N N V L )
    ;

INTNUM
    : [0-9]+
    ;

DECIMAL_VAL
    : ([0-9]+ E [-+]?[0-9]+ F | [0-9]+'.'[0-9]* E [-+]?[0-9]+ F | '.'[0-9]+ E [-+]?[0-9]+ F )
    | ([0-9]+ E [-+]?[0-9]+ D | [0-9]+'.'[0-9]* E [-+]?[0-9]+ D | '.'[0-9]+ E [-+]?[0-9]+ D )
    | ([0-9]+ E [-+]?[0-9]+ | [0-9]+'.'[0-9]* E [-+]?[0-9]+ | '.'[0-9]+ E [-+]?[0-9]+)
    | ([0-9]+'.'[0-9]* F | [0-9]+ F | '.'[0-9]+ F )
    | ([0-9]+'.'[0-9]* D | [0-9]+ D | '.'[0-9]+ D )
    | ([0-9]+'.'[0-9]* | '.'[0-9]+)
    ;

BOOL_VALUE
    : T R U E
    | F A L S E
    ;

At
    : '@'
    ;

LeftBracket
    : '['
    ;

LeftBrace
    : '{'
    ;

RightBracket
    : ']'
    ;

RightBrace
    : '}'
    ;

DATE_VALUE
    : D A T E ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''
    | T I M E S T A M P ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''
    ;

INTERVAL_VALUE
    : I N T E R V A L ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''[ \t\n\r\f]*( Y E A R | M O N T H )[ \t\n\r\f]*('('|[\uff08])[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*(')'|[\uff09])?
    | I N T E R V A L ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''[ \t\n\r\f]*( Y E A R | M O N T H )([ \t\n\r\f]*('('|[\uff08])[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*(')'|[\uff09])[ \t\n\r\f]*|[ \t\n\r\f]+) T O [ \t\n\r\f]+( Y E A R | M O N T H )
    | I N T E R V A L ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''[ \t\n\r\f]*( D A Y | H O U R | M I N U T E | S E C O N D )[ \t\n\r\f]*('('|[\uff08])[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*(')'|[\uff09])?
    | I N T E R V A L ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''[ \t\n\r\f]* S E C O N D [ \t\n\r\f]*('('|[\uff08])[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*','[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*(')'|[\uff09])
    | I N T E R V A L ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''[ \t\n\r\f]*( D A Y | H O U R | M I N U T E | S E C O N D )([ \t\n\r\f]*('('|[\uff08])[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*(')'|[\uff09])[ \t\n\r\f]*|[ \t\n\r\f]+) T O [ \t\n\r\f]+( D A Y | H O U R | M I N U T E | S E C O N D [ \t\n\r\f]*('('|[\uff08])[ \t\n\r\f]*[0-9]+[ \t\n\r\f]*(')'|[\uff09])?)
    ;

HINT_VALUE
    : '/''*' H I N T '+'(~[*])+'*''/'
    ;

Comma
    : [,]
    ;

Plus
    : [+]
    ;

And
    : [&]
    ;

Or
    : [|]
    ;

Star
    : [*]
    ;

Not
    : [!]
    ;

LeftParen
    : [(]
    ;

Minus
    : [-]
    ;

Div
    : [/]
    ;

Caret
    : [^]
    ;

Colon
    : [:]
    ;

Dot
    : [.]
    ;

Mod
    : [%]
    ;

RightParen
    : [)]
    ;

Tilde
    : [~]
    ;

DELIMITER
    : [;]
    ;

CNNOP
    : '||'
    ;

AND_OP
    : '&&'
    ;

COMP_EQ
    : '='
    ;

SET_VAR
    : ':='
    ;

COMP_GT
    : '>'
    ;

COMP_GE
    : '>='
    ;

COMP_LE
    : '<='
    ;

COMP_LT
    : '<'
    ;

COMP_NE
    : '!=' | '<>' | '^='
    ;

SHIFT_LEFT
    : '<<'
    ;

SHIFT_RIGHT
    : '>>'
    ;

COMP_NE_PL
    : '~='
    ;

POW_PL
    : '**'
    ;

QUESTIONMARK
    : '?'
    | ':'[0-9]+
    | ':'(([A-Za-z]|~[\u0000-\u007F\uD800-\uDBFF])([A-Za-z0-9$_#]|~[\u0000-\u007F\uD800-\uDBFF])*)
    ;

SYSTEM_VARIABLE
    : ('@''@'[A-Za-z_][A-Za-z0-9_]*)
    ;

USER_VARIABLE
    : ('@'[A-Za-z0-9_.$]*)|('@'['"]['"A-Za-z0-9_.$/%]*)
    ;

PARSER_SYNTAX_ERROR
    : X '\''([0-9A-F])*'\''
    | '.'
    ;

PLSQL_VARIABLE
    : ('$''$'[A-Za-z_][A-Za-z0-9_$#]*)
    ;

MULTISET_OP
    : ( M U L T I S E T [ \t\n\r\f]+( U N I O N | I N T E R S E C T | E X C E P T ))
    ;

A_
    : A
    ;

NAME_OB
    : (([A-Za-z]|~[\u0000-\u007F\uD800-\uDBFF])([A-Za-z0-9$_#]|~[\u0000-\u007F\uD800-\uDBFF])*)
    | '"' (~["]|('""'))* '"'
    ;

CHAR_STRING_PERL
    : Q '\'' (QS_ANGLE | QS_BRACE | QS_BRACK | QS_PAREN | QS_EXCLAM | QS_SHARP | QS_QUOTE | QS_DQUOTE) '\'' -> type(STRING_VALUE);

STRING_VALUE
    : ('N'|'n')?('Q'|'q')? '\'' (~[']|('\'\''))* '\''
    ;

In_c_comment
    : '/*' .*? '*/'      -> channel(1)
    ;

ANTLR_SKIP
    : '--'[ \t]* .*? '\n'   -> channel(1)
    ;

Blank
    : [ \t\r\n] -> channel(1)    ;


fragment A : [aA];
fragment B : [bB];
fragment C : [cC];
fragment D : [dD];
fragment E : [eE];
fragment F : [fF];
fragment G : [gG];
fragment H : [hH];
fragment I : [iI];
fragment J : [jJ];
fragment K : [kK];
fragment L : [lL];
fragment M : [mM];
fragment N : [nN];
fragment O : [oO];
fragment P : [pP];
fragment Q : [qQ];
fragment R : [rR];
fragment S : [sS];
fragment T : [tT];
fragment U : [uU];
fragment V : [vV];
fragment W : [wW];
fragment X : [xX];
fragment Y : [yY];
fragment Z : [zZ];
fragment QS_ANGLE   : '<' .*? '>';
fragment QS_BRACE   : '{' .*? '}';
fragment QS_BRACK   : '[' .*? ']';
fragment QS_PAREN   : '(' .*? ')';
fragment QS_EXCLAM  : '!' .*? '!';
fragment QS_SHARP   : '#' .*? '#';
fragment QS_QUOTE   : '\'' .*? '\'';
fragment QS_DQUOTE  : '"' .*? '"';
