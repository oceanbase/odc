grammar LogicalTableExpression;

logicalTableExpression: schemaExpression DOT tableExpression ;

schemaExpression: (schemaSliceRangeWithBracket IDENTIFIER?)* IDENTIFIER (schemaSliceRangeWithBracket IDENTIFIER?)* ;

tableExpression: (tableSliceRangeWithBracket IDENTIFIER?)* IDENTIFIER (tableSliceRangeWithBracket IDENTIFIER?)* ;

schemaSliceRangeWithBracket: LEFT_BRACKET sliceRange RIGHT_BRACKET ;

tableSliceRangeWithBracket: schemaSliceRangeWithBracket | DOBULE_LEFT_BRACKET sliceRange DOUBLE_RIGHT_BRACKET;

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