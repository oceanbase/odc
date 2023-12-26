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

ACCESS
    : ( A C C E S S )
    ;

ACCESSIBLE
    : ( A C C E S S I B L E )
    ;

ADD
    : ( A D D )
    ;

AGAINST
    : ( A G A I N S T )
    ;

ALTER
    : ( A L T E R )
    ;

ALWAYS
    : ( A L W A Y S )
    ;

AND
    : ( A N D )
    ;

ANALYZE
    : ( A N A L Y Z E )
    ;

ALL
    : ( A L L )
    ;

NAMESPACE
    : N A M E S P A C E
    ;

AS
    : ( A S )
    ;

ASENSITIVE
    : ( A S E N S I T I V E )
    ;

ASC
    : ( A S C )
    ;

BETWEEN
    : ( B E T W E E N )
    ;

BEFORE
    : ( B E F O R E )
    ;

BIGINT
    : ( B I G I N T )
    ;

BINARY
    : ( B I N A R Y )
    ;

BLOB
    : ( B L O B )
    ;

BOTH
    : ( B O T H )
    ;

BY
    : ( B Y )
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
    : ( C H A R )
    | ( C H A R A C T E R )
    ;

CHECK
    : ( C H E C K )
    ;

CIPHER
    : ( C I P H E R )
    ;

CONDITION
    : ( C O N D I T I O N )
    ;

CONSTRAINT
    : ( C O N S T R A I N T )
    ;

CONTINUE
    : ( C O N T I N U E )
    ;

CONVERT
    : ( C O N V E R T )
    ;

COLLATE
    : ( C O L L A T E )
    ;

COLUMN
    : ( C O L U M N )
    ;

COLUMNS
    : ( C O L U M N S )
    ;

LS
    : L S
    ;

CREATE
    : ( C R E A T E )
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

CURRENT_TIME
    : ( C U R R E N T '_' T I M E )
    ;

CURRENT_TIMESTAMP
    : ( C U R R E N T '_' T I M E S T A M P )
    ;

CURRENT_USER
    : ( C U R R E N T '_' U S E R )
    ;

WITH_ROWID
    : (( W I T H ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*)) R O W I D ))
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

NUMBER
    : ( D E C )
    | ( N U M E R I C )
    | ( N U M B E R )
    ;

DECIMAL
    : ( D E C I M A L )
    ;

DECLARE
    : ( D E C L A R E )
    ;

DEFAULT
    : ( D E F A U L T )
    ;

DELAYED
    : ( D E L A Y E D )
    ;

DELETE
    : ( D E L E T E )
    ;

DESC
    : ( D E S C )
    ;

DESCRIBE
    : ( D E S C R I B E )
    ;

DETERMINISTIC
    : ( D E T E R M I N I S T I C )
    ;

DIV
    : ( D I V )
    ;

DISTINCT
    : ( D I S T I N C T )
    ;

DISTINCTROW
    : ( D I S T I N C T R O W )
    ;

DOUBLE
    : ( D O U B L E )
    ;

DROP
    : ( D R O P )
    ;

DUAL
    : ( D U A L )
    ;

EACH
    : ( E A C H )
    ;

ENCLOSED
    : ( E N C L O S E D )
    ;

ELSE
    : ( E L S E )
    ;

ELSEIF
    : ( E L S E I F )
    ;

ESCAPED
    : ( E S C A P E D )
    ;

EXISTS
    : ( E X I S T S )
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

FIELDS
    : ( F I E L D S )
    ;

FOREIGN
    : ( F O R E I G N )
    ;

FLOAT
    : ( F L O A T )
    ;

FLOAT4
    : ( F L O A T '4')
    ;

FLOAT8
    : ( F L O A T '8')
    ;

FOR
    : ( F O R )
    ;

FORCE
    : ( F O R C E )
    ;

FROM
    : ( F R O M )
    ;

FULL
    : ( F U L L )
    ;

FULLTEXT
    : ( F U L L T E X T )
    ;

GET
    : ( G E T )
    ;

GENERATED
    : ( G E N E R A T E D )
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

IN
    : ( I N )
    ;

INDEX
    : ( I N D E X )
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

INTEGER
    : ( I N T )
    | ( I N T E G E R )
    ;

INT1
    : ( I N T '1')
    ;

INT2
    : ( I N T '2')
    ;

INT3
    : ( I N T '3')
    ;

LIB
    : L I B
    ;

INT4
    : ( I N T '4')
    ;

INT8
    : ( I N T '8')
    ;

INTERVAL
    : I N T E R V A L
    ;

INSERT
    : ( I N S E R T )
    ;

ORDINALITY
    : O R D I N A L I T Y
    ;

INTO
    : ( I N T O )
    ;

IO_AFTER_GTIDS
    : ( I O '_' A F T E R '_' G T I D S )
    ;

IO_BEFORE_GTIDS
    : ( I O '_' B E F O R E '_' G T I D S )
    ;

IS
    : ( I S )
    ;

ISSUER
    : ( I S S U E R )
    ;

ITERATE
    : ( I T E R A T E )
    ;

JOIN
    : ( J O I N )
    ;

KEY
    : ( K E Y )
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

LIMIT
    : ( L I M I T )
    ;

LIKE
    : ( L I K E )
    ;

LINEAR
    : ( L I N E A R )
    ;

LINES
    : ( L I N E S )
    ;

BISON_LIST
    : ( L I S T )
    ;

LOAD
    : ( L O A D )
    ;

LOCAL
    : ( L O C A L )
    ;

OPTIMIZER_FEATURES_ENABLE
    : O P T I M I Z E R '_' F E A T U R E S '_' E N A B L E
    ;

LOCALTIME
    : ( L O C A L T I M E )
    ;

LOCALTIMESTAMP
    : ( L O C A L T I M E S T A M P )
    ;

LOCK_
    : ( L O C K )
    ;

LONG
    : ( L O N G )
    ;

UNLIMITED
    : U N L I M I T E D
    ;

LONGBLOB
    : ( L O N G B L O B )
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

MODE
    : ( M O D E )
    ;

MODIFIES
    : ( M O D I F I E S )
    ;

NATURAL
    : ( N A T U R A L )
    ;

NO_WRITE_TO_BINLOG
    : ( N O '_' W R I T E '_' T O '_' B I N L O G )
    ;

ON
    : ( O N )
    ;

OPTION
    : ( O P T I O N )
    ;

OPTIMIZE
    : ( O P T I M I Z E )
    ;

OPTIONALLY
    : ( O P T I O N A L L Y )
    ;

OR
    : ( O R )
    ;

ORDER
    : ( O R D E R )
    ;

OUT
    : ( O U T )
    ;

IOPS_WEIGHT
    : I O P S '_' W E I G H T
    ;

OUTER
    : ( O U T E R )
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

PRECISION
    : ( P R E C I S I O N )
    ;

PRIMARY
    : ( P R I M A R Y )
    ;

PUBLIC
    : ( P U B L I C )
    ;

RANGE
    : ( R A N G E )
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

REAL
    : ( R E A L )
    ;

RELEASE
    : ( R E L E A S E )
    ;

REFERENCES
    : ( R E F E R E N C E S )
    ;

REGEXP
    : ( R E G E X P )
    | ( R L I K E )
    ;

RENAME
    : ( R E N A M E )
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

REVOKE
    : ( R E V O K E )
    ;

RIGHT
    : ( R I G H T )
    ;

ROWS
    : ( R O W S )
    ;

SECOND_MICROSECOND
    : ( S E C O N D '_' M I C R O S E C O N D )
    ;

SELECT
    : ( S E L E C T )
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

SET
    : ( S E T )
    ;

REJECT
    : R E J E C T
    ;

SENSITIVE
    : ( S E N S I T I V E )
    ;

SHOW
    : ( S H O W )
    ;

SIGNAL
    : ( S I G N A L )
    ;

SMALLINT
    : ( S M A L L I N T )
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

SSL
    : ( S S L )
    ;

STARTING
    : ( S T A R T I N G )
    ;

NUMERIC
    : N U M E R I C
    ;

STORED
    : ( S T O R E D )
    ;

PLUS
    : P L U S
    ;

STRAIGHT_JOIN
    : ( S T R A I G H T '_' J O I N )
    ;

SUBJECT
    : ( S U B J E C T )
    ;

SYSDATE
    : ( S Y S D A T E )
    ;

TERMINATED
    : ( T E R M I N A T E D )
    ;

TEXT
    : ( T E X T )
    ;

TINYBLOB
    : ( T I N Y B L O B )
    ;

TINYINT
    : ( T I N Y I N T )
    ;

TINYTEXT
    : ( T I N Y T E X T )
    ;

TABLE
    : ( T A B L E )
    ;

TABLEGROUP
    : ( T A B L E G R O U P )
    ;

THEN
    : ( T H E N )
    ;

TO
    : ( T O )
    ;

TRAILING
    : ( T R A I L I N G )
    ;

TRIGGER
    : ( T R I G G E R )
    ;

UNDO
    : ( U N D O )
    ;

CALIBRATION
    : C A L I B R A T I O N
    ;

UNION
    : ( U N I O N )
    ;

UNIQUE
    : ( U N I Q U E )
    ;

UNLOCK
    : ( U N L O C K )
    ;

LINE_DELIMITER
    : L I N E '_' D E L I M I T E R
    ;

UNSIGNED
    : ( U N S I G N E D )
    ;

UPDATE
    : ( U P D A T E )
    ;

USAGE
    : ( U S A G E )
    ;

GEOMCOLLECTION
    : G E O M C O L L E C T I O N
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

UTC_TIME
    : ( U T C '_' T I M E )
    ;

UTC_TIMESTAMP
    : ( U T C '_' T I M E S T A M P )
    ;

VALUES
    : ( V A L U E S )
    ;

QUERY_RESPONSE_TIME
    : Q U E R Y '_' R E S P O N S E '_' T I M E
    ;

VARBINARY
    : ( V A R B I N A R Y )
    ;

VARCHAR
    : ( V A R C H A R )
    | ( V A R C H A R A C T E R )
    ;

VARYING
    : ( V A R Y I N G )
    ;

VIRTUAL
    : ( V I R T U A L )
    ;

WHERE
    : ( W H E R E )
    ;

REDUNDANCY
    : R E D U N D A N C Y
    ;

WHEN
    : ( W H E N )
    ;

WHILE
    : ( W H I L E )
    ;

CALIBRATION_INFO
    : C A L I B R A T I O N '_' I N F O
    ;

SCN
    : S C N
    ;

WINDOW
    : ( W I N D O W )
    ;

WITH
    : ( W I T H )
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

UnderlineUTF8
    : ('_' U T F '8')
    ;

UnderlineUTF8MB4
    : ('_' U T F '8' M B '4')
    ;

UnderlineGBK
    : ('_' G B K )
    ;

UnderlineGB18030
    : ('_' G B '1''8''0''3''0')
    ;

UnderlineBINARY
    : ('_' B I N A R Y )
    ;

UnderlineUTF16
    : ('_' U T F '1''6')
    ;

UnderlineLATIN1
    : ('_' L A T I N '1')
    ;

UnderlineGB18030_2022
    : ('_' G B '1''8''0''3''0''_''2''0''2''2')
    ;

STRONG
    : ( S T R O N G )
    ;

WEAK
    : ( W E A K )
    ;

CONNECT
    : C O N N E C T
    ;

FROZEN
    : ( F R O Z E N )
    ;

EXCEPT
    : ( E X C E P T )
    ;

MINUS
    : ( M I N U S )
    ;

INTERSECT
    : ( I N T E R S E C T )
    ;

ISNULL
    : ( I S N U L L )
    ;

NOT
    : N O T
    ;

NULLX
    : N U L L
    ;

INTNUM
    : [0-9]+
    ;

AUDIT
    : A U D I T
    ;

WARNINGS
    : W A R N I N G S
    ;

FORMAT
    : F O R M A T
    ;

MINVALUE
    : M I N V A L U E
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

DISCONNECT
    : D I S C O N N E C T
    ;

STOP
    : S T O P
    ;

STORAGE_FORMAT_WORK_VERSION
    : S T O R A G E '_' F O R M A T '_' W O R K '_' V E R S I O N
    ;

SIZE
    : S I Z E
    ;

DISABLE_PARALLEL_DML
    : D I S A B L E '_' P A R A L L E L '_' D M L
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

CURDATE
    : C U R D A T E
    ;

JSON_VALUE
    : J S O N '_' V A L U E
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

CHANGED
    : C H A N G E D
    ;

MASTER_SSL_CAPATH
    : M A S T E R '_' S S L '_' C A P A T H
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

PROGRESSIVE_MERGE_NUM
    : P R O G R E S S I V E '_' M E R G E '_' N U M
    ;

QUEUE_TIME
    : Q U E U E '_' T I M E
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

CONSISTENT
    : C O N S I S T E N T
    ;

SUSPEND
    : S U S P E N D
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

PREVIEW
    : P R E V I E W
    ;

BANDWIDTH
    : B A N D W I D T H
    ;

TRUNCATE
    : T R U N C A T E
    ;

BACKUP_BACKUP_DEST
    : B A C K U P '_' B A C K U P '_' D E S T
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

REPLICA
    : R E P L I C A
    ;

KILL_EXPR
    : K I L L '_' E X P R
    ;

RECOVERY
    : R E C O V E R Y
    ;

OLD_KEY
    : O L D '_' K E Y
    ;

DISABLE
    : D I S A B L E
    ;

PORT
    : P O R T
    ;

REBUILD
    : R E B U I L D
    ;

FOLLOWER
    : F O L L O W E R
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

SEQUENCE
    : S E Q U E N C E
    ;

MIGRATION
    : M I G R A T I O N
    ;

SUBPARTITION
    : S U B P A R T I T I O N
    ;

MYSQL_DRIVER
    : M Y S Q L '_' D R I V E R
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

UNLOCKED
    : U N L O C K E D
    ;

CLASS_ORIGIN
    : C L A S S '_' O R I G I N
    ;

RUDUNDANT
    : R U D U N D A N T
    ;

STATEMENTS
    : S T A T E M E N T S
    ;

ACTION
    : A C T I O N
    ;

REDUNDANT
    : R E D U N D A N T
    ;

UPGRADE
    : U P G R A D E
    ;

VALIDATE
    : V A L I D A T E
    ;

START
    : S T A R T
    ;

TEMPTABLE
    : T E M P T A B L E
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

LEAVES
    : L E A V E S
    ;

UNDEFINED
    : U N D E F I N E D
    ;

EVERY
    : E V E R Y
    ;

SHARDING
    : S H A R D I N G
    ;

BYTE
    : B Y T E
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

SLOT_IDX
    : S L O T '_' I D X
    ;

APPROXNUM
    : A P P R O X N U M
    ;

HASH
    : H A S H
    ;

ROTATE
    : R O T A T E
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

MAX
    : M A X
    ;

TRANSACTION
    : T R A N S A C T I O N
    ;

SQL_TSI_MONTH
    : S Q L '_' T S I '_' M O N T H
    ;

ARBITRATION
    : A R B I T R A T I O N
    ;

IGNORE
    : I G N O R E
    ;

MAX_QUERIES_PER_HOUR
    : M A X '_' Q U E R I E S '_' P E R '_' H O U R
    ;

COMMENT
    : C O M M E N T
    ;

CTX_ID
    : C T X '_' I D
    ;

MIN_IOPS
    : M I N '_' I O P S
    ;

NVARCHAR
    : N V A R C H A R
    ;

OFF
    : O F F
    ;

BIT_XOR
    : B I T '_' X O R
    ;

PAUSE
    : P A U S E
    ;

QUICK
    : Q U I C K
    ;

PRETTY
    : P R E T T Y
    ;

DUPLICATE
    : D U P L I C A T E
    ;

WAIT
    : W A I T
    ;

FIELD_OPTIONALLY_ENCLOSED_BY
    : F I E L D '_' O P T I O N A L L Y '_' E N C L O S E D '_' B Y
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

ENGINE_
    : E N G I N E
    ;

TABLES
    : T A B L E S
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

VARCHARACTER
    : V A R C H A R A C T E R
    ;

INVOKER
    : I N V O K E R
    ;

DEPTH
    : D E P T H
    ;

NORMAL
    : N O R M A L
    ;

LN
    : L N
    ;

COLUMN_NAME
    : C O L U M N '_' N A M E
    ;

TRIGGERS
    : T R I G G E R S
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

AUTO_INCREMENT_MODE
    : A U T O '_' I N C R E M E N T '_' M O D E
    ;

UNBOUNDED
    : U N B O U N D E D
    ;

WRAPPER
    : W R A P P E R
    ;

TIMESTAMP
    : T I M E S T A M P
    ;

PARTITIONS
    : P A R T I T I O N S
    ;

SUBSTR
    : S U B S T R
    ;

CHUNK
    : C H U N K
    ;

FILEX
    : F I L E X
    ;

BACKUPSET
    : B A C K U P S E T
    ;

PRIMARY_CLUSTER_ID
    : P R I M A R Y '_' C L U S T E R '_' I D
    ;

UNIT
    : U N I T
    ;

NATIONAL_LITERAL
    : N A T I O N A L '_' L I T E R A L
    ;

PRIVILEGES
    : P R I V I L E G E S
    ;

LOWER_ON
    : L O W E R '_' O N
    ;

BACKUPPIECE
    : B A C K U P P I E C E
    ;

LESS
    : L E S S
    ;

SWITCH
    : S W I T C H
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

ROUTINE
    : R O U T I N E
    ;

FOLLOWING
    : F O L L O W I N G
    ;

ROLLBACK
    : R O L L B A C K
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

DUMP
    : D U M P
    ;

EXTERNAL
    : E X T E R N A L
    ;

APPROX_COUNT_DISTINCT_SYNOPSIS
    : A P P R O X '_' C O U N T '_' D I S T I N C T '_' S Y N O P S I S
    ;

GROUPING
    : G R O U P I N G
    ;

OF
    : O F
    ;

SLOG
    : S L O G
    ;

OJ
    : O J
    ;

ARCHIVELOG
    : A R C H I V E L O G
    ;

MAX_CONNECTIONS_PER_HOUR
    : M A X '_' C O N N E C T I O N S '_' P E R '_' H O U R
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

PL
    : P L
    ;

MEMSTORE_PERCENT
    : M E M S T O R E '_' P E R C E N T
    ;

STD
    : S T D
    ;

POLYGON
    : P O L Y G O N
    ;

PS
    : P S
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

PREV
    : P R E V
    ;

RECOVER
    : R E C O V E R
    ;

PROCESS
    : P R O C E S S
    ;

DEALLOCATE
    : D E A L L O C A T E
    ;

OLD_PASSWORD
    : O L D '_' P A S S W O R D
    ;

FAILOVER
    : F A I L O V E R
    ;

P_NSEQ
    : P '_' N S E Q
    ;

LISTAGG
    : L I S T A G G
    ;

SLOW
    : S L O W
    ;

NOAUDIT
    : N O A U D I T
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

RLIKE
    : R L I K E
    ;

INDEXED
    : I N D E X E D
    ;

RETURNING
    : R E T U R N I N G
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

STATISTICS
    : S T A T I S T I C S
    ;

SERVER_TYPE
    : S E R V E R '_' T Y P E
    ;

COMMITTED
    : C O M M I T T E D
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

IDC
    : I D C
    ;

VIEW
    : V I E W
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

REVERSE
    : R E V E R S E
    ;

UP
    : U P
    ;

CLUSTER_ID
    : C L U S T E R '_' I D
    ;

NOARCHIVELOG
    : N O A R C H I V E L O G
    ;

BEGIN_OUTLINE_DATA
    : B E G I N '_' O U T L I N E '_' D A T A
    ;

MAX_SIZE
    : M A X '_' S I Z E
    ;

PAGE
    : P A G E
    ;

NAME
    : N A M E
    ;

ROW_COUNT
    : R O W '_' C O U N T
    ;

LAST
    : L A S T
    ;

WASH
    : W A S H
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

ROLLING
    : R O L L I N G
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

SIGNED
    : S I G N E D
    ;

SERVER
    : S E R V E R
    ;

NEXT
    : N E X T
    ;

ENDS
    : E N D S
    ;

GLOBAL
    : G L O B A L
    ;

ROOTSERVICE_LIST
    : R O O T S E R V I C E '_' L I S T
    ;

SHUTDOWN
    : S H U T D O W N
    ;

VERBOSE
    : V E R B O S E
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

LOWER_COMMA
    : L O W E R '_' C O M M A
    ;

XA
    : X A
    ;

TIME
    : T I M E
    ;

DATETIME
    : D A T E T I M E
    ;

NOMINVALUE
    : N O M I N V A L U E
    ;

BOOL
    : B O O L
    ;

DIRECTORY
    : D I R E C T O R Y
    ;

DATA_TABLE_ID
    : D A T A '_' T A B L E '_' I D
    ;

SEQUENCES
    : S E Q U E N C E S
    ;

PRETTY_COLOR
    : P R E T T Y '_' C O L O R
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

SHARE
    : S H A R E
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

CONSTRAINT_CATALOG
    : C O N S T R A I N T '_' C A T A L O G
    ;

CLUSTER
    : C L U S T E R
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

HYBRID_HIST
    : H Y B R I D '_' H I S T
    ;

INSTANCE
    : I N S T A N C E
    ;

FUNCTION
    : F U N C T I O N
    ;

NOWAIT
    : N O W A I T
    ;

INVISIBLE
    : I N V I S I B L E
    ;

DENSE_RANK
    : D E N S E '_' R A N K
    ;

COUNT
    : C O U N T
    ;

NAMES
    : N A M E S
    ;

MY_NAME
    : M Y '_' N A M E
    ;

CHAR
    : C H A R
    ;

LOWER_THAN_NEG
    : L O W E R '_' T H A N '_' N E G
    ;

P_ENTITY
    : P '_' E N T I T Y
    ;

ISOLATE
    : I S O L A T E
    ;

MAX_ROWS
    : M A X '_' R O W S
    ;

CTXCAT
    : C T X C A T
    ;

ISOLATION
    : I S O L A T I O N
    ;

REPLICATION
    : R E P L I C A T I O N
    ;

DECRYPTION
    : D E C R Y P T I O N
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

MODIFY
    : M O D I F Y
    ;

UNCOMMITTED
    : U N C O M M I T T E D
    ;

PHYSICAL
    : P H Y S I C A L
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

COMPLETION
    : C O M P L E T I O N
    ;

CONDENSED
    : C O N D E N S E D
    ;

INPUT
    : I N P U T
    ;

ROOTTABLE
    : R O O T T A B L E
    ;

SUBSTRING
    : S U B S T R I N G
    ;

ZONE
    : Z O N E
    ;

BACKED
    : B A C K E D
    ;

SERVICE
    : S E R V I C E
    ;

TEMPLATE
    : T E M P L A T E
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

ENABLE
    : E N A B L E
    ;

HOSTS
    : H O S T S
    ;

SCHEMA_NAME
    : S C H E M A '_' N A M E
    ;

EXPANSION
    : E X P A N S I O N
    ;

REORGANIZE
    : R E O R G A N I Z E
    ;

BLOCK_SIZE
    : B L O C K '_' S I Z E
    ;

TRIM_SPACE
    : T R I M '_' S P A C E
    ;

INNER_PARSE
    : I N N E R '_' P A R S E
    ;

MINOR
    : M I N O R
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

BIT_AND
    : B I T '_' A N D
    ;

SUPER
    : S U P E R
    ;

TIMES
    : T I M E S
    ;

COMMIT
    : C O M M I T
    ;

SAVEPOINT
    : S A V E P O I N T
    ;

UNTIL
    : U N T I L
    ;

USER
    : U S E R
    ;

LEAK_RATE
    : L E A K '_' R A T E
    ;

MEMTABLE
    : M E M T A B L E
    ;

CHARSET
    : C H A R S E T
    ;

MOVE
    : M O V E
    ;

XML
    : X M L
    ;

PATH
    : P A T H
    ;

IPC
    : I P C
    ;

TRIM
    : T R I M
    ;

PERFORMANCE
    : P E R F O R M A N C E
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

LEAK_MOD
    : L E A K '_' M O D
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

USE_FRM
    : U S E '_' F R M
    ;

CLIENT_VERSION
    : C L I E N T '_' V E R S I O N
    ;

MASTER_HEARTBEAT_PERIOD
    : M A S T E R '_' H E A R T B E A T '_' P E R I O D
    ;

SUBPARTITIONS
    : S U B P A R T I T I O N S
    ;

CUBE
    : C U B E
    ;

FRAGMENTATION
    : F R A G M E N T A T I O N
    ;

BALANCE
    : B A L A N C E
    ;

QUERY
    : Q U E R Y
    ;

POLICY
    : P O L I C Y
    ;

THROTTLE
    : T H R O T T L E
    ;

SQL_TSI_QUARTER
    : S Q L '_' T S I '_' Q U A R T E R
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

SYNCHRONIZATION
    : S Y N C H R O N I Z A T I O N
    ;

AVAILABILITY
    : A V A I L A B I L I T Y
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

SQL_TSI_WEEK
    : S Q L '_' T S I '_' W E E K
    ;

P_CHUNK
    : P '_' C H U N K
    ;

SYSTEM
    : S Y S T E M
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

SKIP_HEADER
    : S K I P '_' H E A D E R
    ;

SQL_THREAD
    : S Q L '_' T H R E A D
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

FIXED
    : F I X E D
    ;

CACHE
    : C A C H E
    ;

CURRENT
    : C U R R E N T
    ;

STACKED
    : S T A C K E D
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

BADFILE
    : B A D F I L E
    ;

LOG_DISK_SIZE
    : L O G '_' D I S K '_' S I Z E
    ;

SQL_BUFFER_RESULT
    : S Q L '_' B U F F E R '_' R E S U L T
    ;

JSON
    : J S O N
    ;

SOME
    : S O M E
    ;

INDEX_TABLE_ID
    : I N D E X '_' T A B L E '_' I D
    ;

PATTERN
    : P A T T E R N
    ;

RECOVERY_WINDOW
    : R E C O V E R Y '_' W I N D O W
    ;

FREQUENCY
    : F R E Q U E N C Y
    ;

PQ_MAP
    : P Q '_' M A P
    ;

LOCKS
    : L O C K S
    ;

MANUAL
    : M A N U A L
    ;

GEOMETRY
    : G E O M E T R Y
    ;

IDENTIFIED
    : I D E N T I F I E D
    ;

NO_PARALLEL
    : N O '_' P A R A L L E L
    ;

STORAGE_FORMAT_VERSION
    : S T O R A G E '_' F O R M A T '_' V E R S I O N
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

BACKUPROUND
    : B A C K U P R O U N D
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

NOMAXVALUE
    : N O M A X V A L U E
    ;

ESTIMATE
    : E S T I M A T E
    ;

SLAVE
    : S L A V E
    ;

SKIP_BLANK_LINES
    : S K I P '_' B L A N K '_' L I N E S
    ;

GTS
    : G T S
    ;

EXPORT
    : E X P O R T
    ;

AVG_ROW_LENGTH
    : A V G '_' R O W '_' L E N G T H
    ;

ENFORCED
    : E N F O R C E D
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

CURTIME
    : C U R T I M E
    ;

REPLICA_TYPE
    : R E P L I C A '_' T Y P E
    ;

AGGREGATE
    : A G G R E G A T E
    ;

JSON_ARRAYAGG
    : J S O N '_' A R R A Y A G G
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

MAXIMIZE
    : M A X I M I Z E
    ;

MAX_UPDATES_PER_HOUR
    : M A X '_' U P D A T E S '_' P E R '_' H O U R
    ;

CURSOR_NAME
    : C U R S O R '_' N A M E
    ;

CONCURRENT
    : C O N C U R R E N T
    ;

DUMPFILE
    : D U M P F I L E
    ;

COMPRESSED
    : C O M P R E S S E D
    ;

LINESTRING
    : L I N E S T R I N G
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

INCREMENT
    : I N C R E M E N T
    ;

LAG
    : L A G
    ;

BASELINE_ID
    : B A S E L I N E '_' I D
    ;

NEW
    : N E W
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

HOST
    : H O S T
    ;

VALUE
    : V A L U E
    ;

LOGS
    : L O G S
    ;

SERIALIZABLE
    : S E R I A L I Z A B L E
    ;

AUTO_INCREMENT
    : A U T O '_' I N C R E M E N T
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

SET_MASTER_CLUSTER
    : S E T '_' M A S T E R '_' C L U S T E R
    ;

MINUTE
    : M I N U T E
    ;

SWAPS
    : S W A P S
    ;

TASK
    : T A S K
    ;

INNODB
    : I N N O D B
    ;

IO_THREAD
    : I O '_' T H R E A D
    ;

HISTOGRAM
    : H I S T O G R A M
    ;

PCTFREE
    : P C T F R E E
    ;

BC2HOST
    : B C '2' H O S T
    ;

PARAMETERS
    : P A R A M E T E R S
    ;

TABLESPACE
    : T A B L E S P A C E
    ;

OBCONFIG_URL
    : O B C O N F I G '_' U R L
    ;

AUTO
    : A U T O
    ;

PASSWORD
    : P A S S W O R D
    ;

LOWER_THAN_BY_ACCESS_SESSION
    : L O W E R '_' T H A N '_' B Y '_' A C C E S S '_' S E S S I O N
    ;

ROW
    : R O W
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

ANY
    : A N Y
    ;

UNIT_GROUP
    : U N I T '_' G R O U P
    ;

HIGHER_PARENS
    : H I G H E R '_' P A R E N S
    ;

ERROR_CODE
    : E R R O R '_' C O D E
    ;

PHASE
    : P H A S E
    ;

ENTITY
    : E N T I T Y
    ;

PROFILE
    : P R O F I L E
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

DATE_ADD
    : D A T E '_' A D D
    ;

BLOCK_INDEX
    : B L O C K '_' I N D E X
    ;

SERVER_IP
    : S E R V E R '_' I P
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

VIRTUAL_COLUMN_ID
    : V I R T U A L '_' C O L U M N '_' I D
    ;

COLUMN_FORMAT
    : C O L U M N '_' F O R M A T
    ;

MAX_MEMORY
    : M A X '_' M E M O R Y
    ;

NESTED
    : N E S T E D
    ;

CLEAN
    : C L E A N
    ;

MASTER_SSL
    : M A S T E R '_' S S L
    ;

CLEAR
    : C L E A R
    ;

END_OUTLINE_DATA
    : E N D '_' O U T L I N E '_' D A T A
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

STARTS
    : S T A R T S
    ;

PLANREGRESS
    : P L A N R E G R E S S
    ;

AUTOEXTEND_SIZE
    : A U T O E X T E N D '_' S I Z E
    ;

TABLET_ID
    : T A B L E T '_' I D
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

UNDO_BUFFER_SIZE
    : U N D O '_' B U F F E R '_' S I Z E
    ;

SWITCHOVER
    : S W I T C H O V E R
    ;

EXTENDED_NOADDR
    : E X T E N D E D '_' N O A D D R
    ;

GLOBAL_NAME
    : G L O B A L '_' N A M E
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

DESCRIPTION
    : D E S C R I P T I O N
    ;

SEED
    : S E E D
    ;

RTREE
    : R T R E E
    ;

RESOURCE
    : R E S O U R C E
    ;

STDDEV_POP
    : S T D D E V '_' P O P
    ;

RUN
    : R U N
    ;

OBSOLETE
    : O B S O L E T E
    ;

SQL_AFTER_GTIDS
    : S Q L '_' A F T E R '_' G T I D S
    ;

OPEN
    : O P E N
    ;

SQL_TSI_DAY
    : S Q L '_' T S I '_' D A Y
    ;

STRING
    : S T R I N G
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

PROTECTION
    : P R O T E C T I O N
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

BIT_OR
    : B I T '_' O R
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

KVCACHE
    : K V C A C H E
    ;

RELAY
    : R E L A Y
    ;

MEMORY_SIZE
    : M E M O R Y '_' S I Z E
    ;

CONTRIBUTORS
    : C O N T R I B U T O R S
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

DISKGROUP
    : D I S K G R O U P
    ;

CALC_PARTITION_ID
    : C A L C '_' P A R T I T I O N '_' I D
    ;

TP_NAME
    : T P '_' N A M E
    ;

ACTIVATE
    : A C T I V A T E
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

INSERT_METHOD
    : I N S E R T '_' M E T H O D
    ;

EXTENDED
    : E X T E N D E D
    ;

LOG
    : L O G
    ;

WHENEVER
    : W H E N E V E R
    ;

LEVEL
    : L E V E L
    ;

TIME_ZONE_INFO
    : T I M E '_' Z O N E '_' I N F O
    ;

TIMESTAMPADD
    : T I M E S T A M P A D D
    ;

LOWER_INTO
    : L O W E R '_' I N T O
    ;

GET_FORMAT
    : G E T '_' F O R M A T
    ;

PREPARE
    : P R E P A R E
    ;

MATERIALIZED
    : M A T E R I A L I Z E D
    ;

STANDBY
    : S T A N D B Y
    ;

WORK
    : W O R K
    ;

HANDLER
    : H A N D L E R
    ;

CUME_DIST
    : C U M E '_' D I S T
    ;

LEAK
    : L E A K
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

SESSION
    : S E S S I O N
    ;

DAG
    : D A G
    ;

NOCYCLE
    : N O C Y C L E
    ;

SQL_NO_CACHE
    : S Q L '_' N O '_' C A C H E
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

TENANT_ID
    : T E N A N T '_' I D
    ;

CHECKPOINT
    : C H E C K P O I N T
    ;

DAY
    : D A Y
    ;

GROUP_CONCAT
    : G R O U P '_' C O N C A T
    ;

JSON_TABLE
    : J S O N '_' T A B L E
    ;

LEAD
    : L E A D
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

BUCKETS
    : B U C K E T S
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

SCHEDULE
    : S C H E D U L E
    ;

JOB
    : J O B
    ;

SRID
    : S R I D
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

BLOCK
    : B L O C K
    ;

SQL_TSI_SECOND
    : S Q L '_' T S I '_' S E C O N D
    ;

DATE
    : D A T E
    ;

ROLLUP
    : R O L L U P
    ;

MIN_CPU
    : M I N '_' C P U
    ;

OCCUR
    : O C C U R
    ;

DATA
    : D A T A
    ;

SUCCESSFUL
    : S U C C E S S F U L
    ;

REDO_TRANSPORT_OPTIONS
    : R E D O '_' T R A N S P O R T '_' O P T I O N S
    ;

FIELD_DELIMITER
    : F I E L D '_' D E L I M I T E R
    ;

MASTER_HOST
    : M A S T E R '_' H O S T
    ;

VAR_SAMP
    : V A R '_' S A M P
    ;

ALGORITHM
    : A L G O R I T H M
    ;

EXPIRED
    : E X P I R E D
    ;

CONSTRAINT_NAME
    : C O N S T R A I N T '_' N A M E
    ;

APPROX_COUNT_DISTINCT
    : A P P R O X '_' C O U N T '_' D I S T I N C T
    ;

BASIC
    : B A S I C
    ;

DEFAULT_TABLEGROUP
    : D E F A U L T '_' T A B L E G R O U P
    ;

LIST_
    : L I S T
    ;

NO_PX_JOIN_FILTER
    : N O '_' P X '_' J O I N '_' F I L T E R
    ;

WEEK
    : W E E K
    ;

LINK
    : L I N K
    ;

STATEMENT_ID
    : S T A T E M E N T '_' I D
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

DECIMAL_VAL
    : ([0-9]+ E [-+]?[0-9]+ | [0-9]+'.'[0-9]* E [-+]?[0-9]+ | '.'[0-9]+ E [-+]?[0-9]+)
    | ([0-9]+'.'[0-9]* | '.'[0-9]+)
    ;

BOOL_VALUE
    : T R U E
    | F A L S E
    ;

LOG_RESTORE_SOURCE
    : L O G '_' R E S T O R E '_' S O U R C E
    ;

ENABLE_ARBITRATION_SERVICE
    : E N A B L E '_' A R B I T R A T I O N '_' S E R V I C E
    ;

At
    : '@'
    ;

LeftBrace
    : '{'
    ;

RightBrace
    : '}'
    ;

Quote
    : '\''
    ;

PARSER_SYNTAX_ERROR
    : X '\''([0-9A-F])*'\''|'0' X ([0-9A-F])+
    ;

HEX_STRING_VALUE
    : B '\''([01])*'\''|'0' B ([01])+
    ;

DATE_VALUE
    : D A T E ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*))?'\''(~['])*'\''
    | T I M E ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*))?'\''(~['])*'\''
    | T I M E S T A M P ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*))?'\''(~['])*'\''
    | D A T E ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*))?'"'(~["])*'"'
    | T I M E ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*))?'"'(~["])*'"'
    | T I M E S T A M P ([ \t\n\r\f]+|('--'[ \t\n\r\f]+(~[\n\r])*)|('#'(~[\n\r])*))?'"'(~["])*'"'
    ;

HINT_VALUE
    : '/''*' H I N T '+'(~[*])+'*''/'
    ;

//c_ret
//    : '*''/' -> mode(DEFAULT_MODE)
//    ;

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

COMP_NSEQ
    : '<=>'
    ;

COMP_GE
    : '>='
    ;

COMP_GT
    : '>'
    ;

COMP_LE
    : '<='
    ;

COMP_LT
    : '<'
    ;

COMP_NE
    : '!=' | '<>'
    ;

SHIFT_LEFT
    : '<<'
    ;

SHIFT_RIGHT
    : '>>'
    ;

JSON_EXTRACT
    : '->'
    ;

JSON_EXTRACT_UNQUOTED
    : '->>'
    ;

QUESTIONMARK
    : '?'
    | ':'[0-9]+
    | ':'(([A-Za-z0-9$_]|(~[\u0000-\u007F\uD800-\uDBFF]))+)'.'(([A-Za-z0-9$_]|(~[\u0000-\u007F\uD800-\uDBFF]))+)
    ;

SYSTEM_VARIABLE
    : ('@''@'[A-Za-z_][A-Za-z0-9_]*)
    ;

USER_VARIABLE
    : ('@'[A-Za-z0-9_.$]*)|('@'[`'"][`'"A-Za-z0-9_.$/%]*)
    ;

NAME_OB
    : (([A-Za-z0-9$_]|(~[\u0000-\u007F\uD800-\uDBFF]))+)
    | '`' ~[`]* '`'
    ;

STRING_VALUE
    : '\'' (~['\\]|('\'\'')|('\\'.))* '\''
    | '"' (~["\\]|('""')|('\\'.))* '"'

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

