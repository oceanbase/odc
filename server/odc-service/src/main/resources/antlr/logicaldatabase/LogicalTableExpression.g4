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

grammar LogicalTableExpression;
@header {
package com.oceanbase.odc.service.connection.logicaldatabase;
}
logicalTableExpressionList: logicalTableExpression (COMMA logicalTableExpression)* ;

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