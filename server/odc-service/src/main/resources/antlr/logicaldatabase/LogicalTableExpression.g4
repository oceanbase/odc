grammar LogicalTableExpression;
@header {
package com.oceanbase.odc.service.connection.logicaldatabase;
}

logicalTableExpression: schemaExpression DOT tableExpression ;

schemaExpression: (schemaSliceRangeWithBracket IDENTIFIER?)* IDENTIFIER (schemaSliceRangeWithBracket IDENTIFIER?)* ;

tableExpression: (tableSliceRangeWithBracket IDENTIFIER?)* IDENTIFIER (tableSliceRangeWithBracket IDENTIFIER?)* ;

schemaSliceRangeWithBracket: sliceRangeWithSingeBracket ;

tableSliceRangeWithBracket: sliceRangeWithSingeBracket | sliceRangeWithDoubleBracket ;

sliceRangeWithSingeBracket: LEFT_BRACKET sliceRange RIGHT_BRACKET ;

sliceRangeWithDoubleBracket: DOBULE_LEFT_BRACKET sliceRange DOUBLE_RIGHT_BRACKET ;

sliceRange: consecutiveRange | steppedRange | enumRange ;

consecutiveRange: NUMBER DASH NUMBER ;

steppedRange: NUMBER DASH NUMBER COLON NUMBER ;

enumRange: NUMBER (COMMA NUMBER)* ;

IDENTIFIER: [a-zA-Z_$][a-zA-Z0-9_$]* ;
NUMBER: '0'* [1-9][0-9]* | '0'+ ;
WS: [ \t\r\n]+ -> skip ;
DOT: [.] ;
COMMA: [,] ;
DASH: [-] ;
COLON: [:] ;
LEFT_BRACKET: '[' ;
RIGHT_BRACKET: ']' ;
DOBULE_LEFT_BRACKET: '[[' ;
DOUBLE_RIGHT_BRACKET: ']]' ;