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

lexer grammar PLLexer;

@members {
public boolean inRangeOperator = false;
}

MERGE
    : M E R G E
    ;

ALL
    : A L L
    ;

ALTER
    : A L T E R
    ;

AND
    : A N D
    ;

ANY
    : A N Y
    ;

ARRAY
    : A R R A Y
    ;

AS
    : A S
    ;

ASC
    : A S C
    ;

AT
    : A T
    ;

BEGIN_KEY
    : B E G I N
    ;

BETWEEN
    : B E T W E E N
    ;

BINARY_INTEGER
    : B I N A R Y '_' I N T E G E R
    ;

BODY
    : B O D Y
    ;

BOOL
    : B O O L E A N
    | B O O L
    ;

BY
    : B Y
    ;

BINARY_DOUBLE
    : B I N A R Y '_' D O U B L E
    ;

BINARY_FLOAT
    : B I N A R Y '_' F L O A T
    ;

SIMPLE_DOUBLE
    : S I M P L E '_' D O U B L E
    ;

SIMPLE_FLOAT
    : S I M P L E '_' F L O A T
    ;

CASE
    : C A S E
    ;

CHARACTER
    : C H A R
    | C H A R A C T E R
    ;

CHECK
    : C H E C K
    ;

CLOSE
    : C L O S E
    ;

CLUSTER
    : C L U S T E R
    ;

CLUSTERS
    : C L U S T E R S
    ;

COLAUTH
    : C O L A U T H
    ;

COLUMNS
    : C O L U M N S
    ;

COMMIT
    : C O M M I T
    ;

COMPRESS
    : C O M P R E S S
    ;

CONNECT
    : C O N N E C T
    ;

CONSTANT
    : C O N S T A N T
    ;

CRASH
    : C R A S H
    ;

CREATE
    : C R E A T E
    ;

CURSOR
    : C U R S O R
    ;

DATE
    : D A T E
    ;

NUMBER
    : D E C I M A L
    | N U M B E R
    ;

DECLARE
    : D E C L A R E
    ;

DEFAULT
    : D E F A U L T
    ;

DELETE
    : D E L E T E
    ;

DESC
    : D E S C
    ;

DISTINCT
    : D I S T I N C T
    ;

DROP
    : D R O P
    ;

ELSE
    : E L S E
    ;

ELSIF
    : E L S I F
    ;

END_KEY
    : E N D
    ;

EXCEPTION
    : E X C E P T I O N
    ;

EXCEPTIONS
    : E X C E P T I O N S
    ;

EXCEPTION_INIT
    : E X C E P T I O N '_' I N I T
    ;

EXCLUSIVE
    : E X C L U S I V E
    ;

EXISTS
    : E X I S T S
    ;

EXIT
    : E X I T
    ;

FETCH
    : F E T C H
    ;

FLOAT
    : F L O A T
    ;

FROM
    : F R O M
    ;

FUNCTION
    : F U N C T I O N
    ;

GOTO
    : G O T O
    ;

GRANT
    : G R A N T
    ;

GROUP
    : G R O U P
    ;

HAVING
    : H A V I N G
    ;

IDENTIFIED
    : I D E N T I F I E D
    ;

IF
    : I F
    ;

IN
    : I N
    ;

INDEX
    : I N D E X
    ;

INDEXES
    : I N D E X E S
    ;

INSERT
    : I N S E R T
    ;

NUMERIC
    : I N T E G E R
    | S M A L L I N T
    | I N T
    | N U M E R I C
    ;

INTERSECT
    : I N T E R S E C T
    ;

INTERVAL
    : I N T E R V A L
    ;

INTO
    : I N T O
    ;

IS
    : I S
    ;

LEVEL
    : L E V E L
    ;

LIKE
    : L I K E
    ;

LOOP
    : L O O P
    ;

MINUS
    : M I N U S
    ;

MODE
    : M O D E
    ;

NATURAL
    : N A T U R A L
    ;

NCHAR
    : N C H A R
    ;

NVARCHAR
    : N V A R C H A R
    ;

NVARCHAR2
    : N V A R C H A R '2'
    ;

NEW
    : N E W
    ;

NOCOMPRESS
    : N O C O M P R E S S
    ;

NO
    : N O
    ;

NOT
    : N O T
    ;

NOWAIT
    : N O W A I T
    ;

NULLX
    : N U L L
    ;

OF
    : O F
    ;

OLD
    : O L D
    ;

ON
    : O N
    ;

OPEN
    : O P E N
    ;

OPTION
    : O P T I O N
    ;

OR
    : O R
    ;

ORDER
    : O R D E R
    ;

OTHERS
    : O T H E R S
    ;

OUT
    : O U T
    ;

PACKAGE_P
    : P A C K A G E
    ;

PARENT
    : P A R E N T
    ;

PARTITION
    : P A R T I T I O N
    ;

POSITIVE
    : P O S I T I V E
    ;

PRAGMA
    : P R A G M A
    ;

PROCEDURE
    : P R O C E D U R E
    ;

PUBLIC
    : P U B L I C
    ;

RAISE
    : R A I S E
    ;

REAL
    : R E A L
    ;

RECORD
    : R E C O R D
    ;

REFERENCING
    : R E F E R E N C I N G
    ;

RESOURCE
    : R E S O U R C E
    ;

RETURN
    : R E T U R N
    ;

REVERSE
    : R E V E R S E
    ;

REVOKE
    : R E V O K E
    ;

ROLLBACK
    : R O L L B A C K
    ;

ROWID
    : R O W I D
    ;

ROWTYPE
    : R O W T Y P E
    ;

SAVEPOINT
    : S A V E P O I N T
    ;

SQL_KEYWORD
    : S E L E C T
    ;

SHARE
    : S H A R E
    ;

SET
    : S E T
    ;

SIZE
    : S I Z E
    ;

SQL
    : S Q L
    ;

START
    : S T A R T
    ;

STATEMENT
    : S T A T E M E N T
    ;

SUBTYPE
    : S U B T Y P E
    ;

TABAUTH
    : T A B A U T H
    ;

TABLE
    : T A B L E
    ;

THEN
    : T H E N
    ;

TO
    : T O
    ;

TRIGGER
    : T R I G G E R
    ;

TYPE
    : T Y P E
    ;

UNION
    : U N I O N
    ;

UNIQUE
    : U N I Q U E
    ;

UPDATE
    : U P D A T E
    ;

USING_NLS_COMP
    : U S I N G '_' N L S '_' C O M P
    ;

VALUES
    : V A L U E S
    ;

VARCHAR2
    : V A R C H A R
    | V A R C H A R '2'
    ;

VIEW
    : V I E W
    ;

VIEWS
    : V I E W S
    ;

WHEN
    : W H E N
    ;

WHERE
    : W H E R E
    ;

WHILE
    : W H I L E
    ;

WITH
    : W I T H
    ;

BINARY
    : B I N A R Y
    ;

CLOB
    : C L O B
    ;

BLOB
    : B L O B
    ;

CONSTRUCTOR
    : C O N S T R U C T O R
    ;

CONTINUE
    : C O N T I N U E
    ;

DEBUG
    : D E B U G
    ;

EXTERNAL
    : E X T E R N A L
    ;

FINAL
    : F I N A L
    ;

INSTANTIABLE
    : I N S T A N T I A B L E
    ;

LANGUAGE
    : L A N G U A G E
    ;

MAP
    : M A P
    ;

MEMBER
    : M E M B E R
    ;

NATURALN
    : N A T U R A L N
    ;

NOCOPY
    : N O C O P Y
    ;

OVERRIDING
    : O V E R R I D I N G
    ;

PLS_INTEGER
    : P L S '_' I N T E G E R
    ;

POSITIVEN
    : P O S I T I V E N
    ;

RAW
    : R A W
    ;

REUSE
    : R E U S E
    ;

SELF
    : S E L F
    ;

SIGNTYPE
    : S I G N T Y P E
    ;

SIMPLE_INTEGER
    : S I M P L E '_' I N T E G E R
    ;

STATIC
    : S T A T I C
    ;

TIMESTAMP
    : T I M E S T A M P
    ;

LABEL_LEFT
    : '<<'
    ;

LABEL_RIGHT
    : '>>'
    ;

ASSIGN_OPERATOR
    : ':='
    ;

RANGE_OPERATOR
    : '..' ->mode(DEFAULT_MODE)
    ;

PARAM_ASSIGN_OPERATOR
    : '=>'
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

JAVA
    : J A V A
    ;

MONTH
    : M O N T H
    ;

AFTER
    : A F T E R
    ;

SETTINGS
    : S E T T I N G S
    ;

YEAR
    : Y E A R
    ;

EACH
    : E A C H
    ;

PARALLEL_ENABLE
    : P A R A L L E L '_' E N A B L E
    ;

DECLARATION
    : D E C L A R A T I O N
    ;

VARCHAR
    : V A R C H A R
    ;

SERIALLY_REUSABLE
    : S E R I A L L Y '_' R E U S A B L E
    ;

CALL
    : C A L L
    ;

RETURNING
    : R E T U R N I N G
    ;

VARIABLE
    : V A R I A B L E
    ;

INSTEAD
    : I N S T E A D
    ;

RELIES_ON
    : R E L I E S '_' O N
    ;

LONG
    : L O N G
    ;

COLLECT
    : C O L L E C T
    ;

UNDER
    : U N D E R
    ;

REF
    : R E F
    ;

RightBracket
    : R I G H T B R A C K E T
    ;

IMMEDIATE
    : I M M E D I A T E
    ;

EDITIONABLE
    : E D I T I O N A B L E
    ;

UROWID
    : U R O W I D
    ;

REPLACE
    : R E P L A C E
    ;

VARYING
    : V A R Y I N G
    ;

DISABLE
    : D I S A B L E
    ;

NONEDITIONABLE
    : N O N E D I T I O N A B L E
    ;

FOR
    : F O R
    ;

NAME
    : N A M E
    ;

USING
    : U S I N G
    ;

YES
    : Y E S
    ;

TIME
    : T I M E
    ;

VALIDATE
    : V A L I D A T E
    ;

TRUST
    : T R U S T
    ;

AUTHID
    : A U T H I D
    ;

LOCK
    : L O C K
    ;

BULK
    : B U L K
    ;

INTERFACE
    : I N T E R F A C E
    ;

DEFINER
    : D E F I N E R
    ;

LeftBracket
    : L E F T B R A C K E T
    ;

AGGREGATE
    : A G G R E G A T E
    ;

BYTE
    : B Y T E
    ;

LOCAL
    : L O C A L
    ;

RNPS
    : R N P S
    ;

INDICES
    : I N D I C E S
    ;

HASH
    : H A S H
    ;

WNPS
    : W N P S
    ;

FORCE
    : F O R C E
    ;

OVERLAPS
    : O V E R L A P S
    ;

COLLATION
    : C O L L A T I O N
    ;

LOWER_PARENS
    : L O W E R '_' P A R E N S
    ;

COMPOUND
    : C O M P O U N D
    ;

SPECIFICATION
    : S P E C I F I C A T I O N
    ;

ACCESSIBLE
    : A C C E S S I B L E
    ;

SAVE
    : S A V E
    ;

COMPILE
    : C O M P I L E
    ;

COLLATE
    : C O L L A T E
    ;

SELECT
    : S E L E C T
    ;

EXECUTE
    : E X E C U T E
    ;

SQLDATA
    : S Q L D A T A
    ;

PIPELINED
    : P I P E L I N E D
    ;

DAY
    : D A Y
    ;

CURRENT_USER
    : C U R R E N T '_' U S E R
    ;

ZONE
    : Z O N E
    ;

PIPE
    : P I P E
    ;

VALUE
    : V A L U E
    ;

WNDS
    : W N D S
    ;

AUTONOMOUS_TRANSACTION
    : A U T O N O M O U S '_' T R A N S A C T I O N
    ;

UDF
    : U D F
    ;

INLINE
    : I N L I N E
    ;

MINUTE
    : M I N U T E
    ;

RESULT_CACHE
    : R E S U L T '_' C A C H E
    ;

ENABLE
    : E N A B L E
    ;

OID
    : O I D
    ;

OBJECT
    : O B J E C T
    ;

RESTRICT_REFERENCES
    : R E S T R I C T '_' R E F E R E N C E S
    ;

ROW
    : R O W
    ;

RANGE
    : R A N G E
    ;

HOUR
    : H O U R
    ;

ORADATA
    : O R A D A T A
    ;

FORALL
    : F O R A L L
    ;

LIMIT
    : L I M I T
    ;

VARRAY
    : V A R R A Y
    ;

CUSTOMDATUM
    : C U S T O M D A T U M
    ;

DETERMINISTIC
    : D E T E R M I N I S T I C
    ;

RNDS
    : R N D S
    ;

BEFORE
    : B E F O R E
    ;

CHARSET
    : C H A R S E T
    ;

NESTED
    : N E S T E D
    ;

SECOND
    : S E C O N D
    ;

RESULT
    : R E S U L T
    ;

DATE_VALUE
    : D A T E ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''
    | T I M E ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''
    | T I M E S T A M P ([ \t\n\r\f]+|('--'(~[\n\r])*))?'\''(~['])*'\''
    | D A T E ([ \t\n\r\f]+|('--'(~[\n\r])*))?'"'(~["])*'"'
    | T I M E ([ \t\n\r\f]+|('--'(~[\n\r])*))?'"'(~["])*'"'
    | T I M E S T A M P ([ \t\n\r\f]+|('--'(~[\n\r])*))?'"'(~["])*'"'
    ;

QUESTIONMARK
    : '?'
    | ':'[0-9]+
    ;

IDENT
    : ':'(([A-Za-z]|~[\u0000-\u007F\uD800-\uDBFF])([A-Za-z0-9$_#]|~[\u0000-\u007F\uD800-\uDBFF])*)
    | (([A-Za-z]|~[\u0000-\u007F\uD800-\uDBFF])([A-Za-z0-9$_#]|~[\u0000-\u007F\uD800-\uDBFF])*)
    | '`' ~[`]* '`'
    | '"' (~["]|('""'))* '"'

    ;

DELIMITER
    : ';'
    ;

Equal
    : '='
    ;

Or
    : [|]
    ;

Minus
    : [-]
    ;

Star
    : [*]
    ;

Div
    : [/]
    ;

Not
    : [!]
    ;

Caret
    : [^]
    ;

Colon
    : [:]
    ;

Mod
    : [%]
    ;

Dot
    : [.]
    ;

RightParen
    : [)]
    ;

LeftParen
    : [(]
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

Tilde
    : [~]
    ;

STRING
    : '\'' (~[']|('\'\'')|('\\\''))* '\''
    ;

In_c_comment
    : '/*' .*? '*/'      -> channel(1)
    ;

ANTLR_SKIP
    : '--'[ \t]* .*? '\n'   -> channel(1)
    ;

Blank
    : [ \t\r\n] -> channel(1)    ;

SQL_TOKEN_OR_UNKNOWN
    : (.) -> channel(1)    ;


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

