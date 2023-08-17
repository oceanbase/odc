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

parser grammar PLParser;


options { tokenVocab=PLLexer; }


// start rule: null
@parser::header {
import com.oceanbase.tools.sqlparser.oboracle.OBLexer;
import com.oceanbase.tools.sqlparser.oboracle.OBParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
}
@parser::members {
Map<String, HashSet<Integer>> expr2endTokens = new HashMap<String, HashSet<Integer>>(){{
  put("opt_cexpr", new HashSet<Integer>() {{ add(Comma);add(RightParen);add(PARAM_ASSIGN_OPERATOR);}});
  put("pl_right_value", new HashSet<Integer>() {{ add(DELIMITER);}});
  put("expr", new HashSet<Integer>() {{ add(INTO);add(BULK);add(USING);add(WHEN);add(THEN);add(DELIMITER);
    add(LOOP);add(LIMIT);add(Comma);add(END_KEY);add(RANGE_OPERATOR);add(RightParen);}});
  put("bool_expr", new HashSet<Integer>() {{ add(WHEN);add(THEN);add(LOOP);add(Comma);add(RightParen);add(DELIMITER);}});
  put("return_expr", new HashSet<Integer>() {{ add(DELIMITER);}});
  put("for_expr", new HashSet<Integer>() {{ add(Comma);add(RANGE_OPERATOR);add(LOOP);add(INSERT);
    add(UPDATE);add(DELETE);add(SAVE);add(NOT);}});
  put("cursor_for_loop_sql", new HashSet<Integer>() {{ add(RightParen);}});
  put("sql_stmt", new HashSet<Integer>() {{ add(DELIMITER);}});
}};
class PLSQLErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e) throws RecognitionException {
      // just throw the exception to PL Parser without handling anything.
        if (e != null) {
             throw e;
        }
    }
}

class PLErrorStrategy extends DefaultErrorStrategy {
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        // do not recover even in ErrorRecoveryMode
    // the following code is copied from DefaultErrorStrategy#reportError
        this.beginErrorCondition(recognizer);
        if (e instanceof NoViableAltException) {
            this.reportNoViableAlternative(recognizer, (NoViableAltException) e);
        } else if (e instanceof InputMismatchException) {
            this.reportInputMismatch(recognizer, (InputMismatchException) e);
        } else if (e instanceof FailedPredicateException) {
            this.reportFailedPredicate(recognizer, (FailedPredicateException) e);
        } else {
            System.err.println("unknown recognition error type: " + e.getClass().getName());
            recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
        }
    }
    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException {
    	InputMismatchException e;
    	if (this.nextTokensContext == null) {
    		e = new InputMismatchException(recognizer);
    	} else {
    		e = new InputMismatchException(recognizer, this.nextTokensState, this.nextTokensContext);
    	}
    	throw e;
    }
}

public void ForwardSQL(ParserRuleContext ctx, String exprName) throws RecognitionException {
    // Forward sql string to the forward_sql_stmt node of SQL Parser
  String text = _input.getText(ctx.start, _input.LT(-1));
  text = addMoreLAT(text, exprName);
  CharStream cs = CharStreams.fromString(text);
    PLSQLErrorListener lexerErrorListener = new PLSQLErrorListener();
    PLSQLErrorListener parserErrorListener = new PLSQLErrorListener();
    OBLexer lexer = new OBLexer(cs);
    lexer.removeErrorListeners();
    lexer.addErrorListener(lexerErrorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    tokens.fill();
    OBParser parser = new OBParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(parserErrorListener);
    parser.setErrorHandler(new PLErrorStrategy());
    parser.is_pl_parse_ = true;
    parser.is_pl_parse_expr_ = false;
    while(ctx.getChildCount() != 0) {
    	ctx.removeLastChild();
    }
    ctx.addChild(parser.forward_sql_stmt());
}

public void ForwardExpr(ParserRuleContext ctx, String exprName) throws RecognitionException {
    // Forward sql string to the forward_expr node of SQL Parser
  String text = _input.getText(ctx.start, _input.LT(-1));
  if (text.length() == 0) {
    return;
  }
  text = addMoreLAT(text, exprName);
    CharStream cs = CharStreams.fromString(text);
    PLSQLErrorListener lexerErrorListener = new PLSQLErrorListener();
    PLSQLErrorListener parserErrorListener = new PLSQLErrorListener();
    OBLexer lexer = new OBLexer(cs);
    lexer.removeErrorListeners();
    lexer.addErrorListener(lexerErrorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    tokens.fill();
    OBParser parser = new OBParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(parserErrorListener);
    parser.setErrorHandler(new PLErrorStrategy());
    parser.is_pl_parse_ = true;
    parser.is_pl_parse_expr_ = true;
    while(ctx.getChildCount() != 0) {
    	ctx.removeLastChild();
    }
    ctx.addChild(parser.forward_expr());
}

private String addMoreLAT(String text, String exprName){
    int leftParenCount = 0;
    int rightParenCount = 0;
    boolean inSingleQuote = false;
	boolean inDoubleQuote = false;
	boolean inEscape = false;
	for (char c : text.toCharArray()) {
	    if (inEscape) {
    		inEscape = false;
    		continue;
    	}
    	if (c == '\\' && (inDoubleQuote || inSingleQuote)) {
    		inEscape = true;
    		continue;
    	}
		if (c == '\'' && !inDoubleQuote) {
	    	inSingleQuote = !inSingleQuote;
	    }
		if (c == '"' && !inSingleQuote) {
			inDoubleQuote = !inDoubleQuote;
		}
	    if (!inSingleQuote && !inDoubleQuote && c == '(') {
	        leftParenCount++;
	    } else if (!inSingleQuote && !inDoubleQuote && c == ')') {
	        rightParenCount++;
	    }
	}
    // Refer from obpl_oracle_read_sql_construct in pl_parser_oracle_mode.y
  Set<Integer> endTokens = expr2endTokens.get(exprName);
    boolean isBreak = leftParenCount == rightParenCount;
    while (!isBreak) {
      // get the next lookahead token
        int _la = _input.LA(1);
        if (_la == EOF) {
      isBreak = true;
          break;
    }
        if (leftParenCount == rightParenCount && endTokens.contains(_la)) {
          isBreak = true;
          break;
        } else if (_la == LeftParen) {
          leftParenCount++;
        } else if (_la == RightParen) {
          rightParenCount++;
        }
        text += _input.LT(1).getText();
        // match and consume the current lookahead token
        _errHandler.reportMatch(this);
        consume();
    }
    return text;
}
}

pl_entry_stmt_list
    : pl_entry_stmt DELIMITER? EOF
    ;

pl_entry_stmt
    : anonymous_stmt
    | pl_ddl_stmt
    | plsql_procedure_source
    | plsql_function_source
    | plsql_trigger_source
    | update_column_list
    | constructor_spec
    | constructor_def
    ;

pl_ddl_stmt
    : create_package_stmt
    | create_package_body_stmt
    | alter_package_stmt
    | drop_package_stmt
    | create_procedure_stmt
    | create_function_stmt
    | create_trigger_stmt
    | alter_procedure_stmt
    | alter_function_stmt
    | alter_trigger_stmt
    | drop_procedure_stmt
    | drop_function_stmt
    | drop_trigger_stmt
    | package_block
    | package_body_block
    | create_type_stmt
    | drop_type_stmt
    | create_type_body_stmt
    | plsql_type_spec_source
    | plsql_type_body_source
    ;

inner_call_stmt
    : pl_obj_access_ref
    ;

sp_cparam_list
    : LeftParen opt_sp_cparams RightParen
    ;

opt_sp_cparams
    : sp_cparam (Comma sp_cparam)*
    ;

sp_cparam
    : opt_cexpr (PARAM_ASSIGN_OPERATOR opt_cexpr)?
    ;

opt_cexpr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "opt_cexpr");}
    ;

anonymous_stmt
    : label_list? pl_block
    ;

invoke_right
    : AUTHID CURRENT_USER
    | AUTHID DEFINER
    ;

unit_kind
    : FUNCTION
    | PROCEDURE
    | PACKAGE_P
    | TRIGGER
    | TYPE
    ;

accessor
    : unit_kind? pl_schema_name
    ;

accessor_list
    : accessor (Comma accessor)*
    ;

accessible_by
    : ACCESSIBLE BY LeftParen accessor_list RightParen
    ;

proc_clause
    : invoke_right
    | accessible_by
    ;

proc_clause_list
    : proc_clause+
    ;

sp_deterministic
    : DETERMINISTIC
    ;

hash_or_range
    : HASH
    | RANGE
    ;

common_identifier
    : IDENT
    | unreserved_keyword
    ;

identifier
    : IDENT
    | unreserved_keyword
    | unreserved_special_keyword
    ;

sql_keyword_identifier
    : MERGE
    | EXISTS
    ;

argument
    : identifier
    ;

column
    : identifier
    ;

column_list
    : column (Comma column)*
    ;

order_or_cluster
    : ORDER
    | CLUSTER
    ;

stream_clause
    : order_or_cluster expr BY LeftParen column_list RightParen
    ;

partition_by
    : ANY
    | VALUE LeftParen column RightParen
    | hash_or_range LeftParen column_list RightParen stream_clause?
    ;

parallel_enable
    : PARALLEL_ENABLE
    | PARALLEL_ENABLE LeftParen PARTITION argument BY partition_by RightParen
    ;

data_source
    : identifier
    ;

data_source_list
    : data_source (Comma data_source)*
    ;

relies_on_clause
    : RELIES_ON LeftParen data_source_list? RightParen
    ;

result_cache
    : RESULT_CACHE relies_on_clause?
    ;

sf_clause
    : invoke_right
    | accessible_by
    | sp_deterministic
    | parallel_enable
    | result_cache
    ;

sf_clause_list
    : sf_clause+
    ;

create_package_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? package_block
    ;

package_block
    : PACKAGE_P pl_schema_name proc_clause_list? is_or_as decl_stmt_list? END_KEY (identifier | sql_keyword_identifier)?
    ;

create_package_body_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? package_body_block
    ;

package_body_block
    : PACKAGE_P BODY pl_schema_name is_or_as decl_stmt_ext_list? execute_section? END_KEY (identifier | sql_keyword_identifier)?
    ;

alter_package_stmt
    : ALTER PACKAGE_P pl_schema_name alter_package_clause
    ;

alter_package_clause
    : COMPILE DEBUG? (PACKAGE_P | SPECIFICATION | BODY)? (REUSE SETTINGS)?
    | EDITIONABLE
    | NONEDITIONABLE
    ;

drop_package_stmt
    : DROP PACKAGE_P BODY? pl_schema_name
    ;

pl_schema_name
    : identifier (Dot identifier)?
    | sql_keyword_identifier
    ;

pl_access_name
    : identifier Dot identifier ((Dot identifier)?|Dot DELETE)
    | identifier (Dot DELETE)?
    ;

var_common_name
    : IDENT
    | unreserved_keyword
    | MAP
    ;

field_name
    : identifier
    ;

type_name
    : identifier
    ;

func_name
    : identifier
    | sql_keyword_identifier
    ;

proc_name
    : identifier
    | sql_keyword_identifier
    ;

param_name
    : identifier
    ;

cursor_name
    : identifier
    | QUESTIONMARK
    ;

label_name
    : identifier
    ;

exception_name
    : identifier
    ;

is_or_as
    : IS
    | AS
    ;

create_procedure_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? plsql_procedure_source
    ;

create_function_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? plsql_function_source
    ;

create_trigger_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? plsql_trigger_source
    ;

plsql_procedure_source
    : PROCEDURE pl_schema_name (LeftParen sp_param_list RightParen)? proc_clause_list? is_or_as pl_impl_body
    ;

plsql_function_source
    : FUNCTION pl_schema_name (LeftParen sp_param_list RightParen)? RETURN pl_outer_data_type sf_clause_list? PIPELINED? is_or_as pl_impl_body
    | FUNCTION pl_schema_name (LeftParen sp_param_list RightParen)? RETURN pl_outer_data_type sf_clause_list? AGGREGATE USING pl_schema_name
    ;

plsql_trigger_source
    : TRIGGER pl_schema_name (DEFAULT COLLATION USING_NLS_COMP)? trigger_definition
    ;

trigger_definition
    : simple_dml_trigger
    | compound_dml_trigger
    | instead_of_dml_trigger
    ;

simple_dml_trigger
    : before_or_after dml_event_option (REFERENCING referencing_list)? (FOR EACH ROW)? enable_or_disable? (WHEN LeftParen bool_expr RightParen)? simple_trigger_body
    ;

compound_dml_trigger
    : FOR dml_event_option enable_or_disable? (WHEN LeftParen bool_expr RightParen)? compound_trigger_body
    ;

instead_of_dml_trigger
    : INSTEAD OF dml_event_option (REFERENCING referencing_list)? (FOR EACH ROW)? enable_or_disable? (WHEN LeftParen bool_expr RightParen)? simple_trigger_body
    ;

before_or_after
    : BEFORE
    | AFTER
    ;

dml_event_option
    : dml_event_list ON nested_table_column? pl_schema_name
    ;

dml_event_list
    : dml_event_tree
    ;

dml_event_tree
    : dml_event (OR dml_event)*
    ;

dml_event
    : INSERT (OF column_list)?
    | DELETE (OF column_list)?
    | UPDATE (OF column_list)?
    ;

update_column_list
    : UPDATE OF column_list
    ;

nested_table_column
    : NESTED TABLE column OF
    ;

referencing_list
    : referencing_node+
    ;

referencing_node
    : OLD AS? ref_name
    | NEW AS? ref_name
    | PARENT AS? ref_name
    ;

ref_name
    : identifier
    ;

simple_trigger_body
    : pl_block
    ;

compound_trigger_body
    : COMPOUND TRIGGER decl_stmt_ext_list? timing_point_section_list END_KEY (identifier | sql_keyword_identifier)?
    ;

timing_point_section_list
    : timing_point_section (Comma timing_point_section)*
    ;

timing_point_section
    : timing_point IS tps_body timing_point
    ;

timing_point
    : BEFORE (EACH ROW|STATEMENT)
    | AFTER (EACH ROW|STATEMENT)
    | INSTEAD OF EACH ROW
    ;

tps_body
    : execute_section END_KEY
    ;

sp_param_list
    : sp_param (Comma sp_param)*
    ;

sp_param
    : param_name pl_outer_data_type default_opt?
    | param_name IN pl_outer_data_type default_opt?
    | param_name OUT pl_outer_data_type default_opt?
    | param_name OUT NOCOPY pl_outer_data_type default_opt?
    | param_name IN OUT pl_outer_data_type default_opt?
    | param_name IN OUT NOCOPY pl_outer_data_type default_opt?
    ;

pl_impl_body
    : pl_body
    | call_spec
    | EXTERNAL
    ;

call_spec
    : LANGUAGE
    ;

pl_lang_stmt
    : pl_lang_stmt_without_semicolon DELIMITER
    ;

pl_lang_stmt_without_semicolon
    : inline_pragma
    | assign_stmt
    | sql_stmt
    | if_stmt
    | case_stmt
    | basic_loop_stmt
    | while_loop_stmt
    | for_loop_stmt
    | cursor_for_loop_stmt
    | forall_stmt
    | return_stmt
    | continue_stmt
    | exit_stmt
    | open_stmt
    | fetch_stmt
    | close_stmt
    | execute_immediate_stmt
    | raise_stmt
    | pl_block
    | goto_stmt
    | inner_call_stmt
    | null_stmt
    | pipe_row_stmt
    ;

assign_stmt
    : (QUESTIONMARK|pl_left_value) ASSIGN_OPERATOR pl_right_value
    ;

pl_left_value_list
    : pl_left_value (Comma pl_left_value_list)?
    ;

pl_left_value
    : pl_obj_access_ref
    ;

pl_obj_access_ref
    : pl_access_name pl_obj_access_ref_suffix_list?
    | QUESTIONMARK pl_obj_access_ref_suffix_list
    ;

pl_obj_access_ref_suffix_list
    : pl_obj_access_ref_suffix pl_obj_access_ref_suffix_list?
    ;

pl_obj_access_ref_suffix
    : Dot DELETE
    | Dot identifier
    | LeftParen opt_sp_cparams RightParen
    ;

pl_right_value
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "pl_right_value");}
    ;

if_stmt
    : IF sp_if END_KEY IF
    ;

sp_if
    : bool_expr THEN pl_lang_stmt_list ((ELSIF sp_if) | (ELSE pl_lang_stmt_list))?
    ;

case_stmt
    : CASE expr sp_when_list ((ELSIF sp_if) | (ELSE pl_lang_stmt_list))? END_KEY CASE label_name?
    ;

sp_when_list
    : sp_when+
    ;

sp_when
    : WHEN bool_expr THEN pl_lang_stmt_list
    ;

pl_block
    : ((DECLARE declare_section) | DECLARE)? execute_section END_KEY (identifier | sql_keyword_identifier)?
    ;

pl_body
    : declare_section execute_section END_KEY (identifier | sql_keyword_identifier)?
    | execute_section END_KEY (identifier | sql_keyword_identifier)?
    ;

declare_section
    : decl_stmt_ext_list
    ;

execute_section
    : BEGIN_KEY pl_lang_stmt_list (EXCEPTION exception_section)?
    ;

exception_section
    : exception_handler exception_section?
    ;

pl_lang_stmt_list
    : labeled_pl_lang_stmt+
    ;

decl_stmt_list
    : decl_stmt+
    ;

decl_stmt
    : decl_stmt_without_semicolon DELIMITER
    ;

decl_stmt_without_semicolon
    : type_def
    | subtype_def
    | cursor_decl
    | cursor_def
    | item_decl
    | func_decl
    | proc_decl
    ;

item_decl
    : var_decl
    | exception_decl
    | pragma_stmt
    ;

exception_decl
    : exception_name EXCEPTION
    ;

var_decl
    : var_common_name pl_inner_data_type (NOT NULLX)? default_opt?
    | TYPE pl_inner_data_type (NOT NULLX)? default_opt?
    | var_common_name CONSTANT pl_inner_data_type (NOT NULLX)? default_opt
    | TYPE CONSTANT pl_inner_data_type (NOT NULLX)? default_opt
    ;

decl_stmt_ext_list
    : decl_stmt_ext+
    ;

decl_stmt_ext
    : decl_stmt_ext_without_semicolon DELIMITER
    ;

decl_stmt_ext_without_semicolon
    : decl_stmt_without_semicolon
    | func_def
    | proc_def
    ;

func_decl
    : FUNCTION func_name (LeftParen sp_param_list RightParen)? RETURN pl_outer_data_type sf_clause_list? PIPELINED?
    ;

func_def
    : func_decl is_or_as pl_impl_body
    ;

proc_decl
    : PROCEDURE proc_name (LeftParen sp_param_list RightParen)? proc_clause_list?
    ;

proc_def
    : proc_decl is_or_as pl_impl_body
    ;

cursor_decl
    : CURSOR cursor_name (LeftParen sp_param_list RightParen)? return_type
    ;

return_type
    : RETURN pl_outer_data_type
    ;

cursor_sql_stmt
    : sql_stmt
    | multi_left_brackets cursor_for_loop_sql multi_right_brackets
    ;

cursor_def
    : CURSOR cursor_name (LeftParen sp_param_list RightParen)? return_type? IS cursor_sql_stmt
    ;

pl_inner_data_type
    : pl_inner_scalar_data_type
    | pl_obj_access_ref ((Mod TYPE)?|Mod ROWTYPE)
    ;

pl_outer_data_type
    : pl_outer_scalar_data_type
    | pl_obj_access_ref ((Mod TYPE)?|Mod ROWTYPE)
    ;

default_opt
    : DEFAULT default_expr
    | ASSIGN_OPERATOR default_expr
    ;

expr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "expr");}
    ;

bool_expr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "bool_expr");}
    ;

default_expr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "default_expr");}
    ;

return_expr
    : (~(DELIMITER))*?{this.ForwardExpr($ctx, "return_expr");}
    ;

basic_loop_stmt
    : LOOP pl_lang_stmt_list END_KEY LOOP label_name?
    ;

while_loop_stmt
    : WHILE bool_expr LOOP pl_lang_stmt_list END_KEY LOOP label_name?
    ;

for_loop_stmt
    : FOR identifier IN REVERSE? lower_bound RANGE_OPERATOR upper_bound LOOP pl_lang_stmt_list END_KEY LOOP label_name?
    ;

cursor_for_loop_stmt
    : FOR identifier IN REVERSE? for_expr LOOP pl_lang_stmt_list END_KEY LOOP label_name?
    | FOR identifier IN multi_left_brackets cursor_for_loop_sql multi_right_brackets LOOP pl_lang_stmt_list END_KEY LOOP label_name?
    ;

multi_left_brackets
    : LeftParen+
    ;

multi_right_brackets
    : RightParen+
    ;

forall_stmt
    : FORALL identifier IN bound_clause (SAVE EXCEPTIONS)? forall_sql_stmt
    ;

forall_sql_stmt
    : sql_stmt
    | execute_immediate_stmt
    ;

for_expr
    : ~(LeftParen) (.)*?{this.ForwardExpr($ctx, "for_expr");}
    ;

lower_bound
    : for_expr
    ;

upper_bound
    : for_expr
    ;

bound_clause
    : lower_bound RANGE_OPERATOR upper_bound
    | INDICES OF pl_obj_access_ref (BETWEEN lower_bound AND upper_bound)?
    | VALUES OF pl_obj_access_ref
    ;

cursor_for_loop_sql
    : sql_keyword (~(DELIMITER))*?{this.ForwardSQL($ctx, "cursor_for_loop_sql");}
    ;

labeled_pl_lang_stmt
    : label_list? pl_lang_stmt
    ;

label_list
    : label_def+
    ;

label_def
    : LABEL_LEFT label_name LABEL_RIGHT
    ;

return_stmt
    : RETURN return_expr
    ;

goto_stmt
    : GOTO label_name
    ;

continue_stmt
    : CONTINUE label_name? WHEN bool_expr
    | CONTINUE label_name
    ;

exit_stmt
    : EXIT label_name? WHEN bool_expr
    | EXIT label_name
    ;

null_stmt
    : NULLX
    ;

pipe_row_stmt
    : PIPE ROW LeftParen expr RightParen
    ;

pragma_stmt
    : inline_pragma
    | exception_init_pragma
    | udf_pragma
    | serially_reusable_pragma
    | restrict_references_pragma
    | autonomous_transaction_pragma
    | interface_pragma
    ;

inline_pragma
    : PRAGMA INLINE LeftParen identifier Comma STRING RightParen
    ;

exception_init_pragma
    : PRAGMA EXCEPTION_INIT LeftParen exception_init_param_list RightParen
    ;

exception_init_param_list
    : exception_init_param (Comma exception_init_param)*
    ;

exception_init_param
    : exception_name
    | error_code
    ;

udf_pragma
    : PRAGMA UDF
    ;

serially_reusable_pragma
    : PRAGMA SERIALLY_REUSABLE
    ;

restrict_references_pragma
    : PRAGMA RESTRICT_REFERENCES LeftParen default_or_string Comma assert_list RightParen
    ;

autonomous_transaction_pragma
    : PRAGMA AUTONOMOUS_TRANSACTION
    ;

interface_pragma
    : PRAGMA INTERFACE LeftParen C Comma identifier RightParen
    ;

error_code
    : Minus? INTNUM
    | STRING
    ;

exception_handler
    : WHEN exception_pattern THEN pl_lang_stmt_list
    ;

exception_pattern
    : exception_list
    ;

exception_list
    : pl_access_name (OR pl_access_name)*
    ;

drop_procedure_stmt
    : DROP PROCEDURE (IF EXISTS)? pl_schema_name
    ;

drop_function_stmt
    : DROP FUNCTION (IF EXISTS)? pl_schema_name
    ;

drop_trigger_stmt
    : DROP TRIGGER pl_schema_name
    ;

record_member_list
    : record_member (Comma record_member)*
    ;

record_member
    : field_name pl_inner_data_type (NOT NULLX)? ((ASSIGN_OPERATOR pl_right_value) | (DEFAULT pl_right_value))?
    ;

type_def
    : ref_cursor_type_def
    | record_type_def
    | collection_type_def
    ;

subtype_def
    : SUBTYPE type_name IS basetype_of_subtype (NOT NULLX)?
    ;

basetype_of_subtype
    : pl_inner_data_type subtype_range?
    ;

subtype_range
    : RANGE lower_bound RANGE_OPERATOR upper_bound
    ;

ref_cursor_type_def
    : TYPE type_name IS REF CURSOR return_type?
    ;

record_type_def
    : TYPE type_name IS RECORD LeftParen record_member_list RightParen
    ;

collection_type_def
    : TYPE type_name IS coll_type_def
    ;

coll_type_def
    : assoc_array_type_def
    | nested_table_type_def
    | varray_type_def
    ;

nested_table_type_def
    : TABLE OF pl_inner_data_type (NOT NULLX)?
    ;

assoc_array_type_def
    : TABLE OF pl_inner_data_type (NOT NULLX)? INDEX BY index_type
    ;

index_type
    : pl_inner_data_type
    ;

varray_type_def
    : pre_varray LeftParen INTNUM RightParen OF pl_inner_data_type (NOT NULLX)?
    ;

pre_varray
    : VARRAY
    | VARYING? ARRAY
    ;

pl_inner_scalar_data_type
    : NUMERIC number_precision
    | NUMBER number_precision
    | FLOAT LeftParen INTNUM RightParen
    | FLOAT LeftParen precision_decimal_num RightParen
    | FLOAT LeftParen RightParen
    | REAL LeftParen INTNUM RightParen
    | REAL LeftParen precision_decimal_num RightParen
    | TIMESTAMP LeftParen INTNUM RightParen
    | TIMESTAMP LeftParen precision_decimal_num RightParen
    | TIMESTAMP ((LeftParen INTNUM RightParen) | (LeftParen precision_decimal_num RightParen))? WITH TIME ZONE
    | TIMESTAMP ((LeftParen INTNUM RightParen) | (LeftParen precision_decimal_num RightParen))? WITH LOCAL TIME ZONE
    | CHARACTER collation
    | CHARACTER charset_key charset_name collation?
    | CHARACTER BINARY (charset_key charset_name)? collation?
    | CHARACTER string_length_i BINARY? (charset_key charset_name)? collation?
    | RAW LeftParen INTNUM RightParen
    | NCHAR collation
    | NCHAR charset_key charset_name collation?
    | NCHAR BINARY (charset_key charset_name)? collation?
    | NCHAR string_length_i BINARY? (charset_key charset_name)? collation?
    | VARCHAR string_length_i BINARY? (charset_key charset_name)? collation?
    | VARCHAR2 string_length_i BINARY? (charset_key charset_name)? collation?
    | NVARCHAR string_length_i BINARY? (charset_key charset_name)? collation?
    | NVARCHAR2 string_length_i BINARY? (charset_key charset_name)? collation?
    | INTERVAL YEAR ((LeftParen INTNUM RightParen) | (LeftParen precision_decimal_num RightParen))? TO MONTH
    | INTERVAL DAY ((LeftParen INTNUM RightParen) | (LeftParen precision_decimal_num RightParen))? TO SECOND ((LeftParen INTNUM RightParen) | (LeftParen precision_decimal_num RightParen))?
    | CLOB collation
    | CLOB charset_key charset_name collation?
    | CLOB BINARY (charset_key charset_name)? collation?
    | UROWID urowid_length_i
    | ROWID urowid_length_i
    ;

precision_decimal_num
    : DECIMAL_VAL
    ;

pl_outer_scalar_data_type
    : TIMESTAMP WITH TIME ZONE
    | TIMESTAMP WITH LOCAL TIME ZONE
    | CHARACTER collation
    | CHARACTER charset_key charset_name collation?
    | CHARACTER BINARY (charset_key charset_name)? collation?
    | NCHAR collation
    | NCHAR charset_key charset_name collation?
    | NCHAR BINARY (charset_key charset_name)? collation?
    | VARCHAR collation
    | VARCHAR charset_key charset_name collation?
    | VARCHAR BINARY (charset_key charset_name)? collation?
    | VARCHAR2 collation
    | VARCHAR2 charset_key charset_name collation?
    | VARCHAR2 BINARY (charset_key charset_name)? collation?
    | nvarchar_type_i collation
    | nvarchar_type_i charset_key charset_name collation?
    | nvarchar_type_i BINARY (charset_key charset_name)? collation?
    | INTERVAL YEAR TO MONTH
    | INTERVAL DAY TO SECOND
    | CLOB collation
    | CLOB charset_key charset_name collation?
    | CLOB BINARY (charset_key charset_name)? collation?
    ;

nvarchar_type_i
    : NVARCHAR
    | NVARCHAR2
    ;

number_precision
    : LeftParen (precision_decimal_num|signed_int_num (Comma signed_int_num)?) RightParen
    ;

signed_int_num
    : Minus? INTNUM
    ;

number_literal
    : INTNUM
    | DECIMAL_VAL
    ;

string_length_i
    : LeftParen number_literal (CHARACTER | BYTE)? RightParen
    ;

urowid_length_i
    : LeftParen INTNUM RightParen
    ;

collation_name
    : identifier
    | STRING
    ;

charset_name
    : identifier
    | STRING
    ;

charset_key
    : CHARSET
    | CHARACTER SET
    ;

collation
    : COLLATE collation_name
    ;

open_stmt
    : OPEN pl_access_name sp_cparam_list?
    | OPEN cursor_name for_sql (USING using_list)?
    ;

for_sql
    : FOR sql_stmt
    | FOR expr
    ;

fetch_stmt
    : FETCH pl_access_name into_clause
    | FETCH pl_access_name bulk_collect_into_clause (LIMIT expr)?
    ;

into_clause
    : INTO pl_left_value_list
    ;

bulk_collect_into_clause
    : BULK COLLECT INTO pl_left_value_list
    ;

close_stmt
    : CLOSE pl_access_name
    ;

execute_immediate_stmt
    : EXECUTE IMMEDIATE expr normal_into_clause? (USING using_list)? ((RETURNING normal_into_clause) | (RETURN normal_into_clause))?
    ;

raise_stmt
    : RAISE pl_access_name
    ;

normal_into_clause
    : into_clause
    | bulk_collect_into_clause
    ;

using_list
    : using_params
    ;

using_params
    : using_param (Comma using_param)*
    ;

using_param
    : (IN | OUT | (OUT NOCOPY) | (IN OUT) | (IN OUT NOCOPY))? expr
    ;

sql_keyword
    : SQL_KEYWORD
    | INSERT
    | UPDATE
    | DELETE
    | TABLE
    | SAVEPOINT
    | WITH
    | SET
    | MERGE
    ;

sql_stmt
    : sql_keyword (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | COMMIT (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    | ROLLBACK (~(DELIMITER))*?{this.ForwardSQL($ctx, "sql_stmt");}
    ;

alter_procedure_stmt
    : ALTER PROCEDURE pl_schema_name sp_alter_clause
    ;

alter_function_stmt
    : ALTER FUNCTION pl_schema_name sp_alter_clause
    ;

alter_trigger_stmt
    : ALTER TRIGGER pl_schema_name enable_or_disable
    ;

enable_or_disable
    : ENABLE
    | DISABLE
    ;

procedure_compile_clause
    : COMPILE (REUSE SETTINGS)?
    | COMPILE DEBUG (REUSE SETTINGS)?
    | COMPILE compiler_parameter_list (REUSE SETTINGS)?
    | COMPILE DEBUG compiler_parameter_list (REUSE SETTINGS)?
    ;

compiler_parameter
    : identifier Equal identifier
    ;

compiler_parameter_list
    : compiler_parameter+
    ;

sp_editionable
    : EDITIONABLE
    | NONEDITIONABLE
    ;

sp_alter_clause
    : procedure_compile_clause
    | sp_editionable
    ;

create_type_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? plsql_type_spec_source
    ;

plsql_type_spec_source
    : TYPE pl_schema_name FORCE? (OID STRING)? (object_type_def | (is_or_as varray_type_def) | (is_or_as nested_table_type_def))?
    ;

create_type_body_stmt
    : CREATE (OR REPLACE)? (EDITIONABLE | NONEDITIONABLE)? plsql_type_body_source
    ;

plsql_type_body_source
    : TYPE BODY pl_schema_name is_or_as plsql_type_body_decl_list_semicolon END_KEY
    ;

plsql_type_body_decl_list_semicolon
    : plsql_type_body_decl_list DELIMITER
    ;

plsql_type_body_decl_list
    : plsql_type_body_decl (DELIMITER plsql_type_body_decl)*
    ;

plsql_type_body_decl
    : subprog_decl_in_type
    | map_order_function_spec
    ;

subprog_decl_in_type
    : proc_or_func_def_in_type
    | constructor_def_in_type
    ;

proc_or_func_def_in_type
    : member_or_static proc_def
    | member_or_static func_def
    ;

constructor_def_in_type
    : final_inst_list? constructor_def
    ;

object_type_def
    : object_or_under (EXTERNAL NAME pl_schema_name LANGUAGE JAVA USING sqlj_using)? attr_and_element_spec final_inst_list?
    | proc_clause_list object_or_under (EXTERNAL NAME pl_schema_name LANGUAGE JAVA USING sqlj_using)? attr_and_element_spec final_inst_list?
    ;

object_or_under
    : is_or_as OBJECT
    | UNDER pl_schema_name
    ;

sqlj_using
    : SQLDATA
    | CUSTOMDATUM
    | ORADATA
    ;

final_or_inst
    : NOT? FINAL
    | NOT? INSTANTIABLE
    ;

final_inst_list
    : final_or_inst+
    ;

attr_and_element_spec
    : LeftParen attr_list (Comma element_spec)? RightParen
    ;

attr_list
    : attr_spec (Comma attr_spec)*
    ;

attr_spec
    : common_identifier pl_inner_data_type (EXTERNAL NAME STRING)?
    | MAP pl_inner_data_type (EXTERNAL NAME STRING)?
    | TYPE pl_inner_data_type (EXTERNAL NAME STRING)?
    | SUBTYPE pl_inner_data_type (EXTERNAL NAME STRING)?
    ;

element_spec
    : el_element_spec_list_cc
    ;

element_spec_long
    : (OVERRIDING inheritance_final_instantiable_clause|OVERRIDING?) el_element_spec
    | NOT OVERRIDING inheritance_final_instantiable_clause? el_element_spec
    | NOT? FINAL inheritance_overriding_instantiable_clause? el_element_spec
    | NOT? INSTANTIABLE inheritance_overriding_final_clause? el_element_spec
    | restrict_references_pragma
    ;

inheritance_final_instantiable_clause
    : final_clause instantiable_clause?
    | instantiable_clause final_clause?
    ;

inheritance_overriding_instantiable_clause
    : overriding_clause instantiable_clause?
    | instantiable_clause overriding_clause?
    ;

inheritance_overriding_final_clause
    : overriding_clause final_clause?
    | final_clause overriding_clause?
    ;

overriding_clause
    : NOT? OVERRIDING
    ;

final_clause
    : NOT? FINAL
    ;

instantiable_clause
    : NOT? INSTANTIABLE
    ;

el_element_spec_list_cc
    : element_spec_long (Comma element_spec_long)*
    ;

el_element_spec
    : subprogram_spec
    | constructor_spec
    | map_order_function_spec
    ;

default_or_string
    : DEFAULT
    | identifier
    ;

assert_list
    : assert_item (Comma assert_item)*
    ;

assert_item
    : TRUST
    | RNDS
    | WNDS
    | RNPS
    | WNPS
    ;

subprogram_spec
    : member_or_static proc_or_func_spec
    ;

member_or_static
    : MEMBER
    | STATIC
    ;

proc_or_func_spec
    : proc_decl
    | proc_def
    | func_decl
    | func_def
    | sqlj_func_decl
    ;

sqlj_func_decl
    : FUNCTION func_name (LeftParen sp_param_list RightParen)? sqlj_obj_type_sig
    ;

sqlj_obj_type_sig
    : RETURN type_or_self EXTERNAL varname_or_name
    ;

type_or_self
    : pl_outer_data_type
    | SELF AS RESULT
    ;

varname_or_name
    : VARIABLE? NAME STRING
    ;

constructor_spec
    : CONSTRUCTOR FUNCTION func_name (LeftParen sp_param_list RightParen)? RETURN SELF AS RESULT
    ;

constructor_def
    : constructor_spec opt_constructor_impl
    ;

opt_constructor_impl
    : is_or_as pl_impl_body
    ;

map_order_function_spec
    : (MAP|ORDER) MEMBER proc_or_func_spec
    ;

drop_type_stmt
    : DROP TYPE pl_schema_name pl_schema_name?
    ;

unreserved_keyword
    : ACCESSIBLE
    | AUTHID
    | AFTER
    | AGGREGATE
    | AUTONOMOUS_TRANSACTION
    | BLOB
    | BINARY
    | BINARY_DOUBLE
    | BINARY_FLOAT
    | BINARY_INTEGER
    | BOOL
    | BULK
    | BYTE
    | BEFORE
    | BODY
    | C
    | CALL
    | CHARACTER
    | CHARSET
    | CLOB
    | COLLATE
    | COLLECT
    | COLLATION
    | COMPILE
    | CONSTRUCTOR
    | CONTINUE
    | CURRENT_USER
    | CUSTOMDATUM
    | COMPOUND
    | DEBUG
    | DEFINER
    | DETERMINISTIC
    | DISABLE
    | EACH
    | ENABLE
    | EXCEPTIONS
    | EXTERNAL
    | EDITIONABLE
    | EXECUTE
    | FINAL
    | FORALL
    | FORCE
    | HASH
    | IMMEDIATE
    | INDICES
    | INSTEAD
    | INTERFACE
    | INTERVAL
    | JAVA
    | LANGUAGE
    | LIMIT
    | LOCAL
    | LONG
    | INSTANTIABLE
    | MEMBER
    | NAME
    | NATURALN
    | NCHAR
    | NVARCHAR
    | NESTED
    | NVARCHAR2
    | NO
    | NONEDITIONABLE
    | NUMERIC
    | OBJECT
    | OID
    | OVERRIDING
    | ORADATA
    | PARALLEL_ENABLE
    | PIPE
    | PIPELINED
    | PLS_INTEGER
    | POSITIVEN
    | RAW
    | REF
    | RELIES_ON
    | REPLACE
    | RESTRICT_REFERENCES
    | RESULT
    | RESULT_CACHE
    | RETURNING
    | REUSE
    | RNDS
    | RNPS
    | SAVE
    | SELF
    | SERIALLY_REUSABLE
    | SETTINGS
    | SIGNTYPE
    | SIMPLE_INTEGER
    | SIMPLE_DOUBLE
    | SIMPLE_FLOAT
    | SPECIFICATION
    | SQLDATA
    | STATIC
    | TIME
    | TIMESTAMP
    | TRUST
    | UDF
    | UNDER
    | USING
    | USING_NLS_COMP
    | VALIDATE
    | VALUE
    | VARIABLE
    | VARRAY
    | VARYING
    | WNDS
    | WNPS
    | YEAR
    | ZONE
    | MONTH
    | DAY
    | HOUR
    | MINUTE
    | SECOND
    | UROWID
    | LEVEL
    | INLINE
    | NEW
    | OLD
    | PARENT
    | REFERENCING
    | ROW
    | TRIGGER
    | YES
    | NOCOPY
    | ARRAY
    | CLOSE
    | OPEN
    | POSITIVE
    | PRAGMA
    | RAISE
    | RECORD
    | REVERSE
    | ROWTYPE
    | STATEMENT
    | CONSTANT
    | DATE
    | EXCEPTION_INIT
    | EXIT
    | FLOAT
    | LOOP
    | NATURAL
    | NUMBER
    | OTHERS
    | OUT
    | PACKAGE_P
    | PARTITION
    | RANGE
    | REAL
    | ROWID
    | VARCHAR
    | VARCHAR2
    ;

unreserved_special_keyword
    : MAP
    | TYPE
    ;

empty
    :
    ;


