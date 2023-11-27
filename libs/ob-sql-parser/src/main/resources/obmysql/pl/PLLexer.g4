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

ALTER
    : A L T E R
    ;

AFTER
    : A F T E R
    ;

BEFORE
    : B E F O R E
    ;

BEGIN_KEY
    : B E G I N
    ;

BINARY_INTEGER
    : B I N A R Y '_' I N T E G E R
    ;

BODY
    : B O D Y
    ;

BY
    : B Y
    ;

CASE
    : C A S E
    ;

CALL
    : C A L L
    ;

CATALOG_NAME
    : C A T A L O G '_' N A M E
    ;

CLASS_ORIGIN
    : C L A S S '_' O R I G I N
    ;

CLOSE
    : C L O S E
    ;

JSON
    : J S O N
    ;

COLUMN_NAME
    : C O L U M N '_' N A M E
    ;

COMMENT
    : C O M M E N T
    ;

COMMIT
    : C O M M I T
    ;

CONDITION
    : C O N D I T I O N
    ;

CONSTRAINT_CATALOG
    : C O N S T R A I N T '_' C A T A L O G
    ;

CONSTRAINT_NAME
    : C O N S T R A I N T '_' N A M E
    ;

CONSTRAINT_SCHEMA
    : C O N S T R A I N T '_' S C H E M A
    ;

CONTINUE
    : C O N T I N U E
    ;

COUNT
    : C O U N T
    ;

CREATE
    : C R E A T E
    ;

CURRENT_USER
    : C U R R E N T '_' U S E R
    ;

CURSOR
    : C U R S O R
    ;

CURSOR_NAME
    : C U R S O R '_' N A M E
    ;

DECLARE
    : D E C L A R E
    ;

DEFAULT
    : D E F A U L T
    ;

DEFINER
    : D E F I N E R
    ;

DELETE
    : D E L E T E
    ;

DETERMINISTIC
    : D E T E R M I N I S T I C
    ;

DO
    : D O
    ;

DD
    : D D
    ;

DROP
    : D R O P
    ;

EACH
    : E A C H
    ;

ELSE
    : E L S E
    ;

ELSEIF
    : E L S E I F
    ;

END_KEY
    : E N D
    ;

EXISTS
    : E X I S T S
    ;

EXIT
    : E X I T
    ;

EXTEND
    : E X T E N D
    ;

FOR
    : F O R
    ;

FOUND
    : F O U N D
    ;

FROM
    : F R O M
    ;

FUNCTION
    : F U N C T I O N
    ;

HANDLER
    : H A N D L E R
    ;

IF
    : I F
    ;

IN
    : I N
    ;

INSERT
    : I N S E R T
    ;

INTO
    : I N T O
    ;

IS
    : I S
    ;

INOUT
    : I N O U T
    ;

ITERATE
    : I T E R A T E
    ;

LEAVE
    : L E A V E
    ;

LIMIT
    : L I M I T
    ;

LONG
    : L O N G
    ;

LOOP
    : L O O P
    ;

MESSAGE_TEXT
    : M E S S A G E '_' T E X T
    ;

MYSQL_ERRNO
    : M Y S Q L '_' E R R N O
    ;

NEXT
    : N E X T
    ;

NOT
    : N O T
    ;

OF
    : O F
    ;

ON
    : O N
    ;

OPEN
    : O P E N
    ;

OUT
    : O U T
    ;

PACKAGE
    : P A C K A G E
    ;

PROCEDURE
    : P R O C E D U R E
    ;

TABLE
    : T A B L E
    ;

TABLE_NAME
    : T A B L E '_' N A M E
    ;

THEN
    : T H E N
    ;

TYPE
    : T Y P E
    ;

RECORD
    : R E C O R D
    ;

REPEAT
    : R E P E A T
    ;

RESIGNAL
    : R E S I G N A L
    ;

RETURN
    : R E T U R N
    ;

RETURNS
    : R E T U R N S
    ;

ROLLBACK
    : R O L L B A C K
    ;

ROW
    : R O W
    ;

ROWTYPE
    : R O W T Y P E
    ;

SCHEMA_NAME
    : S C H E M A '_' N A M E
    ;

SELECT
    : S E L E C T
    ;

SIGNAL
    : S I G N A L
    ;

SQLEXCEPTION
    : S Q L E X C E P T I O N
    ;

SQLSTATE
    : S Q L S T A T E
    ;

SQLWARNING
    : S Q L W A R N I N G
    ;

SUBCLASS_ORIGIN
    : S U B C L A S S '_' O R I G I N
    ;

UNTIL
    : U N T I L
    ;

USING
    : U S I N G
    ;

WHEN
    : W H E N
    ;

WHILE
    : W H I L E
    ;

LANGUAGE
    : L A N G U A G E
    ;

SQL
    : S Q L
    ;

NO
    : N O
    ;

CONTAINS
    : C O N T A I N S
    ;

READS
    : R E A D S
    ;

MODIFIES
    : M O D I F I E S
    ;

DATA
    : D A T A
    ;

CONSTRAINT_ORIGIN
    : C O N S T R A I N T '_' O R I G I N
    ;

INVOKER
    : I N V O K E R
    ;

SECURITY
    : S E C U R I T Y
    ;

TINYINT
    : T I N Y I N T
    ;

SMALLINT
    : S M A L L I N T
    ;

MEDIUMINT
    : M E D I U M I N T
    ;

INTEGER
    : I N T E G E R
    | I N T
    ;

BIGINT
    : B I G I N T
    ;

FETCH
    : F E T C H
    ;

FLOAT
    : F L O A T
    ;

DOUBLE
    : D O U B L E
    | R E A L
    ;

PRECISION
    : P R E C I S I O N
    ;

NUMBER
    : D E C
    | D E C I M A L
    | N U M E R I C
    ;

BIT
    : B I T
    ;

DATETIME
    : D A T E T I M E
    ;

TIMESTAMP
    : T I M E S T A M P
    ;

TIME
    : T I M E
    ;

DATE
    : D A T E
    ;

YEAR
    : Y E A R
    ;

CHARACTER
    : C H A R A C T E R
    | C H A R
    ;

TEXT
    : T E X T
    ;

VALUE
    : V A L U E
    ;

VARCHAR
    : V A R C H A R
    | V A R C H A R A C T E R
    ;

BINARY
    : B I N A R Y
    ;

VARBINARY
    : V A R B I N A R Y
    ;

UNSIGNED
    : U N S I G N E D
    ;

SIGNED
    : S I G N E D
    ;

ZEROFILL
    : Z E R O F I L L
    ;

COLLATE
    : C O L L A T E
    ;

SET
    : S E T
    ;

CHARSET
    : C H A R S E T
    ;

BOOL
    : B O O L
    ;

BOOLEAN
    : B O O L E A N
    ;

ENUM
    : E N U M
    ;

UPDATE
    : U P D A T E
    ;

TRIGGER
    : T R I G G E R
    ;

NULLX
    : N U L L
    ;

INTNUM
    : [0-9]+
    ;

DECIMAL_VAL
    : ([0-9]+ E [-+]?[0-9]+ | [0-9]+'.'[0-9]* E [-+]?[0-9]+ | '.'[0-9]+ E [-+]?[0-9]+)
    | ([0-9]+'.'[0-9]* | '.'[0-9]+)
    ;

NUMERIC
    : N U M E R I C
    ;

SQL_TOKEN
    : S Q L '_' T O K E N
    ;

SQL_KEYWORD
    : A L T E R
 | A N A L Y Z E
 | B I N A R Y
 | B I N L O G
 | C A C H E
 | C A L L
 | C H A N G E
 | C H E C K S U M
 | C O M M I T
 | C R E A T E
 | D A T A B A S E
 | D E L E T E
 | D E S '_' K E Y '_' F I L E
 | D O
 | D R O P
 | E R R O R S
 | E V E N T
 | E V E N T S
 | F L U S H
 | F U N C T I O N
 | G R A N T
 | H O S T S
 | I N D E X
 | I N S E R T
 | I N S T A L L
 | I N T O
 | K I L L
 | L O A D
 | L O C K
 | L O G S
 | M A S T E R
 | O P T I M I Z E
 | P L U G I N
 | P R I V I L E G E S
 | P R O C E D U R E
 | Q U E R Y
 | R E A D
 | R E N A M E
 | R E P A I R
 | R E P L A C E
 | R E S E T
 | R E V O K E
 | S E L E C T
 | S E T
 | S H O W
 | S L A V E
 | S T A R T
 | S T A T U S
 | S T O P
 | T A B L E
 | T A B L E S
 | T R U N C A T E
 | U N I N S T A L L
 | U P D A T E
 | U S E R
 | U S E R '_' R E S O U R C E S
 | V I E W
 | W A R N I N G S
 | W I T H
 | P R E P A R E
 | E X E C U T E
 | D E A L L O C A T E
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

SYSTEM_VARIABLE
    : ('@''@'[A-Za-z_][A-Za-z0-9_]*)
    ;

USER_VARIABLE
    : ('@'['"A-Za-z0-9_.$]*)
    ;

Equal
    : '='
    ;

DELIMITER
    : ';'
    ;

IDENT
    : ([A-Za-z0-9$_]*)'@'([A-Za-z0-9$_]*)
    | (([A-Za-z0-9$_]|(~[\u0000-\u007F\uD800-\uDBFF]))+)
    | '`' ~[`]* '`' ('@' '`' ~[`]* '`') ?
    ;

Or
    : [|]
    ;

Dot
    : [.]
    ;

RightParen
    : [)]
    ;

Minus
    : [-]
    ;

LeftParen
    : [(]
    ;

Comma
    : [,]
    ;

Star
    : [*]
    ;

Div
    : [/]
    ;

Plus
    : [+]
    ;

Not
    : [!]
    ;

And
    : [&]
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

Tilde
    : [~]
    ;

STRING
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

